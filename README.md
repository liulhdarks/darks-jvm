# darks-jvm

A small JVM written in C++ for learning purposes. Originally a Visual Studio 2010
project for Windows; this branch adds a portable build so it also compiles and
runs on Linux and macOS.

## What this branch changes

* CMake build for Linux, macOS and Windows (MSVC) with CTest integration.
* Platform layer (`platform/platform.cpp`): UTF-8/UTF-16 conversion, path
  separators, time, dynamic library loading and executable path.
* Portable native-method dispatch using [libffi](https://sourceware.org/libffi/)
  instead of Win32 x86 inline assembly. The assembly path is only compiled in
  the MSVC build.
* Fixed-width class-file types (`U1/U2/U4/U8`, `jint`) so the VM is correct on
  LP64 platforms (Linux/macOS), where `long` is 64-bit.
* Class path handling via `-cp`/`-classpath` and the `DARKS_JVM_CLASSPATH`
  environment variable, using the platform path separator (`:` or `;`).
* `--dump` command line option to print a parsed class file.
* Bug fixes: freeing string literals, double free of `Utf8Constant`, wrong
  `delete[]` on the source file, skipping unknown class attributes and using
  `fopen("rb+")` for read-only files.

## Requirements

| Platform | Compiler | Extra packages |
|----------|----------|----------------|
| Linux    | GCC/Clang (C++11) | `cmake`, `libffi-dev`, a JDK (`javac`) for the tests |
| macOS    | Apple Clang (C++11) | `cmake`, `libffi` (`brew install libffi`), a JDK |
| Windows  | MSVC 2015+ (C++11) | `cmake`, libffi (vcpkg), a JDK |

CMake 3.10 or newer is required. The JDK is only needed to compile the small
Java runtime and the test programs; the VM itself does not use it.

## Build

### Linux

```bash
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build -j
```

Artifacts:

* `build/bin/darks-jvm`
* `build/lib/libnative.so`
* `build/classes/` (compiled Java runtime and tests)

### macOS

```bash
brew install libffi cmake
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release \
      -DCMAKE_PREFIX_PATH="$(brew --prefix libffi)"
cmake --build build -j
```

Artifacts:

* `build/bin/darks-jvm`
* `build/lib/libnative.dylib`

The executable is linked with an RPATH of `@loader_path/../lib`, so it finds
`libnative.dylib` next to it when run from `build/bin/`.

### Windows (MSVC)

From a *Developer Command Prompt*:

```bat
cmake -S . -B build -G "Visual Studio 16 2019" -A x64 ^
      -DCMAKE_PREFIX_PATH=C:\vcpkg\installed\x64-windows
cmake --build build --config Release
```

Artifacts (in `build/bin/Release` and `build/lib/Release`):

* `darks-jvm.exe`
* `native.dll`

## Run

```bash
# Run a class from a directory
build/bin/darks-jvm -cp build/classes Hello

# Dump the parsed structure of a class file
build/bin/darks-jvm -cp build/classes --dump Hello

# Use the environment variable instead of -cp
DARKS_JVM_CLASSPATH=build/classes build/bin/darks-jvm Hello
```

Exit codes:

| Code | Meaning |
|------|---------|
| 0 | success |
| 2 | invalid command line usage |
| 3 | internal/VM error |
| 4 | main class not found |

On Windows use `set DARKS_JVM_CLASSPATH=...` and `;` as the path separator.

## Tests

```bash
ctest --test-dir build --output-on-failure
```

The suite contains one unit-test binary (54 assertions covering UTF-8
conversion, path helpers and class-file parsing) plus end-to-end CLI tests:
`--dump`, running `Hello`, the native `System.currentTimeMillis()` path,
missing-class exit code 4, `DARKS_JVM_CLASSPATH`, `--version` and usage errors.

The all-in-one helper configures, builds and runs every check:

```bash
scripts/verify.sh
```

AddressSanitizer build:

```bash
BUILD_DIR=build-asan DARKS_JVM_ASAN=ON scripts/verify.sh
```

## macOS notes

The Linux build and the full test suite were verified on this branch. The macOS
build uses the same sources and CMake logic (Apple Clang, `libnative.dylib`,
`@loader_path/../lib` RPATH, `-undefined dynamic_lookup` for the native library)
but has not yet been run on a Mac. To test it:

```bash
git clone <repo-url> darks-jvm
cd darks-jvm
git checkout adapt/cross-platform
brew install libffi cmake
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release \
      -DCMAKE_PREFIX_PATH="$(brew --prefix libffi)"
cmake --build build -j
ctest --test-dir build --output-on-failure
build/bin/darks-jvm -cp build/classes Hello
```

## License

See [LICENSE](LICENSE).
