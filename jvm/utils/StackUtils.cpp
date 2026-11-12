#include "stdafx.h"
#include "StackUtils.h"

template<class T>
JArrayStack<T>::JArrayStack()
{
	this->cur_size = DEFAULT_MIN_STACK_SIZE;
	this->min_size = DEFAULT_MIN_STACK_SIZE;
	this->max_size = DEFAULT_MAX_STACK_SIZE;
	init();
}

template<class T>
JArrayStack<T>::JArrayStack(int min_size, int max_size)
{
	this->cur_size = min_size;
	this->min_size = min_size;
	this->max_size = max_size;
	init();
}


template<class T>
bool JArrayStack<T>::push(T node)
{
	if (count + 1 >= this->cur_size)
	{
		if (this->cur_size < max_size)
		{
			this->cur_size = max_size;
			nodes = (T*)realloc(nodes, this->cur_size * sizeof(T));
		}
		else
		{
			throw new VMException("Out of memory.");
		}
	}
	nodes[count++] = node;
	return true;
}

template<class T>
T &JArrayStack<T>::pop()
{
	if (count <= 0)
	{
		throw new VMException("Out of memory.");
	}
	return nodes[--count];
}

template<class T>
T &JArrayStack<T>::peek()
{
	if (count <= 0)
	{
		throw new VMException("Out of memory.");
	}
	return nodes[count - 1];
}

template<class T>
int JArrayStack<T>::size()
{
	return count;
}