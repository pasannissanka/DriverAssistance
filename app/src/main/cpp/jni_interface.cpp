#include <jni.h>
#include <string>
#include <ncnn/gpu.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
//#include "yolov4.h"
#include "deepsort.h"

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
    if (deepsort::deepsortDetector == nullptr) {
        deepsort::deepsortDetector = new deepsort();
        deepsort::deepsortDetector->init();
    }
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_YOLOv4_detect(JNIEnv *env, jclass, jobject image, jdouble threshold,
                               jdouble nms_threshold) {
    auto result = yolov4::detector->detect(env, image, threshold, nms_threshold);

    deepsort::deepsortDetector->load_detections(result);
    deepsort::deepsortDetector->update();

    std::vector<BoxInfo> result_deepsort;
    std::vector<int> track_id;
    deepsort::deepsortDetector->get_results(result_deepsort, track_id);

    jclass vector_cls = env->FindClass("java/util/Vector");
    jmethodID vector_add_mid = env->GetMethodID(vector_cls, "add", "(Ljava/lang/Object;)Z");

    jclass int_cls = env->FindClass("java/lang/Integer");

    jclass box_cls = env->FindClass("com/example/Box");
    jmethodID cid = env->GetMethodID(box_cls, "<init>", "(FFFFIFLjava/util/Vector;)V");
    jobjectArray ret = env->NewObjectArray(result.size(), box_cls, nullptr);
    int i = 0;

    for (auto &box:result_deepsort) {
        env->PushLocalFrame(1);

        jmethodID vector_cid = env->GetMethodID(vector_cls, "<init>", "()V");
        jobject vector_val = env->NewObject(vector_cls, vector_cid);

        for (int f: track_id) {
            jmethodID int_cid = env->GetMethodID(int_cls, "<init>", "(I)V");
            jobject int_val = env->NewObject(int_cls, int_cid, f);

            env->CallBooleanMethod(vector_val, vector_add_mid, int_val);
        }
        jobject obj = env->NewObject(box_cls, cid, box.x1, box.y1, box.x2, box.y2, box.label,
                                     box.score, vector_val);
        obj = env->PopLocalFrame(obj);
        env->SetObjectArrayElement(ret, i++, obj);
    }
//    env->DeleteLocalRef(vector_cls);
//    env->DeleteLocalRef(float_cls);
//    env->DeleteLocalRef(box_cls);

    return ret;
}