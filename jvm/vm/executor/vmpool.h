#pragma once

#include <cstddef>

// Size-class pool for the short-lived structures the interpreter creates per
// Java call (StackFrame, OperStack, LocalVarTable and their slot arrays).
// Those sites dominate the malloc traffic of a cold start, and their sizes are
// small and repeated.  Blocks are cached per thread and the cache is capped so
// an idle thread cannot pin memory.  VmPoolAlloc returns NULL for requests
// above kMaxSmallSize or when malloc fails; callers either fall back to new or
// fail the same way they would without the pool.
const size_t kVmPoolMaxSmallSize = 256;

void *VmPoolAlloc(size_t size);
void VmPoolFree(void *ptr);

// Bytes currently held by this thread's free lists, for diagnostics.
size_t VmPoolCachedBytes();