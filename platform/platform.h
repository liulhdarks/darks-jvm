// Portable platform abstraction layer for darks-jvm.
// Provides: UTF-8/UTF-16 conversion, path handling, clocks, dynamic library
// loading, executable path lookup.
#pragma once

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

namespace dk {

// ---- text conversion -------------------------------------------------------
// UTF-8 bytes -> UTF-16 code units (no BOM, no trailing NUL).
std::vector<uint16_t> Utf8ToUtf16(const char *utf8, std::size_t byte_len);
std::vector<uint16_t> Utf8ToUtf16(const std::string &utf8);
// UTF-16 code units -> UTF-8 bytes.
std::string Utf16ToUtf8(const uint16_t *units, std::size_t count);
std::string Utf16ToUtf8(const std::vector<uint16_t> &units);

// ---- time ------------------------------------------------------------------
uint64_t CurrentTimeMillis();
uint64_t NanoTime();

// ---- path ------------------------------------------------------------------
// ';' on Windows, ':' elsewhere.
char PathListSeparator();
std::string JoinPath(const std::string &base, const std::string &name);
std::string DirectoryName(const std::string &path);
bool FileExists(const std::string &path);
bool IsDirectory(const std::string &path);
bool ReadFileBytes(const std::string &path, std::vector<uint8_t> &out);

// ---- dynamic libraries -----------------------------------------------------
// "native" -> "libnative.so" (Linux) / "libnative.dylib" (macOS) / "native.dll".
std::string MapLibraryName(const std::string &base_name);
void *OpenLibrary(const std::string &file_name);
void *LookupSymbol(void *handle, const char *symbol);
void CloseLibrary(void *handle);

// Absolute path of the running executable (empty if it cannot be resolved).
std::string ExecutablePath();

// ---- diagnostics -----------------------------------------------------------
bool DebugEnabled();
void SetDebugEnabled(bool enabled);
void DebugPrint(const char *text);

} // namespace dk
