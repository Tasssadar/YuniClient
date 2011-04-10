LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE    := jni_functions
LOCAL_SRC_FILES := jni_functions.c
include $(BUILD_SHARED_LIBRARY)