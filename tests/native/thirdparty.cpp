/*
 * Third-party JNI library used by the darks-jvm test suite.
 *
 * Deliberately compiled against the *official* JDK <jni.h> (not the VM's
 * internal headers) so the test proves ABI compatibility of the JNIEnv
 * function table: only the first word of JNIEnv is touched, and every call
 * goes through env->... without linking any darks-jvm symbol.
 *
 * Covered here:
 *   - JNI_OnLoad / JNI_OnUnload
 *   - RegisterNatives
 *   - short JNI names and "long" names for overloaded methods
 *   - native -> Java callbacks (static method invocation)
 *   - exception propagation in both directions
 *   - strings, primitive arrays and object arrays
 */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

extern "C" {

static int g_on_load_called = 0;

static void write_marker(const char *text)
{
    const char *path = getenv("DARKS_JNI_UNLOAD_MARKER");
    if (!path || !*path)
    {
        return;
    }
    FILE *fp = fopen(path, "wb");
    if (fp)
    {
        fputs(text, fp);
        fclose(fp);
    }
}

/* Registered implementation for registeredAdd(II)I (no exported symbol). */
static jint JNICALL registered_add(JNIEnv *env, jclass clazz, jint a, jint b)
{
    (void)env;
    (void)clazz;
    return a + b;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv *env = NULL;
    (void)reserved;
    g_on_load_called = 1;
    if (!vm || vm->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK || !env)
    {
        return JNI_ERR;
    }
    jclass cls = env->FindClass("dev/darks/jni/NativeLib");
    if (!cls)
    {
        return JNI_ERR;
    }
    /* Prove RegisterNatives works: registeredAdd has no Java_* symbol. */
    JNINativeMethod methods[1];
    methods[0].name = (char *)"registeredAdd";
    methods[0].signature = (char *)"(II)I";
    methods[0].fnPtr = (void *)registered_add;
    if (env->RegisterNatives(cls, methods, 1) != JNI_OK)
    {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}

JNIEXPORT jint JNICALL Java_dev_darks_jni_NativeLib_isOnLoadCalled(JNIEnv *env, jclass cls)
{
    (void)env;
    (void)cls;
    return g_on_load_called;
}

/* Non-overloaded native: resolved through the short JNI name. */
JNIEXPORT jint JNICALL Java_dev_darks_jni_NativeLib_echoInt(JNIEnv *env, jclass cls, jint v)
{
    (void)env;
    (void)cls;
    return v * 3 + 1;
}

/* Overloaded natives: resolved through long JNI names. */
JNIEXPORT jint JNICALL Java_dev_darks_jni_NativeLib_over__I(JNIEnv *env, jclass cls, jint v)
{
    (void)env;
    (void)cls;
    return v + 100;
}

JNIEXPORT jstring JNICALL Java_dev_darks_jni_NativeLib_over__Ljava_lang_String_2(
    JNIEnv *env, jclass cls, jstring s)
{
    (void)cls;
    const char *utf = s ? env->GetStringUTFChars(s, NULL) : NULL;
    char buffer[256];
    snprintf(buffer, sizeof(buffer), "over[%s]", utf ? utf : "null");
    if (utf)
    {
        env->ReleaseStringUTFChars(s, utf);
    }
    return env->NewStringUTF(buffer);
}

/* Native -> Java callback: invoke a static method on the calling class. */
JNIEXPORT jstring JNICALL Java_dev_darks_jni_NativeLib_callStatic(JNIEnv *env, jclass cls, jint v)
{
    jmethodID mid = env->GetStaticMethodID(cls, "callbackString", "(I)Ljava/lang/String;");
    if (!mid)
    {
        return NULL;
    }
    jstring result = (jstring)env->CallStaticObjectMethod(cls, mid, v);
    return result;
}

/* Arrays: read a region from an int[] and build a new one. */
JNIEXPORT jint JNICALL Java_dev_darks_jni_NativeLib_sumArray(JNIEnv *env, jclass cls, jintArray arr)
{
    (void)cls;
    if (!arr)
    {
        return -1;
    }
    jsize len = env->GetArrayLength(arr);
    jint *data = (jint *)malloc(sizeof(jint) * (size_t)(len > 0 ? len : 1));
    if (!data)
    {
        return -2;
    }
    env->GetIntArrayRegion(arr, 0, len, data);
    jint sum = 0;
    for (jsize i = 0; i < len; i++)
    {
        sum += data[i];
    }
    free(data);
    return sum;
}

JNIEXPORT jintArray JNICALL Java_dev_darks_jni_NativeLib_makeArray(JNIEnv *env, jclass cls, jint n)
{
    (void)cls;
    jintArray arr = env->NewIntArray(n);
    if (!arr)
    {
        return NULL;
    }
    jint *data = (jint *)malloc(sizeof(jint) * (size_t)(n > 0 ? n : 1));
    if (!data)
    {
        return NULL;
    }
    for (jint i = 0; i < n; i++)
    {
        data[i] = i * i;
    }
    env->SetIntArrayRegion(arr, 0, n, data);
    free(data);
    return arr;
}

JNIEXPORT jobjectArray JNICALL Java_dev_darks_jni_NativeLib_makeStrings(JNIEnv *env, jclass cls, jint n)
{
    (void)cls;
    jclass string_cls = env->FindClass("java/lang/String");
    if (!string_cls)
    {
        return NULL;
    }
    jobjectArray arr = env->NewObjectArray(n, string_cls, NULL);
    if (!arr)
    {
        return NULL;
    }
    char buffer[64];
    for (jint i = 0; i < n; i++)
    {
        snprintf(buffer, sizeof(buffer), "s%d", (int)i);
        jstring s = env->NewStringUTF(buffer);
        env->SetObjectArrayElement(arr, i, s);
        env->DeleteLocalRef(s);
    }
    return arr;
}

/* Strings: read UTF-8 and return a new string. */
JNIEXPORT jstring JNICALL Java_dev_darks_jni_NativeLib_upper(JNIEnv *env, jclass cls, jstring s)
{
    (void)cls;
    if (!s)
    {
        return NULL;
    }
    const char *utf = env->GetStringUTFChars(s, NULL);
    if (!utf)
    {
        return NULL;
    }
    size_t n = strlen(utf);
    char *copy = (char *)malloc(n + 1);
    if (!copy)
    {
        env->ReleaseStringUTFChars(s, utf);
        return NULL;
    }
    for (size_t i = 0; i < n; i++)
    {
        char c = utf[i];
        copy[i] = (c >= 'a' && c <= 'z') ? (char)(c - 'a' + 'A') : c;
    }
    copy[n] = '\0';
    env->ReleaseStringUTFChars(s, utf);
    jstring result = env->NewStringUTF(copy);
    free(copy);
    return result;
}

/* Throw a Java exception from native code; the Java side catches it. */
JNIEXPORT void JNICALL Java_dev_darks_jni_NativeLib_raiseFromNative(JNIEnv *env, jclass cls)
{
    (void)cls;
    jclass ex = env->FindClass("java/lang/IllegalArgumentException");
    if (ex)
    {
        env->ThrowNew(ex, "native boom");
    }
}

/* Call a Java method that throws, observe the pending exception and clear it. */
JNIEXPORT jint JNICALL Java_dev_darks_jni_NativeLib_observeJavaException(JNIEnv *env, jclass cls)
{
    jmethodID mid = env->GetStaticMethodID(cls, "thrower", "()V");
    if (!mid)
    {
        return -1;
    }
    env->CallStaticVoidMethod(cls, mid);
    if (env->ExceptionCheck() != JNI_TRUE)
    {
        return 0;
    }
    jthrowable ex = env->ExceptionOccurred();
    env->ExceptionClear();
    if (!ex)
    {
        return 1;
    }
    env->DeleteLocalRef(ex);
    return 2;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
    (void)vm;
    (void)reserved;
    write_marker("unloaded");
}

} // extern "C"
