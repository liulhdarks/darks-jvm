// Minimal JNI library used by the TodoList example.  Declared without
// <jni.h> so the target has no JDK include dependency.
extern "C" int Java_TodoList_add(void *, void *, int a, int b)
{
    return a + b;
}
