# darks-jvm cross-platform build (Linux / macOS / Windows).
#
# Layout of the produced artifacts:
#   <build>/bin/darks-jvm          (or darks-jvm.exe)
#   <build>/lib/libnative.so       (libnative.dylib / native.dll)
#
# Dependencies: a C++11 compiler and libffi (used for portable native method
# dispatch). The Java tests additionally need a JDK (javac) at build time.

cmake_minimum_required(VERSION 3.10)
project(darks-jvm C CXX)

if(WIN32)
    # WSAPoll/WSAPOLLFD and other Vista+ APIs are gated on _WIN32_WINNT;
    # MinGW defaults to 0x0502 (Server 2003), so request Windows 7.
    add_definitions(-D_WIN32_WINNT=0x0601)
endif()

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib")
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib")

option(DARKS_JVM_ASAN "Build with AddressSanitizer" OFF)
option(DARKS_JVM_WARNINGS "Enable compiler warnings" ON)

# ---------------------------------------------------------------------------
# libffi
# ---------------------------------------------------------------------------
find_package(Threads REQUIRED)

find_package(PkgConfig QUIET)
if(PkgConfig_FOUND)
    pkg_check_modules(FFI QUIET libffi)
endif()
if(NOT FFI_FOUND)
    find_path(FFI_INCLUDE_DIR ffi.h)
    find_library(FFI_LIBRARY NAMES ffi libffi)
    if(NOT FFI_INCLUDE_DIR OR NOT FFI_LIBRARY)
        message(FATAL_ERROR "libffi not found. Install libffi (libffi-dev / brew install libffi).")
    endif()
    set(FFI_INCLUDE_DIRS "${FFI_INCLUDE_DIR}")
    set(FFI_LIBRARIES "${FFI_LIBRARY}")
endif()

# ---------------------------------------------------------------------------
# zlib (optional): required to read DEFLATE-compressed JAR entries.
# ---------------------------------------------------------------------------
find_package(ZLIB QUIET)
if(ZLIB_FOUND)
    add_definitions(-DDARKS_JVM_HAVE_ZLIB=1)
endif()

# ---------------------------------------------------------------------------
# Core VM library
# ---------------------------------------------------------------------------
set(DARKS_JVM_CORE_SOURCES
    platform/platform.cpp
    os/os.cpp
    jvm/env/env.cpp
    jvm/jni/jni.cpp
    jvm/jni/jni_ref.cpp
    jvm/jni/jni_array.cpp
    jvm/jni/jni_external.cpp
    jvm/jni/jni_object.cpp
    jvm/jni/jni_string.cpp
    jvm/jni/jni_vm.cpp
    jvm/utils/CommonUtils.cpp
    jvm/utils/StackUtils.cpp
    jvm/vm/classfiles/classloader.cpp
    jvm/vm/classfiles/constant.cpp
    jvm/vm/classfiles/parseclass.cpp
    jvm/vm/executor/attributes.cpp
    jvm/vm/executor/context.cpp
    jvm/vm/executor/deterministic.cpp
    jvm/vm/executor/descriptor.cpp
    jvm/vm/executor/executor.cpp
    jvm/vm/executor/javaexception.cpp
    jvm/vm/executor/jit.cpp
    jvm/vm/executor/jclass.cpp
    jvm/vm/executor/jfield.cpp
    jvm/vm/executor/jmethod.cpp
    jvm/vm/executor/jobject.cpp
    jvm/vm/executor/methodarea.cpp
    jvm/vm/executor/nativelibs.cpp
    jvm/vm/executor/objectheap.cpp
    jvm/vm/executor/operstack.cpp
    jvm/vm/executor/gc/cms_collector.cpp
    jvm/vm/executor/gc/gcbarrier.cpp
    jvm/vm/executor/gc/gccollector.cpp
    jvm/vm/executor/gc/marksweep_collector.cpp
    jvm/vm/executor/proxy.cpp
    jvm/vm/executor/scheduler.cpp
    jvm/vm/executor/stackframe.cpp
    jvm/vm/executor/threadstack.cpp
    jvm/vm/executor/vmmutex.cpp
    jvm/vm/io/bytes_io.cpp
    jvm/vm/io/zipreader.cpp
)

if(WIN32)
    list(APPEND DARKS_JVM_CORE_SOURCES os/win32/os_win32.cpp)
endif()

if(WIN32)
    add_library(darksvm SHARED ${DARKS_JVM_CORE_SOURCES})
    # The core is consumed by both the launcher and native.dll, so all of its
    # API has to leave the DLL: most classes are not annotated with JVM_API,
    # because on POSIX default visibility is enough.  Drop MinGW's "lib"
    # prefix so the artifacts are darksvm.dll / native.dll.
    set_target_properties(darksvm PROPERTIES PREFIX "")
    if(MSVC)
        set_target_properties(darksvm PROPERTIES WINDOWS_EXPORT_ALL_SYMBOLS ON)
    else()
        set_target_properties(darksvm PROPERTIES LINK_FLAGS "-Wl,--export-all-symbols")
    endif()
else()
    add_library(darksvm STATIC ${DARKS_JVM_CORE_SOURCES})
endif()

target_include_directories(darksvm PUBLIC
    "${CMAKE_CURRENT_SOURCE_DIR}"
    "${CMAKE_CURRENT_SOURCE_DIR}/jvm"
    "${CMAKE_CURRENT_SOURCE_DIR}/os"
    "${CMAKE_CURRENT_SOURCE_DIR}/platform"
    "${CMAKE_CURRENT_SOURCE_DIR}/jvm/jni"
    ${FFI_INCLUDE_DIRS}
)
target_include_directories(darksvm PRIVATE ${FFI_INCLUDE_DIRS})
target_link_libraries(darksvm PUBLIC ${FFI_LIBRARIES} Threads::Threads)
if(ZLIB_FOUND)
    target_link_libraries(darksvm PUBLIC ${ZLIB_LIBRARIES})
    target_include_directories(darksvm PUBLIC ${ZLIB_INCLUDE_DIRS})
endif()
if(NOT WIN32)
    target_link_libraries(darksvm PUBLIC ${CMAKE_DL_LIBS})
endif()

if(WIN32)
    # jvm/common.h selects dllexport/dllimport based on JVM_EXPORTS.  The
    # checked-in Visual Studio project used to define it; CMake names its
    # automatic export macro after the target (darksvm_EXPORTS), so define the
    # macro the headers actually check.
    target_compile_definitions(darksvm PRIVATE JVM_EXPORTS)
endif()

if(MSVC)
    target_compile_definitions(darksvm PUBLIC _CRT_SECURE_NO_WARNINGS)
else()
    target_compile_definitions(darksvm PUBLIC _FILE_OFFSET_BITS=64)
    if(DARKS_JVM_WARNINGS)
        target_compile_options(darksvm PRIVATE -Wall)
    endif()
endif()

if(DARKS_JVM_ASAN)
    target_compile_options(darksvm PUBLIC -fsanitize=address -fno-omit-frame-pointer)
    target_link_libraries(darksvm PUBLIC -fsanitize=address)
endif()

# ---------------------------------------------------------------------------
# Executable
# ---------------------------------------------------------------------------
add_executable(darks-jvm java/java.cpp)
target_link_libraries(darks-jvm PRIVATE darksvm)

if(MINGW)
    # The launcher defines wmain() on Windows so that command line arguments
    # are decoded as UTF-16; MinGW needs this flag to pick the matching CRT
    # entry point (MSVC selects it automatically).
    set_property(TARGET darks-jvm APPEND_STRING PROPERTY LINK_FLAGS " -municode")
endif()

# Export the VM symbols so that the native library (dlopen'ed at run time) can
# resolve them without linking a second copy of the VM state.
set_target_properties(darks-jvm PROPERTIES ENABLE_EXPORTS ON)

if(NOT WIN32)
    if(APPLE)
        set_target_properties(darks-jvm PROPERTIES
            BUILD_RPATH "@loader_path/../lib"
            INSTALL_RPATH "@loader_path/../lib")
    else()
        set_target_properties(darks-jvm PROPERTIES
            BUILD_RPATH "$ORIGIN/../lib"
            INSTALL_RPATH "$ORIGIN/../lib")
    endif()
endif()

if(DARKS_JVM_ASAN AND NOT MSVC)
    target_compile_options(darks-jvm PRIVATE -fsanitize=address -fno-omit-frame-pointer)
    target_link_libraries(darks-jvm PRIVATE -fsanitize=address)
endif()

# ---------------------------------------------------------------------------
# Native method library
# ---------------------------------------------------------------------------
set(DARKS_JVM_NATIVE_SOURCES
    native/native.cpp
    native/java/lang/core_md.cpp
    native/java/lang/proxy_md.cpp
    native/java/lang/reflect_md.cpp
    native/java/lang/system_md.cpp
    native/java/lang/thread_md.cpp
    native/java/lang/string_md.cpp
    native/java/io/file_md.cpp
    native/java/io/printstream.cpp
    native/java/io/randomaccessfile_md.cpp
    native/java/net/socket_md.cpp
    native/java/util/regex_md.cpp
    native/java/util/zip/inflater_md.cpp
    native/java/util/zip/zipfile_md.cpp
    native/java/util/concurrent/structured_scope_md.cpp
    native/darks/deterministic_md.cpp
)

add_library(native SHARED ${DARKS_JVM_NATIVE_SOURCES})
target_include_directories(native PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}"
    "${CMAKE_CURRENT_SOURCE_DIR}/jvm"
    "${CMAKE_CURRENT_SOURCE_DIR}/os"
    "${CMAKE_CURRENT_SOURCE_DIR}/platform"
    "${CMAKE_CURRENT_SOURCE_DIR}/jvm/jni"
)
if(ZLIB_FOUND)
    target_link_libraries(native PRIVATE ${ZLIB_LIBRARIES})
    target_include_directories(native PRIVATE ${ZLIB_INCLUDE_DIRS})
endif()
find_path(PCRE2_INCLUDE_DIR NAMES pcre2.h)
find_library(PCRE2_32_LIBRARY NAMES pcre2-32)
if(PCRE2_INCLUDE_DIR AND PCRE2_32_LIBRARY)
    target_include_directories(native PRIVATE ${PCRE2_INCLUDE_DIR})
    target_link_libraries(native PRIVATE ${PCRE2_32_LIBRARY})
    target_compile_definitions(native PRIVATE DARKS_HAVE_PCRE2=1)
endif()
if(WIN32)
    # MapLibraryName() looks for "native.dll"; MinGW would emit libnative.dll.
    set_target_properties(native PROPERTIES PREFIX "")
    target_link_libraries(native PRIVATE darksvm ws2_32)
    # JNI entry points are plain C symbols and a missing JNIEXPORT would make
    # them invisible to GetProcAddress.  Export the whole table so that a
    # definition that forgets the attribute cannot silently disappear.
    if(MSVC)
        set_target_properties(native PROPERTIES WINDOWS_EXPORT_ALL_SYMBOLS ON)
    else()
        set_target_properties(native PROPERTIES
            LINK_FLAGS "-Wl,--export-all-symbols")
    endif()
else()
    # On POSIX the symbols are provided by the darks-jvm executable (built with
    # ENABLE_EXPORTS); leave them undefined in the shared object.
    if(APPLE)
        set_target_properties(native PROPERTIES LINK_FLAGS "-undefined dynamic_lookup")
        set_target_properties(native PROPERTIES
            INSTALL_RPATH "@loader_path/../lib")
    else()
        set_target_properties(native PROPERTIES
            BUILD_RPATH "$ORIGIN/../lib"
            INSTALL_RPATH "$ORIGIN/../lib")
    endif()
endif()

if(DARKS_JVM_ASAN AND NOT MSVC)
    target_compile_options(native PRIVATE -fsanitize=address -fno-omit-frame-pointer)
    target_link_libraries(native PRIVATE -fsanitize=address)
endif()

# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------
option(DARKS_JVM_BUILD_TESTS "Build the test suite" ON)
if(DARKS_JVM_BUILD_TESTS AND NOT CMAKE_CROSSCOMPILING)
    enable_testing()
    add_subdirectory(tests)
elseif(DARKS_JVM_BUILD_TESTS)
    # The test suite links against the host JDK's jni.h/jni_md.h and runs the
    # produced binaries, so it cannot be built for another target platform
    # without a JDK for that platform. Native builds (including MinGW on
    # Windows) keep the suite enabled.
    message(STATUS "Cross-compiling: skipping test suite (needs a JDK for the target)")
endif()
