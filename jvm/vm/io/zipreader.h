#pragma once

#include "common.h"

#include <map>
#include <string>
#include <vector>

// Minimal read-only ZIP/JAR reader.
//
// Supports the subset needed to load classes and resources from archives:
//  - end-of-central-directory (EOCD) lookup, including the trailing comment,
//  - central directory entries with STORED and DEFLATE compression,
//  - local file header parsing (name/extra field lengths are honoured).
// ZIP64 archives and encrypted entries are rejected with a readable error.
class JVM_API ZipFile
{
public:
	struct Entry
	{
		std::string name;
		uint16_t method;
		uint32_t compressed_size;
		uint32_t uncompressed_size;
		uint32_t local_header_offset;
	};

	ZipFile();
	~ZipFile();

	// Opens the archive and parses the central directory. Returns false on
	// malformed input (call LastError() for a human readable reason).
	bool Open(const std::string &path);

	bool IsOpen() const { return opened; }

	const std::string &Path() const { return path; }

	const std::string &LastError() const { return last_error; }

	bool HasEntry(const std::string &name) const;

	// Extracts one entry. Returns false when the entry is missing or the
	// compressed data cannot be decoded.
	bool ReadEntry(const std::string &name, std::vector<uint8_t> &out);

	// Convenience: Main-Class from META-INF/MANIFEST.MF ("" when absent).
	std::string GetManifestMainClass();

	const std::map<std::string, Entry> &Entries() const { return entries; }

private:
	std::string path;
	std::string last_error;
	bool opened;
	std::vector<uint8_t> data;
	std::map<std::string, Entry> entries;

	bool ParseCentralDirectory();
	bool Inflate(const Entry &entry, std::vector<uint8_t> &out);
};
