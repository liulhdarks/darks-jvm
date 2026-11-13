#include "stdafx.h"
#include "zipreader.h"
#include "platform/platform.h"

#ifdef DARKS_JVM_HAVE_ZLIB
#include <zlib.h>
#endif

namespace {

uint16_t ReadU16(const std::vector<uint8_t> &b, size_t pos)
{
	return static_cast<uint16_t>(b[pos] | (b[pos + 1] << 8));
}

uint32_t ReadU32(const std::vector<uint8_t> &b, size_t pos)
{
	return static_cast<uint32_t>(b[pos]) |
		(static_cast<uint32_t>(b[pos + 1]) << 8) |
		(static_cast<uint32_t>(b[pos + 2]) << 16) |
		(static_cast<uint32_t>(b[pos + 3]) << 24);
}

} // namespace

ZipFile::ZipFile()
{
	opened = false;
}

ZipFile::~ZipFile()
{
}

bool ZipFile::Open(const std::string &file_path)
{
	path = file_path;
	opened = false;
	last_error.clear();
	entries.clear();
	data.clear();
	if (!dk::ReadFileBytes(path, data))
	{
		last_error = "cannot read archive";
		return false;
	}
	if (!ParseCentralDirectory())
	{
		return false;
	}
	opened = true;
	return true;
}

bool ZipFile::ParseCentralDirectory()
{
	if (data.size() < 22)
	{
		last_error = "file too small to be a zip archive";
		return false;
	}
	// Locate the EOCD signature, scanning backwards over the optional comment.
	size_t search_start = data.size() > 65557 ? data.size() - 65557 : 0;
	size_t eocd = std::string::npos;
	for (size_t i = data.size() - 22 + 1; i-- > search_start;)
	{
		if (data[i] == 'P' && data[i + 1] == 'K' && data[i + 2] == 0x05 && data[i + 3] == 0x06)
		{
			eocd = i;
			break;
		}
	}
	if (eocd == std::string::npos)
	{
		last_error = "end of central directory not found";
		return false;
	}
	uint16_t total_entries = ReadU16(data, eocd + 10);
	uint32_t central_offset = ReadU32(data, eocd + 16);
	if (central_offset == 0xFFFFFFFF || total_entries == 0xFFFF)
	{
		last_error = "ZIP64 archives are not supported";
		return false;
	}
	if (static_cast<size_t>(central_offset) >= data.size())
	{
		last_error = "central directory offset out of range";
		return false;
	}
	size_t pos = central_offset;
	for (uint16_t i = 0; i < total_entries; i++)
	{
		if (pos + 46 > data.size() ||
		    !(data[pos] == 'P' && data[pos + 1] == 'K' && data[pos + 2] == 0x01 && data[pos + 3] == 0x02))
		{
			last_error = "corrupt central directory";
			return false;
		}
		uint16_t flags = ReadU16(data, pos + 8);
		uint16_t method = ReadU16(data, pos + 10);
		uint32_t comp_size = ReadU32(data, pos + 20);
		uint32_t uncomp_size = ReadU32(data, pos + 24);
		uint16_t name_len = ReadU16(data, pos + 28);
		uint16_t extra_len = ReadU16(data, pos + 30);
		uint16_t comment_len = ReadU16(data, pos + 32);
		uint32_t local_offset = ReadU32(data, pos + 42);
		if (pos + 46 + name_len > data.size())
		{
			last_error = "corrupt central directory entry";
			return false;
		}
		Entry entry;
		entry.name.assign(reinterpret_cast<const char*>(&data[pos + 46]), name_len);
		entry.method = method;
		entry.compressed_size = comp_size;
		entry.uncompressed_size = uncomp_size;
		entry.local_header_offset = local_offset;
		// Encrypted entries cannot be decoded; keep them listed but reading
		// will fail with a clear message.
		if (flags & 0x0001)
		{
			entry.method = 0xFFFF;
		}
		// Duplicate names resolve to the first central-directory entry,
		// matching the JDK zip implementation.
		if (!entry.name.empty() && entry.name[entry.name.size() - 1] != '/' &&
		    entries.find(entry.name) == entries.end())
		{
			entries[entry.name] = entry;
		}
		pos += 46u + name_len + extra_len + comment_len;
	}
	return true;
}

bool ZipFile::HasEntry(const std::string &name) const
{
	return entries.find(name) != entries.end();
}

bool ZipFile::ReadEntry(const std::string &name, std::vector<uint8_t> &out)
{
	out.clear();
	std::map<std::string, Entry>::const_iterator it = entries.find(name);
	if (it == entries.end())
	{
		last_error = "entry not found: " + name;
		return false;
	}
	const Entry &entry = it->second;
	if (entry.method == 0xFFFF)
	{
		last_error = "encrypted entries are not supported";
		return false;
	}
	size_t pos = entry.local_header_offset;
	if (pos + 30 > data.size() ||
	    !(data[pos] == 'P' && data[pos + 1] == 'K' && data[pos + 2] == 0x03 && data[pos + 3] == 0x04))
	{
		last_error = "bad local file header for " + name;
		return false;
	}
	uint16_t name_len = ReadU16(data, pos + 26);
	uint16_t extra_len = ReadU16(data, pos + 28);
	size_t payload = pos + 30 + name_len + extra_len;
	if (payload + entry.compressed_size > data.size())
	{
		last_error = "truncated entry data for " + name;
		return false;
	}
	if (entry.method == 0)
	{
		out.assign(data.begin() + payload, data.begin() + payload + entry.compressed_size);
		return true;
	}
	return Inflate(entry, out);
}

bool ZipFile::Inflate(const Entry &entry, std::vector<uint8_t> &out)
{
	if (entry.method != 8)
	{
		last_error = "unsupported compression method";
		return false;
	}
#ifdef DARKS_JVM_HAVE_ZLIB
	size_t pos = entry.local_header_offset;
	uint16_t name_len = ReadU16(data, pos + 26);
	uint16_t extra_len = ReadU16(data, pos + 28);
	size_t payload = pos + 30 + name_len + extra_len;
	if (payload + entry.compressed_size > data.size())
	{
		last_error = "truncated entry data";
		return false;
	}
	// Grow the output buffer as needed instead of trusting the declared
	// uncompressed size; this also covers entries whose size fields are
	// inconsistent with the actual deflate stream.
	size_t capacity = entry.uncompressed_size ? entry.uncompressed_size : 1024;
	if (capacity < 1024)
	{
		capacity = 1024;
	}
	out.resize(capacity);
	z_stream stream;
	memset(&stream, 0, sizeof(stream));
	// Raw deflate stream: the zip container stores its own framing.
	if (inflateInit2(&stream, -MAX_WBITS) != Z_OK)
	{
		last_error = "inflateInit2 failed";
		return false;
	}
	stream.next_in = reinterpret_cast<Bytef*>(&data[payload]);
	stream.avail_in = entry.compressed_size;
	int rc = Z_OK;
	while (rc != Z_STREAM_END)
	{
		if (stream.total_out >= out.size())
		{
			size_t grown = out.size() * 2;
			if (grown < out.size() + 1024)
			{
				grown = out.size() + 1024;
			}
			out.resize(grown);
		}
		stream.next_out = reinterpret_cast<Bytef*>(&out[stream.total_out]);
		stream.avail_out = static_cast<uInt>(out.size() - stream.total_out);
		rc = inflate(&stream, Z_NO_FLUSH);
		if (rc != Z_OK && rc != Z_STREAM_END)
		{
			inflateEnd(&stream);
			last_error = "inflate failed";
			out.clear();
			return false;
		}
		if (rc == Z_BUF_ERROR && stream.avail_out != 0)
		{
			inflateEnd(&stream);
			last_error = "truncated deflate stream";
			out.clear();
			return false;
		}
	}
	size_t total = stream.total_out;
	inflateEnd(&stream);
	out.resize(total);
	return true;
#else
	(void)entry;
	last_error = "archive uses DEFLATE but this build has no zlib support";
	return false;
#endif
}

namespace {

std::string ManifestAttribute(const std::string &manifest, const std::string &wanted)
{
	std::string text = manifest;
	// Unfold continuation lines (a leading space means "append to previous").
	std::string unfolded;
	for (size_t i = 0; i < text.size(); i++)
	{
		char c = text[i];
		if (c == '\r')
		{
			continue;
		}
		if (c == '\n' && i + 1 < text.size() && text[i + 1] == ' ')
		{
			i++;
			continue;
		}
		unfolded.push_back(c);
	}
	size_t start = 0;
	while (start < unfolded.size())
	{
		size_t end = unfolded.find('\n', start);
		if (end == std::string::npos)
		{
			end = unfolded.size();
		}
		std::string line = unfolded.substr(start, end - start);
		start = end + 1;
		size_t colon = line.find(':');
		if (colon == std::string::npos)
		{
			continue;
		}
		std::string key = line.substr(0, colon);
		std::string value = line.substr(colon + 1);
		while (!value.empty() && (value[0] == ' ' || value[0] == '\t'))
		{
			value.erase(value.begin());
		}
		while (!value.empty() &&
		       (value[value.size() - 1] == ' ' || value[value.size() - 1] == '\t'))
		{
			value.erase(value.size() - 1);
		}
		if (key == wanted)
		{
			return value;
		}
	}
	return std::string();
}

std::string ReadManifest(ZipFile &zip)
{
	std::vector<uint8_t> manifest;
	if (!zip.ReadEntry("META-INF/MANIFEST.MF", manifest))
	{
		return std::string();
	}
	return std::string(manifest.begin(), manifest.end());
}

} // namespace

std::string ZipFile::GetManifestMainClass()
{
	std::string manifest = ReadManifest(*this);
	if (manifest.empty())
	{
		return std::string();
	}
	return ManifestAttribute(manifest, "Main-Class");
}

std::vector<std::string> ZipFile::GetManifestClassPath()
{
	std::vector<std::string> result;
	std::string manifest = ReadManifest(*this);
	if (manifest.empty())
	{
		return result;
	}
	std::string value = ManifestAttribute(manifest, "Class-Path");
	size_t start = 0;
	while (start <= value.size())
	{
		size_t end = value.find(' ', start);
		if (end == std::string::npos)
		{
			end = value.size();
		}
		if (end > start)
		{
			result.push_back(value.substr(start, end - start));
		}
		start = end + 1;
	}
	return result;
}
