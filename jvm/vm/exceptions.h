#pragma once

#include "common.h"

class VMException
{

private:
	LPSTR cause;

	int errCode;

public:
	VMException()
	{
		cause = NULL;
	}

	VMException(LPSTR cause)
	{
		this->cause = cause;
	}

	VMException(LPSTR cause, int errCode)
	{
		this->cause = cause;
		this->errCode = errCode;
	}

	~VMException()
	{
		if (cause)
		{
			delete cause;
			cause = NULL;
		}
	}
};

class BytesException : public VMException
{


public:
	BytesException() : VMException()
	{
		
	}

	BytesException(LPSTR cause) : VMException(cause)
	{
		
	}

	BytesException(LPSTR cause, int errCode) : VMException(cause, errCode)
	{
		
	}
};

class VMExecutorException : public VMException
{


public:
	VMExecutorException() : VMException()
	{

	}

	VMExecutorException(LPSTR cause) : VMException(cause)
	{

	}

	VMExecutorException(LPSTR cause, int errCode) : VMException(cause, errCode)
	{

	}
};