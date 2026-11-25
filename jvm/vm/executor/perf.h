#pragma once

#include "common.h"

// Startup performance counters/phase timers.
//
// Enabled by DARKS_PERF=1 (read once per process). When disabled every hook is
// a single well-predicted branch on a cached flag; no counters are touched.
//
// The summary is emitted from VMContext::destroy() after the scheduler has
// joined its workers, so the consolidated numbers do not race with thread
// exit. Per-thread instruction counters are merged into the global totals by
// the thread's accumulator destructor.
namespace darksperf {

extern bool g_enabled;

inline bool Enabled()
{
	return g_enabled;
}

enum Phase
{
	PHASE_VM_INIT = 0,
	PHASE_MAIN_CLASS,
	PHASE_MAIN,
	PHASE_SHUTDOWN,
	PHASE_COUNT
};

JVM_API void BeginPhase(Phase phase);
JVM_API void EndPhase(Phase phase);

JVM_API void NoteClassCacheHit();
JVM_API void NoteClassCacheMiss();
JVM_API void AddClassReadNs(uint64_t ns);
JVM_API void NoteClassRead();
JVM_API void AddClassParseNs(uint64_t ns);
JVM_API void NoteClassParsed();
JVM_API void AddClassLinkNs(uint64_t ns);
JVM_API void NoteClassLinked();
JVM_API void AddClinitNs(uint64_t ns);
JVM_API void NoteClinit();

JVM_API void NoteMethod();
// One executed bytecode; "op" is the JVM opcode (aggregated per opcode).
JVM_API void NoteInstruction(unsigned op);

JVM_API void BeginGc();
JVM_API void AddGcPauseNs(uint64_t ns);
JVM_API void AddAllocated(size_t bytes);

// First accepted connection (the first HTTP request in a server workload).
JVM_API void NoteFirstConnection();

// One newly compiled JIT program and its retained size in bytes.
JVM_API void AddJitProgram(size_t bytes);

JVM_API void PrintSummary();

} // namespace darksperf
