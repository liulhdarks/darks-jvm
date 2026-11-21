#include "stdafx.h"
#include "deterministic.h"

#include "env/env.h"

#include <cstdlib>
#include <cstring>

namespace {

const uint64_t kFnvOffset = 1469598103934665603ULL;
const uint64_t kFnvPrime = 1099511628211ULL;

void FnvByte(uint64_t &hash, uint8_t value)
{
	hash ^= static_cast<uint64_t>(value);
	hash *= kFnvPrime;
}

void FnvWord(uint64_t &hash, uint64_t value)
{
	for (int i = 0; i < 8; i++)
	{
		FnvByte(hash, static_cast<uint8_t>((value >> (8 * i)) & 0xff));
	}
}

std::string TrimProperty(const std::string &value)
{
	size_t begin = 0;
	size_t end = value.size();
	while (begin < end && (value[begin] == ' ' || value[begin] == '\t'))
	{
		begin++;
	}
	while (end > begin && (value[end - 1] == ' ' || value[end - 1] == '\t' ||
	                       value[end - 1] == '\r' || value[end - 1] == '\n'))
	{
		end--;
	}
	return value.substr(begin, end - begin);
}

bool LooksDisabled(const std::string &value)
{
	return value == "off" || value == "false" || value == "no" || value == "0";
}

bool GetProp(const char *key, std::string &out)
{
	return Environment::GetProperty(std::string(key), out);
}

} // namespace

DarksDeterministic::DarksDeterministic()
{
	initialized = false;
	enabled = false;
	replay = false;
	finished = false;
	seed = 0;
	vnow = 0;
	epoch_ms = 1700000000000LL; // 2023-11-14T22:13:20Z, fixed for reproducibility
	record_fp = NULL;
	hash = kFnvOffset;
	count = 0;
	expected_pos = 0;
	replay_failed = false;
	expected_hash = 0;
	expected_count = 0;
}

DarksDeterministic::~DarksDeterministic()
{
	CloseRecordLocked();
}

DarksDeterministic &DarksDeterministic::Instance()
{
	static DarksDeterministic *instance = new DarksDeterministic();
	return *instance;
}

void DarksDeterministic::Initialize()
{
	if (initialized)
	{
		return;
	}
	std::lock_guard<std::mutex> lk(mu);
	if (initialized)
	{
		return;
	}
	InitializeLocked();
	initialized = true;
}

void DarksDeterministic::InitializeLocked()
{
	std::string value;
	bool requested = false;
	if (GetProp("darks.deterministic", value))
	{
		value = TrimProperty(value);
		if (!LooksDisabled(value))
		{
			requested = true;
			if (!value.empty())
			{
				seed = strtoull(value.c_str(), NULL, 10);
			}
		}
	}
	if (GetProp("darks.record", value))
	{
		record_path = TrimProperty(value);
		if (!record_path.empty())
		{
			requested = true;
		}
	}
	if (GetProp("darks.replay", value))
	{
		replay_path = TrimProperty(value);
		if (!replay_path.empty())
		{
			requested = true;
		}
	}
	enabled = requested;
	if (!enabled)
	{
		return;
	}
	if (replay_path.empty() && seed == 0)
	{
		// A seed of zero is legal, but an omitted value should still be a
		// usable seed; only promote the implicit case.
		if (!GetProp("darks.deterministic", value) || TrimProperty(value).empty() ||
		    TrimProperty(value) == "on" || TrimProperty(value) == "true")
		{
			seed = 1;
		}
	}
	if (!replay_path.empty())
	{
		if (!LoadReplayLocked(replay_path))
		{
			// A missing or corrupt log must not silently claim a match.
			replay = true;
			replay_failed = true;
		}
		else
		{
			replay = true;
		}
	}
	if (!record_path.empty())
	{
		record_fp = fopen(record_path.c_str(), "wb");
		if (record_fp)
		{
			fprintf(record_fp, "# darks deterministic v1 seed=%llu\n",
			        static_cast<unsigned long long>(seed));
			fflush(record_fp);
		}
	}
}

bool DarksDeterministic::LoadReplayLocked(const std::string &path)
{
	FILE *fp = fopen(path.c_str(), "rb");
	if (!fp)
	{
		return false;
	}
	char line[512];
	bool saw_header = false;
	while (fgets(line, sizeof(line), fp))
	{
		if (line[0] == '#' || line[0] == '\n' || line[0] == '\r')
		{
			if (!saw_header && strstr(line, "darks deterministic"))
			{
				const char *seed_pos = strstr(line, "seed=");
				if (seed_pos)
				{
					seed = strtoull(seed_pos + 5, NULL, 10);
				}
				saw_header = true;
			}
			continue;
		}
		if (line[0] == 'E')
		{
			char kind = '?';
			unsigned long long a = 0;
			unsigned long long b = 0;
			if (sscanf(line, "E %c %llu %llu", &kind, &a, &b) != 3)
			{
				fclose(fp);
				return false;
			}
			expected.push_back(Entry(kind, static_cast<uint64_t>(a),
			                         static_cast<uint64_t>(b)));
			continue;
		}
		if (line[0] == 'H')
		{
			unsigned long long n = 0;
			unsigned long long h = 0;
			if (sscanf(line, "H %llu %llu", &n, &h) == 2)
			{
				expected_count = static_cast<uint64_t>(n);
				expected_hash = static_cast<uint64_t>(h);
			}
		}
	}
	fclose(fp);
	return true;
}

void DarksDeterministic::CloseRecordLocked()
{
	if (record_fp)
	{
		fclose(record_fp);
		record_fp = NULL;
	}
}

int64_t DarksDeterministic::NowNs()
{
	return vnow;
}

void DarksDeterministic::AdvanceTo(int64_t t)
{
	if (t > vnow)
	{
		vnow = t;
	}
}

int64_t DarksDeterministic::NanoTime()
{
	if (!enabled)
	{
		return 0;
	}
	std::lock_guard<std::mutex> lk(mu);
	uint64_t value = static_cast<uint64_t>(vnow);
	Entry entry('C', 0, value);
	MixLocked(entry);
	if (record_fp)
	{
		fprintf(record_fp, "E C 0 %llu\n", static_cast<unsigned long long>(value));
	}
	if (replay)
	{
		if (expected_pos < expected.size() && expected[expected_pos].kind == 'C' &&
		    expected[expected_pos].b == value)
		{
			expected_pos++;
		}
		else
		{
			replay_failed = true;
			if (expected_pos < expected.size() && expected[expected_pos].kind == 'C')
			{
				expected_pos++;
			}
		}
	}
	return vnow;
}

int64_t DarksDeterministic::CurrentTimeMillis()
{
	if (!enabled)
	{
		return 0;
	}
	std::lock_guard<std::mutex> lk(mu);
	uint64_t value = static_cast<uint64_t>(epoch_ms + vnow / 1000000LL);
	Entry entry('C', 1, value);
	MixLocked(entry);
	if (record_fp)
	{
		fprintf(record_fp, "E C 1 %llu\n", static_cast<unsigned long long>(value));
	}
	if (replay)
	{
		if (expected_pos < expected.size() && expected[expected_pos].kind == 'C' &&
		    expected[expected_pos].a == 1 && expected[expected_pos].b == value)
		{
			expected_pos++;
		}
		else
		{
			replay_failed = true;
			if (expected_pos < expected.size() && expected[expected_pos].kind == 'C')
			{
				expected_pos++;
			}
		}
	}
	return static_cast<int64_t>(value);
}

uint64_t DarksDeterministic::Mix64(uint64_t value) const
{
	value += 0x9E3779B97F4A7C15ULL;
	value = (value ^ (value >> 30)) * 0xBF58476D1CE4E5B9ULL;
	value = (value ^ (value >> 27)) * 0x94D049BB133111EBULL;
	return value ^ (value >> 31);
}

uint64_t DarksDeterministic::OrderFor(uint64_t coroutine_id) const
{
	// seed + id are mixed so the same seed reproduces the same rank for the
	// same creation order, while a different seed permutes the ranks.
	return Mix64(seed ^ (coroutine_id * 0x9E3779B97F4A7C15ULL));
}

void DarksDeterministic::MixLocked(const Entry &entry)
{
	FnvByte(hash, static_cast<uint8_t>(entry.kind));
	FnvWord(hash, entry.a);
	FnvWord(hash, entry.b);
	count++;
}

void DarksDeterministic::Event(char kind, uint64_t a, uint64_t b)
{
	if (!enabled)
	{
		return;
	}
	std::lock_guard<std::mutex> lk(mu);
	Entry entry(kind, a, b);
	MixLocked(entry);
	if (record_fp)
	{
		fprintf(record_fp, "E %c %llu %llu\n", kind,
		        static_cast<unsigned long long>(a),
		        static_cast<unsigned long long>(b));
	}
	if (replay)
	{
		if (expected_pos < expected.size() &&
		    expected[expected_pos].kind == entry.kind &&
		    expected[expected_pos].a == entry.a &&
		    expected[expected_pos].b == entry.b)
		{
			expected_pos++;
		}
		else
		{
			replay_failed = true;
			if (expected_pos < expected.size() &&
			    expected[expected_pos].kind == entry.kind)
			{
				// Same kind but divergent payload: consume it so later
				// events can still line up.
				expected_pos++;
			}
		}
	}
}

int DarksDeterministic::NextRandom(int bits, int value)
{
	if (!enabled)
	{
		return value;
	}
	std::lock_guard<std::mutex> lk(mu);
	int result = value;
	bool have_recorded = false;
	if (replay && expected_pos < expected.size() &&
	    expected[expected_pos].kind == 'R' && expected[expected_pos].a == static_cast<uint64_t>(bits))
	{
		result = static_cast<int>(static_cast<uint32_t>(expected[expected_pos].b));
		have_recorded = true;
	}
	Entry entry('R', static_cast<uint64_t>(bits),
	            static_cast<uint64_t>(static_cast<uint32_t>(result)));
	MixLocked(entry);
	if (record_fp)
	{
		fprintf(record_fp, "E R %llu %llu\n", static_cast<unsigned long long>(bits),
		        static_cast<unsigned long long>(static_cast<uint32_t>(result)));
	}
	if (replay)
	{
		if (have_recorded)
		{
			expected_pos++;
		}
		else if (expected_pos < expected.size() && expected[expected_pos].kind == 'R' &&
		         expected[expected_pos].a == static_cast<uint64_t>(bits))
		{
			// Payload mismatch; consume to resynchronise.
			replay_failed = true;
			expected_pos++;
		}
		else
		{
			replay_failed = true;
		}
	}
	return result;
}

void DarksDeterministic::Boundary(const char *what)
{
	if (!enabled)
	{
		return;
	}
	uint64_t h = kFnvOffset;
	if (what)
	{
		for (const char *p = what; *p; p++)
		{
			FnvByte(h, static_cast<uint8_t>(*p));
		}
	}
	Event('B', h, 1);
}

void DarksDeterministic::Finish()
{
	if (!enabled)
	{
		return;
	}
	std::lock_guard<std::mutex> lk(mu);
	if (finished)
	{
		return;
	}
	finished = true;
	bool matched = !replay_failed;
	if (replay)
	{
		if (expected_pos != expected.size())
		{
			matched = false;
		}
		if (expected_count != 0 &&
		    (count != expected_count || hash != expected_hash))
		{
			matched = false;
		}
		printf("deterministic-replay: seed=%llu events=%llu hash=%016llx matched=%s\n",
		       static_cast<unsigned long long>(seed),
		       static_cast<unsigned long long>(count),
		       static_cast<unsigned long long>(hash),
		       matched ? "true" : "false");
	}
	else
	{
		printf("deterministic: seed=%llu events=%llu hash=%016llx\n",
		       static_cast<unsigned long long>(seed),
		       static_cast<unsigned long long>(count),
		       static_cast<unsigned long long>(hash));
	}
	fflush(stdout);
	if (record_fp)
	{
		fprintf(record_fp, "H %llu %llu\n",
		        static_cast<unsigned long long>(count),
		        static_cast<unsigned long long>(hash));
		fflush(record_fp);
		CloseRecordLocked();
	}
}
