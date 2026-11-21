#pragma once

// Deterministic execution support (B5).
//
// The VM is single-worker coroutine scheduled by default, but timers, the
// wall clock, java.util.Random seeds and a handful of collection iteration
// orders still introduce run-to-run variation.  This module provides the
// opt-in deterministic mode used by the B5 tests:
//
//   -Ddarks.deterministic=<seed>   virtual clock + seeded ready-queue order
//   -Ddarks.record=<file>          additionally write the decision log
//   -Ddarks.replay=<file>          verify a previous decision log
//
// The decision log records the choices that make an execution unique:
// coroutine scheduling, monitor grants, timer fires, Random draws, clock
// reads, GC points and JNI/real-I/O boundaries.  Events are chained into an
// FNV-1a hash so a run can be compared by a single number.
//
// Nothing here is active unless one of the properties above is present, so
// the default (non-deterministic) behaviour is unchanged.

#include <stdint.h>
#include <stddef.h>
#include <cstdio>
#include <mutex>
#include <string>
#include <vector>

class DarksDeterministic
{
public:
	static DarksDeterministic &Instance();

	// Reads the -D properties.  Idempotent; safe to call from any thread.
	void Initialize();

	bool Enabled() const { return enabled; }
	bool ReplayEnabled() const { return !replay_path.empty(); }
	bool RecordEnabled() const { return !record_path.empty(); }
	uint64_t Seed() const { return seed; }

	// ---- virtual clock ---------------------------------------------------
	// Silent clock used by the scheduler (timers, sleep deadlines).
	int64_t NowNs();
	// Advance the virtual clock; never moves backwards.
	void AdvanceTo(int64_t t);
	// Java-facing reads: identical to the silent clock in deterministic mode
	// but recorded in the decision log.  Outside deterministic mode they must
	// not be used (callers keep the real implementation).
	int64_t NanoTime();
	int64_t CurrentTimeMillis();

	// ---- seeded scheduling order ----------------------------------------
	// Stable rank for a coroutine id: the ready queue orders by
	// (priority, rank, enqueue sequence), so a fixed seed reproduces the same
	// interleaving while different seeds explore different ones.
	uint64_t OrderFor(uint64_t coroutine_id) const;
	uint64_t Mix64(uint64_t value) const;

	// ---- decision log ----------------------------------------------------
	// Records (or verifies) one event.  a/b carry the event payload.
	void Event(char kind, uint64_t a, uint64_t b = 0);
	// Random hook: records the draw, or returns the recorded value while
	// replaying.  Called from java.util.Random.next(int).
	int NextRandom(int bits, int value);
	// Marks the point where execution leaves deterministic territory
	// (JNI calls, real file I/O).
	void Boundary(const char *what);

	uint64_t EventHash() const { return hash; }
	uint64_t EventCount() const { return count; }
	bool ReplayFailed() const { return replay_failed; }

	// Writes the record trailer and prints the run summary.  Called once by
	// the launcher after the main class returns.
	void Finish();

private:
	DarksDeterministic();
	~DarksDeterministic();
	DarksDeterministic(const DarksDeterministic &);
	DarksDeterministic &operator=(const DarksDeterministic &);

	struct Entry
	{
		Entry() : kind('?'), a(0), b(0) {}
		Entry(char k, uint64_t va, uint64_t vb) : kind(k), a(va), b(vb) {}
		char kind;
		uint64_t a;
		uint64_t b;
	};

	void InitializeLocked();
	void MixLocked(const Entry &entry);
	void AppendLocked(char kind, uint64_t a, uint64_t b);
	bool LoadReplayLocked(const std::string &path);
	void CloseRecordLocked();

	mutable std::mutex mu;
	bool initialized;
	bool enabled;
	bool replay;
	bool finished;
	uint64_t seed;
	int64_t vnow;
	int64_t epoch_ms;

	std::string record_path;
	std::string replay_path;
	FILE *record_fp;

	// Events produced by this run (used for the hash and for record files).
	uint64_t hash;
	uint64_t count;
	// Events loaded from a replay file, compared as they are produced.
	std::vector<Entry> expected;
	size_t expected_pos;
	bool replay_failed;
	uint64_t expected_hash;
	uint64_t expected_count;
};

// Convenience used by the scheduler and natives.
inline DarksDeterministic &Deterministic()
{
	return DarksDeterministic::Instance();
}
