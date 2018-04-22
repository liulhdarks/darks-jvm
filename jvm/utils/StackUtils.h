#pragma once

#define DEFAULT_MIN_STACK_SIZE 10
#define DEFAULT_MAX_STACK_SIZE 20

#include "common.h"
#include "vm/exceptions.h"

template<class T>
class JVM_API JStack
{
protected:
	int cur_size;

public:
	JStack()
	{

	}

	virtual bool push(T node) = 0;

	virtual T &pop() = 0;

	virtual T &peek() = 0;

	virtual int size() = 0;

};

template<class T>
class JVM_API JArrayStack : public JStack<T>
{

private:
	int min_size;
	int max_size;
	T *nodes;
	int count;

private:
	void init()
	{
		count = 0;
		nodes = (T*)malloc(cur_size * sizeof(T));
		if (!nodes)
		{
			throw new VMException("Fail to initialize stack memory");
		}
	}

public:
	JArrayStack();

	JArrayStack(int min_size, int max_size);


	bool push(T node);

	T &pop();

	T &peek();

	int size();
};