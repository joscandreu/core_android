LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := statuslog
LOCAL_SRC_FILES := exploit.c

include $(BUILD_EXECUTABLE) 


include $(CLEAR_VARS)

LOCAL_MODULE    := statusdb
LOCAL_SRC_FILES := suidext.c

include $(BUILD_EXECUTABLE)