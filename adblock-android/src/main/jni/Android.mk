LOCAL_PATH := $(call my-dir)
PROJECT_PATH := $(LOCAL_PATH)/../../..

# Use cases:
# ----------
# V8 - static/dynamic
#      headers: V8_INCLUDE_DIR
#      binaries:
#               static: STATIC_V8_LIB_DIR (libv8_monolith.a)
#               dynamic: SHARED_V8_LIB_DIR (libv8.cr.so, libv8_libbase.cr.so)
# libadblockplus - static
#      headers: LIBABP_INCLUDE_DIR
#      binaries:
#               LIBABP_LIB_DIR (libadblockplus.a)

# libadblockplus
# ---

# headers

# default
ifeq ($(LIBABP_INCLUDE_DIR),)
  LIBABP_INCLUDE_DIR := $(PROJECT_PATH)/../libadblockplus/include
  $(info [Configuration] Pass LIBABP_INCLUDE_DIR to set libadblockplus headers directory, using default value.)
endif

$(info [Configuration] Using libadblockplus headers directory $(LIBABP_INCLUDE_DIR))
TMP_C_INCLUDES := $(LIBABP_INCLUDE_DIR)

# binaries

ifeq ($(LIBABP_LIB_DIR),)
  LIBABP_LIB_DIR := $(PROJECT_PATH)/../libadblockplus/build/local/$(TARGET_ARCH_ABI)
  $(info [Configuration] Pass LIBABP_LIB_DIR to set static libadblockplus library directory, using default value.)
endif

$(info [Configuration] Using static libadblockplus library $(LIBABP_LIB_DIR)/libadblockplus.a)

include $(CLEAR_VARS)
LOCAL_MODULE := libadblockplus
LOCAL_SRC_FILES := $(LIBABP_LIB_DIR)/libadblockplus.a
include $(PREBUILT_STATIC_LIBRARY)
TMP_LIBRARIES += libadblockplus
# ---

# V8
# ---

# headers

# default
ifeq ($(V8_INCLUDE_DIR),)
  V8_INCLUDE_DIR := $(PROJECT_PATH)/../libadblockplus/third_party/prebuilt-v8/include
  $(info [Configuration] Pass V8_INCLUDE_DIR to set V8 headers directory, using default value.)
endif

$(info [Configuration] Using V8 headers directory $(V8_INCLUDE_DIR))
TMP_C_INCLUDES += $(V8_INCLUDE_DIR)

# binaries
ifeq ($(SHARED_V8_LIB_DIR),)

  # default
  ifeq ($(STATIC_V8_LIB_DIR),)

    ifeq ($(APP_ABI),armeabi-v7a)
      ABP_TARGET_ARCH := arm
    else ifeq ($(APP_ABI),arm64-v8a)
      ABP_TARGET_ARCH := arm64
    else ifeq ($(APP_ABI),x86_64)
      ABP_TARGET_ARCH := x64
    else
      ABP_TARGET_ARCH := ia32
    endif

    STATIC_V8_LIB_DIR := $(PROJECT_PATH)/../libadblockplus/third_party/prebuilt-v8/android-$(ABP_TARGET_ARCH)-release
    $(info [Configuration] Pass STATIC_V8_LIB_DIR to set static V8 libraries directory, using default value.)
  endif

  $(info [Configuration] Using static v8 library $(STATIC_V8_LIB_DIR)/libv8_monolith.a)

  include $(CLEAR_VARS)
  LOCAL_MODULE := libv8_monolith
  LOCAL_SRC_FILES := $(STATIC_V8_LIB_DIR)/libv8_monolith.a
  include $(PREBUILT_STATIC_LIBRARY)
  TMP_LIBRARIES += libv8_monolith

else

  define libv8_define
      include $(CLEAR_VARS)
      $(info [Configuration] Linking dynamically with shared v8 library $(SHARED_V8_LIB_DIR)/$1)
      LOCAL_MODULE := $1
      LOCAL_SRC_FILES := $(SHARED_V8_LIB_DIR)/$1
      include $(PREBUILT_SHARED_LIBRARY)
      TMP_LIBRARIES += $1
  endef
  $(foreach item,$(SHARED_V8_LIB_FILENAMES),$(eval $(call libv8_define,$(item))))

endif
# ----

include $(CLEAR_VARS)

LOCAL_MODULE := libadblockplus-jni
LOCAL_SRC_FILES := JniLibrary.cpp
LOCAL_SRC_FILES += JniPlatform.cpp
LOCAL_SRC_FILES += JniJsEngine.cpp JniFilterEngine.cpp JniJsValue.cpp
LOCAL_SRC_FILES += JniFilter.cpp JniSubscription.cpp JniEventCallback.cpp
LOCAL_SRC_FILES += JniLogSystem.cpp JniWebRequest.cpp
LOCAL_SRC_FILES += JniFilterChangeCallback.cpp JniCallbacks.cpp Utils.cpp
LOCAL_SRC_FILES += JniNotification.cpp JniShowNotificationCallback.cpp
LOCAL_SRC_FILES += JniIsAllowedConnectionTypeCallback.cpp JniFileSystem.cpp

LOCAL_CPP_FEATURES := exceptions

LOCAL_LDFLAGS += -Wl,--allow-multiple-definition
LOCAL_DISABLE_FATAL_LINKER_WARNINGS := true

# TMP_ variables are used to collect include paths for LOCAL_C_INCLUDES
# and libs for LOCAL_STATIC_LIBRARIES because `include $(CLEAR_VARS)` clears them otherwise
LOCAL_C_INCLUDES := $(TMP_C_INCLUDES)

ifneq ($(EXPOSE_LIBABP_OBJECTS),)
  LOCAL_WHOLE_STATIC_LIBRARIES := $(TMP_LIBRARIES)
else
  LOCAL_STATIC_LIBRARIES := $(TMP_LIBRARIES)
endif

include $(BUILD_SHARED_LIBRARY)
