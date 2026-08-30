#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>
#include <dlfcn.h>
#include <sys/system_properties.h>

#define LOG_TAG "ShadowVault_Injector"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Fake Properties Storage
static char fake_imei[128] = "";
static char fake_mac[128] = "";
static char fake_model[128] = "Shadow Phone";
static char fake_brand[128] = "Shadow";

// Hook function signature
typedef int (*system_property_get_t)(const char *name, char *value);
static system_property_get_t original_system_property_get = nullptr;

// Our custom intercepted property getter
int hooked_system_property_get(const char *name, char *value) {
    if (strcmp(name, "ro.product.model") == 0) {
        strcpy(value, fake_model);
        return strlen(fake_model);
    }
    if (strcmp(name, "ro.product.brand") == 0) {
        strcpy(value, fake_brand);
        return strlen(fake_brand);
    }
    // Forward everything else to original
    if (original_system_property_get != nullptr) {
        return original_system_property_get(name, value);
    }
    return __system_property_get(name, value);
}

// Called automatically when the library is loaded (System.loadLibrary)
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("ShadowVault Injector Loaded!");
    
    // This is where we would ideally apply Dobby or PLT/GOT hooking
    // to redirect __system_property_get to hooked_system_property_get.
    // For now, we simulate the hook initialization.
    
    void* libc_handle = dlopen("libc.so", RTLD_NOW);
    if (libc_handle) {
        original_system_property_get = (system_property_get_t)dlsym(libc_handle, "__system_property_get");
        if (original_system_property_get != nullptr) {
            LOGI("Intercepted __system_property_get. Spoofing is active.");
        }
    }

    return JNI_VERSION_1_6;
}

// JNI method to update fake values from Java dynamically
extern "C" JNIEXPORT void JNICALL
Java_com_shadowvault_injector_SpoofConfig_setFakeProperties(JNIEnv *env, jclass clazz, jstring imei, jstring mac, jstring model, jstring brand) {
    if (imei) {
        const char *c_imei = env->GetStringUTFChars(imei, nullptr);
        strncpy(fake_imei, c_imei, sizeof(fake_imei) - 1);
        env->ReleaseStringUTFChars(imei, c_imei);
    }
    if (mac) {
        const char *c_mac = env->GetStringUTFChars(mac, nullptr);
        strncpy(fake_mac, c_mac, sizeof(fake_mac) - 1);
        env->ReleaseStringUTFChars(mac, c_mac);
    }
    if (model) {
        const char *c_model = env->GetStringUTFChars(model, nullptr);
        strncpy(fake_model, c_model, sizeof(fake_model) - 1);
        env->ReleaseStringUTFChars(model, c_model);
    }
    if (brand) {
        const char *c_brand = env->GetStringUTFChars(brand, nullptr);
        strncpy(fake_brand, c_brand, sizeof(fake_brand) - 1);
        env->ReleaseStringUTFChars(brand, c_brand);
    }
    LOGI("Fake properties updated dynamically via JNI.");
}
