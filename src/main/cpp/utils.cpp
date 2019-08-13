#include <jni.h>
#include <cstdlib>
#include <cstring>

extern "C"
JNIEXPORT void JNICALL Java_com_winom_multimedia_utils_JniEntry_byteBufferCopy(JNIEnv *env,
                                                                               jclass __unused obj,
                                                                               jobject srcBuf,
                                                                               jobject dstBuf,
                                                                               jint size) {
    void *pSrcBuf = env->GetDirectBufferAddress(srcBuf);
    void *pDstBuf = env->GetDirectBufferAddress(dstBuf);
    memcpy(pDstBuf, pSrcBuf, (size_t) size);
}

