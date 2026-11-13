#!/usr/bin/env bash
#
# Reproducible performance harness for the object-layout phase.
#
#   BUILD_DIR=build scripts/perf.sh [label]
#
# Compiles perf/src with javac and runs every benchmark REPS times (default 3)
# against BUILD_DIR/bin/darks-jvm.  The median of each metric is written to
# $OUT_DIR/<label>.tsv (default OUT_DIR=<repo>/perf-results).
#
# Environment knobs: N_MEM (300000), N_FIELD (2000000), N_CALL (500000),
# N_ALLOC (300000), REPS (3).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="${BUILD_DIR:-${ROOT_DIR}/build}"
LABEL="${1:-run}"
JVM="${BUILD_DIR}/bin/darks-jvm"
CLASSES="${BUILD_DIR}/classes"
PC="${BUILD_DIR}/perf-classes"
OUT_DIR="${OUT_DIR:-${ROOT_DIR}/perf-results}"
N_MEM="${N_MEM:-300000}"
N_FIELD="${N_FIELD:-2000000}"
N_CALL="${N_CALL:-500000}"
N_ALLOC="${N_ALLOC:-300000}"
REPS="${REPS:-3}"

if [ ! -x "${JVM}" ] || [ ! -d "${CLASSES}" ]; then
    echo "missing ${JVM} or ${CLASSES}; build the VM first" >&2
    exit 1
fi

mkdir -p "${OUT_DIR}/raw-${LABEL}" "${PC}"
javac --release 8 -d "${PC}" "${ROOT_DIR}"/perf/src/*.java 2>/dev/null

RAW="${OUT_DIR}/raw-${LABEL}"

run_once() {
    local name="$1"; shift
    local rep="$1"; shift
    local env_args=()
    while [ "$1" != "--" ]; do
        env_args+=("$1")
        shift
    done
    shift
    env "${env_args[@]}" "${JVM}" -cp "${CLASSES}:${PC}" "$@" > "${RAW}/${name}.${rep}.txt"
}

for rep in $(seq 1 "${REPS}"); do
    run_once mem "${rep}" DARKS_JVM_GC=0 -- PerfMemory "${N_MEM}"
    run_once mem_long "${rep}" DARKS_JVM_GC=0 -- PerfMemoryLong "${N_MEM}"
    run_once fields_jit0 "${rep}" DARKS_JVM_GC=0 DARKS_JVM_JIT=0 -- PerfFields "${N_FIELD}"
    run_once fields_jit1 "${rep}" DARKS_JVM_GC=0 DARKS_JVM_JIT=1 -- PerfFields "${N_FIELD}"
    run_once virtual "${rep}" DARKS_JVM_GC=0 -- PerfVirtual "${N_CALL}"
    run_once alloc "${rep}" DARKS_JVM_GC=0 -- PerfAlloc "${N_ALLOC}"
done

python3 - "${RAW}" "${OUT_DIR}/${LABEL}.tsv" "${REPS}" <<'PYEOF'
import os, statistics, sys

raw, out, reps = sys.argv[1], sys.argv[2], int(sys.argv[3])
derived = {}


def metric(bench, key, rep):
    path = os.path.join(raw, "%s.%d.txt" % (bench, rep))
    for line in open(path):
        if "=" in line:
            k, v = line.strip().split("=", 1)
            if k == key:
                return float(v)
    raise SystemExit("missing %s in %s" % (key, path))


for rep in range(1, reps + 1):
    for bench in ("mem", "mem_long"):
        n = metric(bench, "objects", rep)
        delta = metric(bench, "rss_delta_kb", rep) * 1024.0
        bucket = derived.setdefault(bench, {})
        bucket.setdefault("bytes_per_object", []).append(int((delta - n * 8.0) // n))
        for key in ("rss_delta_kb", "rss_after_kb", "alloc_ns"):
            bucket.setdefault(key, []).append(metric(bench, key, rep))

    for bench in ("fields_jit0", "fields_jit1"):
        bucket = derived.setdefault(bench, {})
        ops = metric(bench, "read_ops", rep)
        ns = metric(bench, "read_ns", rep)
        bucket.setdefault("read_mops", []).append(ops / ns * 1000.0)
        ops = metric(bench, "write_ops", rep)
        ns = metric(bench, "write_ns", rep)
        bucket.setdefault("write_mops", []).append(ops / ns * 1000.0)

    calls = metric("virtual", "calls", rep)
    ns = metric("virtual", "call_ns", rep)
    derived.setdefault("virtual", {}).setdefault("call_mops", []).append(calls / ns * 1000.0)

    objs = metric("alloc", "objects", rep)
    ns = metric("alloc", "alloc_ns", rep)
    bucket = derived.setdefault("alloc", {})
    bucket.setdefault("alloc_kops_per_s", []).append(objs * 1e6 / ns)
    for key in ("gc_live_ns", "gc_dead_ns"):
        bucket.setdefault(key.replace("_ns", "_ms"), []).append(
            metric("alloc", key, rep) / 1e6)

order = [
    ("mem", "rss_delta_kb"), ("mem", "bytes_per_object"), ("mem", "alloc_ns"),
    ("mem_long", "rss_delta_kb"), ("mem_long", "bytes_per_object"),
    ("fields_jit0", "read_mops"), ("fields_jit0", "write_mops"),
    ("fields_jit1", "read_mops"), ("fields_jit1", "write_mops"),
    ("virtual", "call_mops"),
    ("alloc", "alloc_kops_per_s"), ("alloc", "gc_live_ms"), ("alloc", "gc_dead_ms"),
]

with open(out, "w") as fh:
    fh.write("benchmark\tmetric\tvalue\n")
    for bench, key in order:
        value = statistics.median(derived[bench][key])
        if key in ("rss_delta_kb", "bytes_per_object", "alloc_ns"):
            text = "%d" % value
        elif key in ("gc_live_ms", "gc_dead_ms"):
            text = "%.1f" % value
        else:
            text = "%.3f" % value
        fh.write("%s\t%s\t%s\n" % (bench, key, text))
        print("%-12s %-20s %s" % (bench, key, text))
PYEOF

echo "== wrote ${OUT_DIR}/${LABEL}.tsv =="
