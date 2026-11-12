#include "platform.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>

#if defined(_WIN32)
#include <windows.h>
#else
#include <dlfcn.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>
#include <climits>
#if defined(__APPLE__)
#include <mach-o/dyld.h>
#endif
#endif

namespace dk {

// ---------------------------------------------------------------------------
// UTF-8 <-> UTF-16
// ---------------------------------------------------------------------------
std::vector<uint16_t> Utf8ToUtf16(const char *utf8, std::size_t byte_len) {
    std::vector<uint16_t> out;
    if (!utf8) {
        return out;
    }
    out.reserve(byte_len);
    std::size_t i = 0;
    while (i < byte_len) {
        unsigned char c = static_cast<unsigned char>(utf8[i]);
        uint32_t cp = 0;
        std::size_t extra = 0;
        if (c < 0x80) {
            cp = c;
        } else if ((c & 0xE0) == 0xC0) {
            cp = c & 0x1F;
            extra = 1;
        } else if ((c & 0xF0) == 0xE0) {
            cp = c & 0x0F;
            extra = 2;
        } else if ((c & 0xF8) == 0xF0) {
            cp = c & 0x07;
            extra = 3;
        } else {
            // Invalid lead byte: emit U+FFFD and advance one byte.
            out.push_back(0xFFFD);
            ++i;
            continue;
        }
        if (i + extra >= byte_len) {
            out.push_back(0xFFFD);
            break;
        }
        bool valid = true;
        for (std::size_t k = 1; k <= extra; ++k) {
            unsigned char cc = static_cast<unsigned char>(utf8[i + k]);
            if ((cc & 0xC0) != 0x80) {
                valid = false;
                break;
            }
            cp = (cp << 6) | (cc & 0x3F);
        }
        if (!valid) {
            out.push_back(0xFFFD);
            ++i;
            continue;
        }
        i += extra + 1;
        if (cp <= 0xFFFF) {
            out.push_back(static_cast<uint16_t>(cp));
        } else if (cp <= 0x10FFFF) {
            cp -= 0x10000;
            out.push_back(static_cast<uint16_t>(0xD800 + (cp >> 10)));
            out.push_back(static_cast<uint16_t>(0xDC00 + (cp & 0x3FF)));
        } else {
            out.push_back(0xFFFD);
        }
    }
    return out;
}

std::vector<uint16_t> Utf8ToUtf16(const std::string &utf8) {
    return Utf8ToUtf16(utf8.data(), utf8.size());
}

std::string Utf16ToUtf8(const uint16_t *units, std::size_t count) {
    std::string out;
    if (!units) {
        return out;
    }
    out.reserve(count);
    for (std::size_t i = 0; i < count; ++i) {
        uint32_t cp = units[i];
        if (cp >= 0xD800 && cp <= 0xDBFF && i + 1 < count &&
            units[i + 1] >= 0xDC00 && units[i + 1] <= 0xDFFF) {
            cp = 0x10000 + ((cp - 0xD800) << 10) + (units[i + 1] - 0xDC00);
            ++i;
        } else if (cp >= 0xD800 && cp <= 0xDFFF) {
            cp = 0xFFFD;
        }
        if (cp < 0x80) {
            out.push_back(static_cast<char>(cp));
        } else if (cp < 0x800) {
            out.push_back(static_cast<char>(0xC0 | (cp >> 6)));
            out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
        } else if (cp < 0x10000) {
            out.push_back(static_cast<char>(0xE0 | (cp >> 12)));
            out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
            out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
        } else {
            out.push_back(static_cast<char>(0xF0 | (cp >> 18)));
            out.push_back(static_cast<char>(0x80 | ((cp >> 12) & 0x3F)));
            out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
            out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
        }
    }
    return out;
}

std::string Utf16ToUtf8(const std::vector<uint16_t> &units) {
    return Utf16ToUtf8(units.empty() ? nullptr : units.data(), units.size());
}

// ---------------------------------------------------------------------------
// Clocks
// ---------------------------------------------------------------------------
uint64_t CurrentTimeMillis() {
#if defined(_WIN32)
    FILETIME ft;
    GetSystemTimeAsFileTime(&ft);
    uint64_t ticks = (static_cast<uint64_t>(ft.dwHighDateTime) << 32) | ft.dwLowDateTime;
    // 100-ns intervals since 1601-01-01 -> ms since 1970-01-01.
    return (ticks - 116444736000000000ULL) / 10000ULL;
#else
    struct timespec ts;
    if (clock_gettime(CLOCK_REALTIME, &ts) != 0) {
        return 0;
    }
    return static_cast<uint64_t>(ts.tv_sec) * 1000ULL +
           static_cast<uint64_t>(ts.tv_nsec) / 1000000ULL;
#endif
}

uint64_t NanoTime() {
#if defined(_WIN32)
    LARGE_INTEGER freq, counter;
    if (!QueryPerformanceFrequency(&freq) || !QueryPerformanceCounter(&counter)) {
        return 0;
    }
    return static_cast<uint64_t>((counter.QuadPart * 1000000000LL) / freq.QuadPart);
#else
    struct timespec ts;
    if (clock_gettime(CLOCK_MONOTONIC, &ts) != 0) {
        return 0;
    }
    return static_cast<uint64_t>(ts.tv_sec) * 1000000000ULL +
           static_cast<uint64_t>(ts.tv_nsec);
#endif
}

// ---------------------------------------------------------------------------
// Paths
// ---------------------------------------------------------------------------
char PathListSeparator() {
#if defined(_WIN32)
    return ';';
#else
    return ':';
#endif
}

std::string JoinPath(const std::string &base, const std::string &name) {
    if (base.empty()) {
        return name;
    }
    char last = base[base.size() - 1];
    if (last == '/' || last == '\\') {
        return base + name;
    }
#if defined(_WIN32)
    return base + "\\" + name;
#else
    return base + "/" + name;
#endif
}

std::string DirectoryName(const std::string &path) {
    std::size_t pos = path.find_last_of("/\\");
    if (pos == std::string::npos) {
        return std::string();
    }
    return path.substr(0, pos);
}

bool FileExists(const std::string &path) {
#if defined(_WIN32)
    DWORD attr = GetFileAttributesA(path.c_str());
    return attr != INVALID_FILE_ATTRIBUTES && (attr & FILE_ATTRIBUTE_DIRECTORY) == 0;
#else
    struct stat st;
    return stat(path.c_str(), &st) == 0 && S_ISREG(st.st_mode);
#endif
}

bool ReadFileBytes(const std::string &path, std::vector<uint8_t> &out) {
    // Always open in binary read-only mode: the original code used "rb+" which
    // fails on read-only files.
    FILE *fp = std::fopen(path.c_str(), "rb");
    if (!fp) {
        return false;
    }
    if (std::fseek(fp, 0, SEEK_END) != 0) {
        std::fclose(fp);
        return false;
    }
    long len = std::ftell(fp);
    if (len < 0) {
        std::fclose(fp);
        return false;
    }
    std::rewind(fp);
    out.resize(static_cast<std::size_t>(len));
    if (len > 0 && std::fread(out.data(), 1, out.size(), fp) != out.size()) {
        std::fclose(fp);
        out.clear();
        return false;
    }
    std::fclose(fp);
    return true;
}

// ---------------------------------------------------------------------------
// Dynamic libraries
// ---------------------------------------------------------------------------
std::string MapLibraryName(const std::string &base_name) {
#if defined(_WIN32)
    return base_name + ".dll";
#elif defined(__APPLE__)
    return "lib" + base_name + ".dylib";
#else
    return "lib" + base_name + ".so";
#endif
}

void *OpenLibrary(const std::string &file_name) {
#if defined(_WIN32)
    return static_cast<void *>(LoadLibraryA(file_name.c_str()));
#else
    return dlopen(file_name.c_str(), RTLD_NOW | RTLD_LOCAL);
#endif
}

void *LookupSymbol(void *handle, const char *symbol) {
    if (!handle || !symbol) {
        return nullptr;
    }
#if defined(_WIN32)
    return reinterpret_cast<void *>(GetProcAddress(static_cast<HMODULE>(handle), symbol));
#else
    return dlsym(handle, symbol);
#endif
}

void CloseLibrary(void *handle) {
    if (!handle) {
        return;
    }
#if defined(_WIN32)
    FreeLibrary(static_cast<HMODULE>(handle));
#else
    dlclose(handle);
#endif
}

std::string ExecutablePath() {
#if defined(_WIN32)
    char buffer[MAX_PATH];
    DWORD len = GetModuleFileNameA(nullptr, buffer, MAX_PATH);
    if (len == 0 || len >= MAX_PATH) {
        return std::string();
    }
    return std::string(buffer, len);
#elif defined(__APPLE__)
    uint32_t size = 0;
    _NSGetExecutablePath(nullptr, &size);
    std::vector<char> buffer(size + 1, 0);
    if (_NSGetExecutablePath(buffer.data(), &size) != 0) {
        return std::string();
    }
    char resolved[PATH_MAX];
    if (realpath(buffer.data(), resolved) == nullptr) {
        return std::string(buffer.data());
    }
    return std::string(resolved);
#else
    char buffer[4096];
    ssize_t len = readlink("/proc/self/exe", buffer, sizeof(buffer) - 1);
    if (len <= 0) {
        return std::string();
    }
    buffer[len] = '\0';
    return std::string(buffer, static_cast<std::size_t>(len));
#endif
}

// ---------------------------------------------------------------------------
// Diagnostics
// ---------------------------------------------------------------------------
namespace {
bool g_debug_enabled = false;
}

bool DebugEnabled() {
    return g_debug_enabled;
}

void SetDebugEnabled(bool enabled) {
    g_debug_enabled = enabled;
}

void DebugPrint(const char *text) {
    if (!text || !g_debug_enabled) {
        return;
    }
#if defined(_WIN32)
    OutputDebugStringA(text);
#else
    std::fputs(text, stderr);
    std::fflush(stderr);
#endif
}

} // namespace dk
