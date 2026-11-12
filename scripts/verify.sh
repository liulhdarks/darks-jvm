#!/usr/bin/env bash
#
# Build and verify darks-jvm on the current platform.
#
# Usage:
#   scripts/verify.sh              # configure + build + ctest + E2E
#   BUILD_DIR=build-asan DARKS_JVM_ASAN=ON scripts/verify.sh
#
# Requirements: CMake >= 3.10, a C++11 compiler, libffi development files and
# a JDK providing javac (the tests compile a tiny Java runtime at build time).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="${BUILD_DIR:-${ROOT_DIR}/build}"
JOBS="${JOBS:-2}"
CMAKE_BUILD_TYPE="${CMAKE_BUILD_TYPE:-Release}"
ASAN_FLAG=OFF
if [ "${DARKS_JVM_ASAN:-}" = "ON" ]; then
    ASAN_FLAG=ON
fi

echo "== configure =="
cmake -S "${ROOT_DIR}" -B "${BUILD_DIR}" \
      -DCMAKE_BUILD_TYPE="${CMAKE_BUILD_TYPE}" \
      -DDARKS_JVM_ASAN="${ASAN_FLAG}"

echo "== build =="
cmake --build "${BUILD_DIR}" -j "${JOBS}"

CLASSES_DIR="${BUILD_DIR}/classes"
JVM="${BUILD_DIR}/bin/darks-jvm"

echo "== ctest =="
ctest --test-dir "${BUILD_DIR}" --output-on-failure

echo "== end-to-end =="
"${JVM}" -cp "${CLASSES_DIR}" --dump Hello | grep -q "this_class:  Hello"
echo "  --dump Hello: ok"

hello_out="$("${JVM}" -cp "${CLASSES_DIR}" Hello)"
[ "${hello_out}" = "Hello, darks-jvm!" ]
echo "  Hello: ${hello_out}"

clock_out="$("${JVM}" -cp "${CLASSES_DIR}" Clock)"
echo "${clock_out}" | grep -q "entered native System.currentTimeMillis()"
echo "${clock_out}" | grep -q "clock: ok"
echo "  Clock: ok"

set +e
"${JVM}" -cp "${CLASSES_DIR}" NoSuchClass >/dev/null 2>&1
missing_rc=$?
set -e
[ "${missing_rc}" -eq 4 ]
echo "  missing class exit code: ${missing_rc}"

env_out="$(DARKS_JVM_CLASSPATH="${CLASSES_DIR}" "${JVM}" Hello)"
[ "${env_out}" = "Hello, darks-jvm!" ]
echo "  DARKS_JVM_CLASSPATH: ok"

echo "== all checks passed =="
