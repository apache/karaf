#include "org_apache_felix_simple_Activator.h"

JNIEXPORT jstring JNICALL
    Java_org_apache_felix_simple_Activator_foo
        (JNIEnv *env, jobject obj)
{
    char *cstr = "Hello!";
    jstring jstr = (*env)->NewStringUTF(env, cstr);
    return jstr;
}
