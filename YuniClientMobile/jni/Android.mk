LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE    := load_hex
LOCAL_SRC_FILES := load_hex.c
include $(BUILD_SHARED_LIBRARY)