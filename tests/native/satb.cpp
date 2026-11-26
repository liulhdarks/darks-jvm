/*
 * JNI helper for the SATB write-barrier stress test.
 *
 * Compiled against the official JDK <jni.h> so the calls go through the
 * JNIEnv function table exactly like a third-party library would, and every
 * write lands in the VM's JNI field/array setters.
 */
#include <jni.h>

extern "C" {

JNIEXPORT void JNICALL Java_SatbStress_jniSetField(JNIEnv *env, jclass unused,
                                                   jobject target, jobject value)
{
    (void)unused;
    if (!env || !target)
    {
        return;
    }
    jclass cls = env->GetObjectClass(target);
    if (!cls)
    {
        return;
    }
    jfieldID fid = env->GetFieldID(cls, "next", "LSatbStress$Node;");
    if (fid)
    {
        env->SetObjectField(target, fid, value);
    }
}

JNIEXPORT void JNICALL Java_SatbStress_jniSetStatic(JNIEnv *env, jclass unused,
                                                    jobject value)
{
    (void)unused;
    if (!env || !value)
    {
        return;
    }
    jclass cls = env->GetObjectClass(value);
    if (!cls)
    {
        return;
    }
    jfieldID fid = env->GetStaticFieldID(cls, "shared", "LSatbStress$Node;");
    if (fid)
    {
        env->SetStaticObjectField(cls, fid, value);
    }
}

JNIEXPORT void JNICALL Java_SatbStress_jniSetArray(JNIEnv *env, jclass unused,
                                                   jobjectArray array, jint index,
                                                   jobject value)
{
    (void)unused;
    if (!env || !array)
    {
        return;
    }
    env->SetObjectArrayElement(array, index, value);
}

} // extern "C"
