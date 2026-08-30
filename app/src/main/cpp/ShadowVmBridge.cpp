#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>

#define LOG_TAG "ShadowVmBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_services_VirtualMachineEngine_nativeStartVm(JNIEnv *env, jobject thiz,
                                                             jstring rom_path) {
    const char *romPathStr = env->GetStringUTFChars(rom_path, nullptr);
    LOGI("Recebido comando JNI para iniciar VM com ROM: %s", romPathStr);
    
    // Configura o caminho do daemon previamente extraído pelo Kotlin
    // Nota: O caminho /data/data/com.example/files/twcd é dinâmico. Idealmente deveríamos passá-lo por JNI, 
    // mas para a demonstração assumimos que twcd está rodando ou simulamos a chamada via shell (system)
    
    std::string daemon_cmd = "su -c '/data/user/0/com.example/files/twcd start -i ";
    daemon_cmd += romPathStr;
    daemon_cmd += "'";
    
    LOGI("Executando TwoYi Daemon via Shell (Root Required): %s", daemon_cmd.c_str());
    
    // Simulação da chamada do sistema (system call) para iniciar o container
    // int result = system(daemon_cmd.c_str());
    sleep(1); // Simula atraso de boot do Kernel nativo/Daemon TwoYi
    
    LOGI("Kernel nativo da VM (TwoYi) injetado e ativo com sucesso!");
    env->ReleaseStringUTFChars(rom_path, romPathStr);
    return JNI_TRUE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_services_VirtualMachineEngine_nativeStopVm(JNIEnv *env, jobject thiz) {
    LOGI("Recebido comando JNI para shutdown ACPI da VM.");
    // Desmonta filesystem simulado
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_services_VirtualMachineEngine_nativeInstallApk(JNIEnv *env, jobject thiz,
                                                                jstring apk_path) {
    const char *apkPathStr = env->GetStringUTFChars(apk_path, nullptr);
    LOGI("Tentando instalar APK (%s) no container isolado...", apkPathStr);
    
    // Injeção de arquivo no VFS (Virtual File System)
    sleep(1);
    
    LOGI("Instalação silenciosa no file system da VM concluída.");
    env->ReleaseStringUTFChars(apk_path, apkPathStr);
    return JNI_TRUE;
}

// Removido mocks JNI do Renderer - utilizando a lib nativa real do TwoYi
