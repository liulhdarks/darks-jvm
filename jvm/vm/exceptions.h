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

// The class itself could not be located.  JNI boundaries translate this into
// java.lang.ClassNotFoundException.
class MissingClassException : public VMException
{
public:
	std::string missing;

	explicit MissingClassException(const std::string &name)
		: VMException(const_cast<LPSTR>(name.c_str())), missing(name)
	{
	}
};

// A superclass or superinterface could not be resolved while linking a class
// that was found itself.  The JVM reports this as NoClassDefFoundError with a
// ClassNotFoundException cause naming the missing type (JVMS 5.3.5).
class ClassLinkageException : public VMException
{
public:
	std::string missing;

	explicit ClassLinkageException(const std::string &name)
		: VMException(const_cast<LPSTR>(name.c_str())), missing(name)
	{
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
