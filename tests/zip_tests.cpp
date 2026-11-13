// Self-contained checks for the minimal ZIP/JAR reader.
//
// Usage: zip_tests <work-dir>
// The test writes a few synthetic archives to <work-dir> and reads them back
// through ZipFile, covering the edge cases the JVM class loader relies on:
// stored and deflated entries, central-directory ordering, duplicate names,
// zero-length entries and DEFLATE payloads larger than 64 KiB.

#include "stdafx.h"
#include "vm/io/zipreader.h"

#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

#include <zlib.h>

static int g_passed = 0;
static int g_failed = 0;

#define CHECK(cond) \
	do { \
		if (cond) { ++g_passed; } \
		else { ++g_failed; std::fprintf(stderr, "FAIL %s:%d: %s\n", __FILE__, __LINE__, #cond); } \
	} while (0)

namespace {

struct EntrySpec
{
	std::string name;
	std::vector<uint8_t> data;
	bool deflate;
};

void PutU16(std::vector<uint8_t> &out, uint16_t v)
{
	out.push_back(static_cast<uint8_t>(v & 0xFF));
	out.push_back(static_cast<uint8_t>((v >> 8) & 0xFF));
}

void PutU32(std::vector<uint8_t> &out, uint32_t v)
{
	out.push_back(static_cast<uint8_t>(v & 0xFF));
	out.push_back(static_cast<uint8_t>((v >> 8) & 0xFF));
	out.push_back(static_cast<uint8_t>((v >> 16) & 0xFF));
	out.push_back(static_cast<uint8_t>((v >> 24) & 0xFF));
}

std::vector<uint8_t> DeflateRaw(const std::vector<uint8_t> &input)
{
	std::vector<uint8_t> output;
	z_stream stream;
	memset(&stream, 0, sizeof(stream));
	if (deflateInit2(&stream, Z_DEFAULT_COMPRESSION, Z_DEFLATED, -MAX_WBITS,
	                 8, Z_DEFAULT_STRATEGY) != Z_OK)
	{
		return output;
	}
	stream.next_in = const_cast<Bytef*>(input.empty() ? NULL : &input[0]);
	stream.avail_in = static_cast<uInt>(input.size());
	uint8_t buffer[16384];
	int rc = Z_OK;
	do
	{
		stream.next_out = buffer;
		stream.avail_out = sizeof(buffer);
		rc = deflate(&stream, Z_FINISH);
		size_t produced = sizeof(buffer) - stream.avail_out;
		output.insert(output.end(), buffer, buffer + produced);
	} while (rc == Z_OK);
	deflateEnd(&stream);
	if (rc != Z_STREAM_END)
	{
		output.clear();
	}
	return output;
}

std::vector<uint8_t> BuildArchive(const std::vector<EntrySpec> &entries,
                                  bool reverse_central)
{
	struct Local
	{
		uint32_t offset;
		uint32_t crc;
		uint32_t compressed_size;
		uint32_t uncompressed_size;
		uint16_t method;
		std::string name;
	};
	std::vector<Local> locals;
	std::vector<uint8_t> out;

	for (size_t i = 0; i < entries.size(); i++)
	{
		const EntrySpec &entry = entries[i];
		std::vector<uint8_t> payload =
			entry.deflate ? DeflateRaw(entry.data) : entry.data;
		Local local;
		local.offset = static_cast<uint32_t>(out.size());
		local.crc = static_cast<uint32_t>(crc32(0L, Z_NULL, 0));
		if (!entry.data.empty())
		{
			local.crc = static_cast<uint32_t>(
				crc32(local.crc, &entry.data[0],
				      static_cast<uInt>(entry.data.size())));
		}
		local.compressed_size = static_cast<uint32_t>(payload.size());
		local.uncompressed_size = static_cast<uint32_t>(entry.data.size());
		local.method = entry.deflate ? 8 : 0;
		local.name = entry.name;
		locals.push_back(local);

		PutU32(out, 0x04034b50);
		PutU16(out, 20);
		PutU16(out, 0);
		PutU16(out, local.method);
		PutU16(out, 0);
		PutU16(out, 0);
		PutU32(out, local.crc);
		PutU32(out, local.compressed_size);
		PutU32(out, local.uncompressed_size);
		PutU16(out, static_cast<uint16_t>(entry.name.size()));
		PutU16(out, 0);
		out.insert(out.end(), entry.name.begin(), entry.name.end());
		out.insert(out.end(), payload.begin(), payload.end());
	}

	size_t central_offset = out.size();
	size_t count = locals.size();
	for (size_t n = 0; n < count; n++)
	{
		size_t i = reverse_central ? count - 1 - n : n;
		const Local &local = locals[i];
		PutU32(out, 0x02014b50);
		PutU16(out, 20);
		PutU16(out, 20);
		PutU16(out, 0);
		PutU16(out, local.method);
		PutU16(out, 0);
		PutU16(out, 0);
		PutU32(out, local.crc);
		PutU32(out, local.compressed_size);
		PutU32(out, local.uncompressed_size);
		PutU16(out, static_cast<uint16_t>(local.name.size()));
		PutU16(out, 0);
		PutU16(out, 0);
		PutU16(out, 0);
		PutU16(out, 0);
		PutU32(out, 0);
		PutU32(out, local.offset);
		out.insert(out.end(), local.name.begin(), local.name.end());
	}
	size_t central_size = out.size() - central_offset;

	PutU32(out, 0x06054b50);
	PutU16(out, 0);
	PutU16(out, 0);
	PutU16(out, static_cast<uint16_t>(count));
	PutU16(out, static_cast<uint16_t>(count));
	PutU32(out, static_cast<uint32_t>(central_size));
	PutU32(out, static_cast<uint32_t>(central_offset));
	PutU16(out, 0);
	return out;
}

bool WriteFile(const std::string &path, const std::vector<uint8_t> &data)
{
	FILE *f = fopen(path.c_str(), "wb");
	if (!f)
	{
		return false;
	}
	bool ok = data.empty() ||
		fwrite(&data[0], 1, data.size(), f) == data.size();
	fclose(f);
	return ok;
}

std::string Join(const std::string &dir, const std::string &name)
{
	if (dir.empty())
	{
		return name;
	}
	char last = dir[dir.size() - 1];
	if (last == '/' || last == '\\')
	{
		return dir + name;
	}
	return dir + "/" + name;
}

bool ReadMatches(ZipFile &zip, const std::string &name,
                 const std::vector<uint8_t> &expected)
{
	std::vector<uint8_t> out;
	if (!zip.ReadEntry(name, out))
	{
		return false;
	}
	return out.size() == expected.size() &&
		(expected.empty() || memcmp(&out[0], &expected[0], out.size()) == 0);
}

std::vector<uint8_t> Pattern(size_t size)
{
	// Deterministic but poorly compressible bytes so the deflate stream for
	// the large entry really exceeds 64 KiB.
	std::vector<uint8_t> data(size);
	uint32_t state = 0x12345678u;
	for (size_t i = 0; i < size; i++)
	{
		state ^= state << 13;
		state ^= state >> 17;
		state ^= state << 5;
		data[i] = static_cast<uint8_t>(state & 0xFF);
	}
	return data;
}

} // namespace

int main(int argc, char **argv)
{
	std::string dir = argc > 1 ? argv[1] : ".";

	// Stored entries with the central directory in reverse order of the
	// local headers.  Reading must follow the recorded offsets, not the
	// physical order of the headers.
	{
		std::vector<EntrySpec> entries;
		EntrySpec a; a.name = "a.txt"; a.deflate = false;
		const char text_a[] = "alpha";
		a.data.assign(text_a, text_a + sizeof(text_a) - 1);
		EntrySpec b; b.name = "nested/b.txt"; b.deflate = false;
		const char text_b[] = "beta-payload";
		b.data.assign(text_b, text_b + sizeof(text_b) - 1);
		entries.push_back(a);
		entries.push_back(b);
		std::string path = Join(dir, "order.jar");
		CHECK(WriteFile(path, BuildArchive(entries, true)));
		ZipFile zip;
		CHECK(zip.Open(path));
		CHECK(ReadMatches(zip, "a.txt", a.data));
		CHECK(ReadMatches(zip, "nested/b.txt", b.data));
	}

	// Duplicate names: the first central-directory entry must win.
	{
		std::vector<EntrySpec> entries;
		EntrySpec first; first.name = "dup.txt"; first.deflate = false;
		const char one[] = "first";
		first.data.assign(one, one + sizeof(one) - 1);
		EntrySpec second; second.name = "dup.txt"; second.deflate = false;
		const char two[] = "second";
		second.data.assign(two, two + sizeof(two) - 1);
		entries.push_back(first);
		entries.push_back(second);
		std::string path = Join(dir, "duplicate.jar");
		CHECK(WriteFile(path, BuildArchive(entries, false)));
		ZipFile zip;
		CHECK(zip.Open(path));
		CHECK(zip.Entries().size() == 1);
		CHECK(ReadMatches(zip, "dup.txt", first.data));
	}

	// A zero-length class entry must read back as empty rather than crash.
	{
		std::vector<EntrySpec> entries;
		EntrySpec empty; empty.name = "Empty.class"; empty.deflate = false;
		entries.push_back(empty);
		std::string path = Join(dir, "empty-entry.jar");
		CHECK(WriteFile(path, BuildArchive(entries, false)));
		ZipFile zip;
		CHECK(zip.Open(path));
		std::vector<uint8_t> out;
		out.push_back(0xAA);
		CHECK(zip.ReadEntry("Empty.class", out));
		CHECK(out.empty());
	}

	// DEFLATE around the 64 KiB boundary and a payload whose compressed form
	// is itself larger than 64 KiB.
	{
		const size_t sizes[] = {0, 1, 32768, 65535, 65536, 65537, 200000};
		std::vector<EntrySpec> entries;
		for (size_t i = 0; i < sizeof(sizes) / sizeof(sizes[0]); i++)
		{
			EntrySpec spec;
			char name[64];
			std::snprintf(name, sizeof(name), "deflate/%lu.bin",
			              static_cast<unsigned long>(sizes[i]));
			spec.name = name;
			spec.data = Pattern(sizes[i]);
			spec.deflate = true;
			entries.push_back(spec);
		}
		std::string path = Join(dir, "deflate.jar");
		std::vector<uint8_t> archive = BuildArchive(entries, false);
		CHECK(WriteFile(path, archive));
		ZipFile zip;
		CHECK(zip.Open(path));
		for (size_t i = 0; i < entries.size(); i++)
		{
			CHECK(ReadMatches(zip, entries[i].name, entries[i].data));
		}
		std::map<std::string, ZipFile::Entry>::const_iterator it =
			zip.Entries().find("deflate/200000.bin");
		CHECK(it != zip.Entries().end());
		if (it != zip.Entries().end())
		{
			CHECK(it->second.method == 8);
			CHECK(it->second.compressed_size > 65536);
			CHECK(it->second.uncompressed_size == 200000);
		}
	}

	// DEFLATE with a declared uncompressed size that is smaller than reality:
	// the reader must grow its output buffer instead of truncating.
	{
		std::vector<uint8_t> data = Pattern(70000);
		std::vector<uint8_t> archive = BuildArchive(
			std::vector<EntrySpec>(1, EntrySpec{"grow.bin", data, true}), false);
		// Corrupt only the declared uncompressed size.  In the local header
		// it is at offset 22, in the central header at offset 24.
		for (size_t pos = 0; pos + 4 <= archive.size(); pos++)
		{
			if (archive[pos] != 'P' || archive[pos + 1] != 'K')
			{
				continue;
			}
			size_t size_offset = 0;
			if (archive[pos + 2] == 0x03 && archive[pos + 3] == 0x04)
			{
				size_offset = pos + 22;
			}
			else if (archive[pos + 2] == 0x01 && archive[pos + 3] == 0x02)
			{
				size_offset = pos + 24;
			}
			else
			{
				continue;
			}
			archive[size_offset] = 0x10;
			archive[size_offset + 1] = 0x00;
			archive[size_offset + 2] = 0x00;
			archive[size_offset + 3] = 0x00;
		}
		std::string path = Join(dir, "grow.jar");
		CHECK(WriteFile(path, archive));
		ZipFile zip;
		CHECK(zip.Open(path));
		CHECK(ReadMatches(zip, "grow.bin", data));
	}

	std::printf("zip checks: %d, failed: %d\n", g_passed, g_failed);
	return g_failed == 0 ? 0 : 1;
}
