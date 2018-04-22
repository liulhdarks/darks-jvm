// java.cpp : 定义控制台应用程序的入口点。
//

#include "stdafx.h"
#include "env/env.h"
#include "common.h"
#include<iostream>
#include "DebugUtils.h"
#include "vm/executor/executor.h"

using namespace std;

#pragma comment(lib, "../Debug/jvm.lib")

std::wstring UT2WC(const char* buf) 
{ 
	int len = MultiByteToWideChar(CP_ACP, 0, buf, -1, NULL, 0); 
	std::vector<wchar_t> unicode(len); 
	MultiByteToWideChar(CP_ACP, 0, buf, -1, &unicode[0], len); 
	cout<<unicode.size()<<" "<<len<<endl;
	return std::wstring(&unicode[0]); 
} 

int _tmain(int argc, _TCHAR* argv[])
{
	Environment::AddClassPath(string("F:/code/java/workdemo/rt/bin/"));
	Environment::AddClassPath(string("F:/code/java/workdemo/JvmTest/bin/"));
	VMContext::initialize();
	lpVMExecutor exec = new VMExecutor();
	exec->Initialize();
	exec->Execute("com.jvm.test.JvmTest1");
	if (exec)
	{
		delete exec;
		exec = NULL;
	}
	VMContext::destroy();
	return 0;
}

