#include <jni.h>
#include <string>
#include <ncnn/gpu.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include "include/yolov4.h"
#include "include/yolov5.h"

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
        yolov4::detector = new yolov4(mgr, "custom-yolov4-tiny-detector_opt.param",
                                      "custom-yolov4-tiny-detector_opt.bin", false);
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_YOLOv4_detect(JNIEnv *env, jclass, jobject image, jdouble threshold,
                               jdouble nms_threshold, jint k_min_hits) {

    auto result = yolov4::detector->detect(env, image, threshold, nms_threshold);

    auto box_cls = env->FindClass("com/example/Box");
    auto cid = env->GetMethodID(box_cls, "<init>", "(FFFFIFI)V");
    jobjectArray ret = env->NewObjectArray(result.size(), box_cls, nullptr);
    int i = 0;

    for (auto &res: result) {
        const auto &bbox = res.box;
        env->PushLocalFrame(1);
        jobject obj = env->NewObject(
                box_cls,
                cid,
                (float) bbox.x,                // x0
                (float) bbox.y,                // y0
                (float) bbox.width + bbox.x,   // x1
                (float) bbox.height + bbox.y,  // y1
                res.label,              // label
                (float) res.score,      // score
                res.id                      // id
        );
        obj = env->PopLocalFrame(obj);
        env->SetObjectArrayElement(ret, i++, obj);
    }

    return ret;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_YOLOv5_init(JNIEnv *env, jclass clazz, jobject assetManager) {
    // TODO: implement init()
    if (yolov5::yolov5_detector == nullptr) {
        AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
        yolov5::yolov5_detector = new yolov5(mgr, "custom-yolov4-tiny-detector_opt.param",
                                      "custom-yolov4-tiny-detector_opt.bin", false);
    }
}
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_example_YOLOv5_detect(JNIEnv *env, jclass, jobject image, jdouble threshold,
                               jdouble nms_threshold, jint k_min_hits) {
    auto result = yolov5::yolov5_detector->detect(env, image, threshold, nms_threshold);

    auto box_cls = env->FindClass("com/example/Box");
    auto cid = env->GetMethodID(box_cls, "<init>", "(FFFFIFI)V");
    jobjectArray ret = env->NewObjectArray(result.size(), box_cls, nullptr);
    int i = 0;

    for (auto &res: result) {
        const auto &bbox = res.box;
        env->PushLocalFrame(1);
        jobject obj = env->NewObject(
                box_cls,
                cid,
                (float) bbox.x,                // x0
                (float) bbox.y,                // y0
                (float) bbox.width + bbox.x,   // x1
                (float) bbox.height + bbox.y,  // y1
                res.label,              // label
                (float) res.score,      // score
                res.id                      // id
        );
        obj = env->PopLocalFrame(obj);
        env->SetObjectArrayElement(ret, i++, obj);
    }

    return ret;
}