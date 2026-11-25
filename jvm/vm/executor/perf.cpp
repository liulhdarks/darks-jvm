#include "stdafx.h"
#include "perf.h"

#include <algorithm>
#include <atomic>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>

namespace darksperf {

bool g_enabled = false;

namespace {

typedef std::chrono::steady_clock Clock;

uint64_t g_start_ns = 0;

std::atomic<uint64_t> g_phase_start[PHASE_COUNT];
std::atomic<uint64_t> g_phase_end[PHASE_COUNT];

std::atomic<uint64_t> g_class_cache_hits(0);
std::atomic<uint64_t> g_class_cache_misses(0);
std::atomic<uint64_t> g_class_reads(0);
std::atomic<uint64_t> g_class_read_ns(0);
std::atomic<uint64_t> g_class_parsed(0);
std::atomic<uint64_t> g_class_parse_ns(0);
std::atomic<uint64_t> g_class_linked(0);
std::atomic<uint64_t> g_class_link_ns(0);
std::atomic<uint64_t> g_clinit(0);
std::atomic<uint64_t> g_clinit_ns(0);

std::atomic<uint64_t> g_gc_runs(0);
std::atomic<uint64_t> g_gc_pause_ns(0);
std::atomic<uint64_t> g_allocated(0);

std::atomic<uint64_t> g_first_connection_ns(0);

std::atomic<uint64_t> g_jit_programs(0);
std::atomic<uint64_t> g_jit_bytes(0);

std::atomic<uint64_t> g_bytecodes(0);
std::atomic<uint64_t> g_methods(0);
std::atomic<uint64_t> g_opcodes[256];

struct ThreadAccumulator
{
	uint64_t bytecodes;
	uint64_t methods;
	uint64_t opcodes[256];

	ThreadAccumulator()
	{
		bytecodes = 0;
		methods = 0;
		memset(opcodes, 0, sizeof(opcodes));
	}

	~ThreadAccumulator()
	{
		if (!g_enabled)
		{
			return;
		}
		if (bytecodes)
		{
			g_bytecodes.fetch_add(bytecodes, std::memory_order_relaxed);
		}
		if (methods)
		{
			g_methods.fetch_add(methods, std::memory_order_relaxed);
		}
		for (int i = 0; i < 256; i++)
		{
			if (opcodes[i])
			{
				g_opcodes[i].fetch_add(opcodes[i], std::memory_order_relaxed);
			}
		}
	}
};

thread_local ThreadAccumulator t_accumulator;

uint64_t NowNs()
{
	return static_cast<uint64_t>(
		std::chrono::duration_cast<std::chrono::nanoseconds>(
			Clock::now().time_since_epoch()).count());
}

uint64_t SinceStartNs()
{
	return NowNs() - g_start_ns;
}

struct Initialize
{
	Initialize()
	{
		const char *value = getenv("DARKS_PERF");
		g_enabled = value && value[0] && strcmp(value, "0") != 0;
		g_start_ns = NowNs();
	}
};

Initialize g_initialize;

double Ms(uint64_t ns)
{
	return static_cast<double>(ns) / 1000000.0;
}

const char *PhaseName(int phase)
{
	switch (phase)
	{
	case PHASE_VM_INIT: return "vm-init";
	case PHASE_MAIN_CLASS: return "main-class-load";
	case PHASE_MAIN: return "main-run";
	case PHASE_SHUTDOWN: return "shutdown";
	default: return "?";
	}
}

} // namespace

void BeginPhase(Phase phase)
{
	if (!g_enabled)
	{
		return;
	}
	if (phase >= 0 && phase < PHASE_COUNT)
	{
		g_phase_start[phase].store(SinceStartNs(), std::memory_order_relaxed);
	}
}

void EndPhase(Phase phase)
{
	if (!g_enabled)
	{
		return;
	}
	if (phase >= 0 && phase < PHASE_COUNT)
	{
		g_phase_end[phase].store(SinceStartNs(), std::memory_order_relaxed);
	}
}

void NoteClassCacheHit()
{
	if (g_enabled)
	{
		g_class_cache_hits.fetch_add(1, std::memory_order_relaxed);
	}
}

void NoteClassCacheMiss()
{
	if (g_enabled)
	{
		g_class_cache_misses.fetch_add(1, std::memory_order_relaxed);
	}
}

void AddClassReadNs(uint64_t ns)
{
	if (g_enabled)
	{
		g_class_read_ns.fetch_add(ns, std::memory_order_relaxed);
	}
}

void NoteClassRead()
{
	if (g_enabled)
	{
		g_class_reads.fetch_add(1, std::memory_order_relaxed);
	}
}

void AddClassParseNs(uint64_t ns)
{
	if (g_enabled)
	{
		g_class_parse_ns.fetch_add(ns, std::memory_order_relaxed);
	}
}

void NoteClassParsed()
{
	if (g_enabled)
	{
		g_class_parsed.fetch_add(1, std::memory_order_relaxed);
	}
}

void AddClassLinkNs(uint64_t ns)
{
	if (g_enabled)
	{
		g_class_link_ns.fetch_add(ns, std::memory_order_relaxed);
	}
}

void NoteClassLinked()
{
	if (g_enabled)
	{
		g_class_linked.fetch_add(1, std::memory_order_relaxed);
	}
}

void AddClinitNs(uint64_t ns)
{
	if (g_enabled)
	{
		g_clinit_ns.fetch_add(ns, std::memory_order_relaxed);
	}
}

void NoteClinit()
{
	if (g_enabled)
	{
		g_clinit.fetch_add(1, std::memory_order_relaxed);
	}
}

void NoteMethod()
{
	if (g_enabled)
	{
		t_accumulator.methods++;
	}
}

void NoteInstruction(unsigned op)
{
	if (g_enabled)
	{
		t_accumulator.bytecodes++;
		if (op < 256)
		{
			t_accumulator.opcodes[op]++;
		}
	}
}

void BeginGc()
{
	if (g_enabled)
	{
		g_gc_runs.fetch_add(1, std::memory_order_relaxed);
	}
}

void AddGcPauseNs(uint64_t ns)
{
	if (g_enabled)
	{
		g_gc_pause_ns.fetch_add(ns, std::memory_order_relaxed);
	}
}

void AddAllocated(size_t bytes)
{
	if (g_enabled)
	{
		g_allocated.fetch_add(static_cast<uint64_t>(bytes),
			std::memory_order_relaxed);
	}
}

void NoteFirstConnection()
{
	if (!g_enabled)
	{
		return;
	}
	uint64_t expected = 0;
	g_first_connection_ns.compare_exchange_strong(expected, SinceStartNs(),
		std::memory_order_relaxed);
}

void AddJitProgram(size_t bytes)
{
	if (g_enabled)
	{
		g_jit_programs.fetch_add(1, std::memory_order_relaxed);
		g_jit_bytes.fetch_add(static_cast<uint64_t>(bytes),
			std::memory_order_relaxed);
	}
}

void PrintSummary()
{
	if (!g_enabled)
	{
		return;
	}
	// The scheduler has already joined its workers here; any remaining
	// per-thread counters belong to this thread and are merged on exit, so
	// flush them explicitly first.
	{
		ThreadAccumulator &a = t_accumulator;
		if (a.bytecodes)
		{
			g_bytecodes.fetch_add(a.bytecodes, std::memory_order_relaxed);
			a.bytecodes = 0;
		}
		if (a.methods)
		{
			g_methods.fetch_add(a.methods, std::memory_order_relaxed);
			a.methods = 0;
		}
		for (int i = 0; i < 256; i++)
		{
			if (a.opcodes[i])
			{
				g_opcodes[i].fetch_add(a.opcodes[i], std::memory_order_relaxed);
				a.opcodes[i] = 0;
			}
		}
	}

	fprintf(stderr,
		"[perf] uptime_ms=%.1f\n", Ms(SinceStartNs()));

	for (int phase = 0; phase < PHASE_COUNT; phase++)
	{
		uint64_t begin = g_phase_start[phase].load(std::memory_order_relaxed);
		uint64_t end = g_phase_end[phase].load(std::memory_order_relaxed);
		if (end >= begin && end != 0)
		{
			fprintf(stderr, "[perf] phase %s=%.1fms\n",
				PhaseName(phase), Ms(end - begin));
		}
	}
	uint64_t first = g_first_connection_ns.load(std::memory_order_relaxed);
	if (first)
	{
		fprintf(stderr, "[perf] first-connection=%.1fms\n", Ms(first));
	}

	fprintf(stderr,
		"[perf] class cache_hits=%llu cache_misses=%llu reads=%llu "
		"parsed=%llu linked=%llu clinit=%llu read_ms=%.1f parse_ms=%.1f "
		"link_ms=%.1f clinit_ms=%.1f\n",
		static_cast<unsigned long long>(g_class_cache_hits.load()),
		static_cast<unsigned long long>(g_class_cache_misses.load()),
		static_cast<unsigned long long>(g_class_reads.load()),
		static_cast<unsigned long long>(g_class_parsed.load()),
		static_cast<unsigned long long>(g_class_linked.load()),
		static_cast<unsigned long long>(g_clinit.load()),
		Ms(g_class_read_ns.load()), Ms(g_class_parse_ns.load()),
		Ms(g_class_link_ns.load()), Ms(g_clinit_ns.load()));

	fprintf(stderr,
		"[perf] interp methods=%llu bytecodes=%llu\n",
		static_cast<unsigned long long>(g_methods.load()),
		static_cast<unsigned long long>(g_bytecodes.load()));

	fprintf(stderr,
		"[perf] jit programs=%llu code_kb=%.1f\n",
		static_cast<unsigned long long>(g_jit_programs.load()),
		static_cast<double>(g_jit_bytes.load()) / 1024.0);

	fprintf(stderr,
		"[perf] gc runs=%llu pause_ms=%.1f allocated_mb=%.2f\n",
		static_cast<unsigned long long>(g_gc_runs.load()),
		Ms(g_gc_pause_ns.load()),
		static_cast<double>(g_allocated.load()) / (1024.0 * 1024.0));

	std::vector<std::pair<uint64_t, int> > ops;
	for (int i = 0; i < 256; i++)
	{
		uint64_t count = g_opcodes[i].load(std::memory_order_relaxed);
		if (count)
		{
			ops.push_back(std::make_pair(count, i));
		}
	}
	std::sort(ops.begin(), ops.end());
	size_t limit = ops.size() < 16 ? ops.size() : 16;
	fprintf(stderr, "[perf] top-opcodes:");
	for (size_t i = 0; i < limit; i++)
	{
		const std::pair<uint64_t, int> &entry = ops[ops.size() - 1 - i];
		fprintf(stderr, " %d:%llu", entry.second,
			static_cast<unsigned long long>(entry.first));
	}
	fprintf(stderr, "\n");
	fflush(stderr);
}

} // namespace darksperf
