#include "stdafx.h"
#include "vm/executor/vmpool.h"

#include <cstdlib>
#include <cstring>

namespace
{
const size_t kHeaderSize = 16;
const int kClassCount = 16;   // 16, 32, ... 256 bytes
const int kCacheLimit = 1024; // blocks per class per thread

struct ThreadPool
{
	void *free_list[kClassCount];
	int cached[kClassCount];

	ThreadPool()
	{
		memset(free_list, 0, sizeof(free_list));
		memset(cached, 0, sizeof(cached));
	}

	~ThreadPool()
	{
		for (int i = 0; i < kClassCount; i++)
		{
			void *block = free_list[i];
			while (block)
			{
				void *next = *(void **)block;
				std::free(block);
				block = next;
			}
		}
	}
};

thread_local ThreadPool g_pool;

inline int ClassOf(size_t size)
{
	return static_cast<int>((size + 15) / 16) - 1;
}

inline size_t ClassSize(int cls)
{
	return static_cast<size_t>(cls + 1) * 16;
}
}

void *VmPoolAlloc(size_t size)
{
	if (size == 0 || size > kVmPoolMaxSmallSize)
	{
		return NULL;
	}
	int cls = ClassOf(size);
	void *block = g_pool.free_list[cls];
	if (block)
	{
		g_pool.free_list[cls] = *(void **)block;
		g_pool.cached[cls]--;
	}
	else
	{
		block = std::malloc(kHeaderSize + ClassSize(cls));
		if (!block)
		{
			return NULL;
		}
	}
	*(size_t *)block = static_cast<size_t>(cls + 1);
	return static_cast<char *>(block) + kHeaderSize;
}

void VmPoolFree(void *ptr)
{
	if (!ptr)
	{
		return;
	}
	char *block = static_cast<char *>(ptr) - kHeaderSize;
	int cls = static_cast<int>(*(size_t *)block) - 1;
	if (cls < 0 || cls >= kClassCount)
	{
		// Not a pool block; should be unreachable, but never leak it.
		std::free(block);
		return;
	}
	if (g_pool.cached[cls] >= kCacheLimit)
	{
		std::free(block);
		return;
	}
	*(void **)block = g_pool.free_list[cls];
	g_pool.free_list[cls] = block;
	g_pool.cached[cls]++;
}

size_t VmPoolCachedBytes()
{
	size_t total = 0;
	for (int i = 0; i < kClassCount; i++)
	{
		total += static_cast<size_t>(g_pool.cached[i]) * ClassSize(i);
	}
	return total;
}