#pragma once

#include "common.h"

#include <string>

// Owns its message. The original implementation stored the caller's pointer
// and called "delete" on it, which crashed for string literals and double-freed
// dynamically built messages.
class VMException
{

private:
	std::string cause;

	int errCode;

public:
	VMException()
	{
		errCode = 0;
	}

	VMException(LPSTR cause)
	{
		this->cause = cause ? cause : "";
		errCode = 0;
	}

	VMException(LPSTR cause, int errCode)
	{
		this->cause = cause ? cause : "";
		this->errCode = errCode;
	}

	virtual ~VMException()
	{
	}

	LPCSTR what() const
	{
		return cause.c_str();
	}

	int code() const
	{
		return errCode;
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
