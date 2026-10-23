LOCAL_PATH := $(call my-dir)

# Privileged permissions for microG Companion / FakeStore
# (com.android.vending). Modeled on
# lineageos4microg/android_vendor_partner_gms, minus
# FAKE_PACKAGE_SIGNATURE (not needed on stock LineageOS 21, see
# GmsCore/Android.mk).
include $(CLEAR_VARS)
LOCAL_MODULE := privapp-permissions-com.android.vending.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_PRODUCT_MODULE := true
LOCAL_MODULE_PATH := $(TARGET_OUT_PRODUCT_ETC)/permissions
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := default-permissions-com.android.vending.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_PRODUCT_MODULE := true
LOCAL_MODULE_PATH := $(TARGET_OUT_PRODUCT_ETC)/default-permissions
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := FakeStore
LOCAL_SRC_FILES := FakeStore.apk
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_TAGS := optional
LOCAL_PRIVILEGED_MODULE := true
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_REQUIRED_MODULES := privapp-permissions-com.android.vending.xml default-permissions-com.android.vending.xml
LOCAL_PRODUCT_MODULE := true
LOCAL_OPTIONAL_USES_LIBRARIES := androidx.window.extensions androidx.window.sidecar
LOCAL_DEX_PREOPT := false
include $(BUILD_PREBUILT)
