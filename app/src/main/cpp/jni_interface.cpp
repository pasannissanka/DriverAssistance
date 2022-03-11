#include <jni.h>
#include <string>
#include <ncnn/gpu.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include "yolov4.h"
#include "SORT.h"

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    ncnn::create_gpu_instance();
    if (ncnn::get_gpu_count() > 0) {
        yolov4::hasGPU = true;
    }
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    ncnn::destroy_gpu_instance();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_YOLOv4_init(JNIEnv *env, jclass, jobject assetManager) {
    if (yolov4::detector == nullptr) {
        AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
        yolov4::detector = new yolov4(mgr, "custom-yolov4-tiny-detector_opt.param", "custom-yolov4-tiny-detector_opt.bin", false);
    }
    if (SORT::detector == nullptr) {
        SORT::detector = new SORT(0.3);
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_YOLOv4_detect(JNIEnv *env, jclass, jobject image, jdouble threshold,
                               jdouble nms_threshold) {
    auto result = yolov4::detector->detect(env, image, threshold, nms_threshold);

    auto trackers = SORT::predict(result);

    auto box_cls = env->FindClass("com/example/Box");
    auto cid = env->GetMethodID(box_cls, "<init>", "(FFFFIFII)V");
    jobjectArray ret = env->NewObjectArray(trackers.size(), box_cls, nullptr);
    int i = 0;
    for (auto &box:trackers) {
        env->PushLocalFrame(1);
        jobject obj = env->NewObject(box_cls, cid, box.x1, box.y1, box.x2, box.y2, box.label,
                                     box.score, 0, box.id);
        obj = env->PopLocalFrame(obj);
        env->SetObjectArrayElement(ret, i++, obj);
    }
    return ret;
}