LOCAL_PATH := $(call my-dir)

# microG Services (com.google.android.gms) + microG Companion
# (com.android.vending) + Aurora Store (com.aurora.store): the Google
# Play compatibility stack, preloaded on the product partition and
# enabled/disabled per user by the CandyOS Experience app (OTE step +
# menu; see apps/CandyExperience and docs/apps.md). The APKs are fetched
# pinned by scripts/candyos-fetch-apps.sh (never committed).
#
# Signature spoofing: both microG packages pretend to be signed with the
# Google platform certificate. The permission-gated spoof lives in
# patches/frameworks-base/0016 (P34); the FAKE_PACKAGE_SIGNATURE grant
# below is what activates it, and it is scoped to exactly these two
# packages. The rest of the whitelist mirrors the grant set the
# LineageOS-for-microG project ships for GmsCore (network location
# provider registration + GCM push maintenance).

include $(CLEAR_VARS)
LOCAL_MODULE := privapp-permissions-com.google.android.gms.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_PRODUCT_MODULE := true
LOCAL_MODULE_PATH := $(TARGET_OUT_PRODUCT_ETC)/permissions
LOCAL_SRC_FILES := privapp-permissions-com.google.android.gms.xml
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := sysconfig-com.google.android.gms.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_PRODUCT_MODULE := true
LOCAL_MODULE_PATH := $(TARGET_OUT_PRODUCT_ETC)/sysconfig
LOCAL_SRC_FILES := sysconfig-com.google.android.gms.xml
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := privapp-permissions-com.android.vending.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS := optional
LOCAL_PRODUCT_MODULE := true
LOCAL_MODULE_PATH := $(TARGET_OUT_PRODUCT_ETC)/permissions
LOCAL_SRC_FILES := privapp-permissions-com.android.vending.xml
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := GmsCore
LOCAL_SRC_FILES := com.google.android.gms.apk
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_PRODUCT_MODULE := true
LOCAL_PRIVILEGED_MODULE := true
# microG UnifiedNlp takes the OS network-location-provider slot on
# devices that ship a Qualcomm provider (no-op where none exists).
LOCAL_OVERRIDES_PACKAGES := com.qualcomm.location
LOCAL_REQUIRED_MODULES := privapp-permissions-com.google.android.gms.xml sysconfig-com.google.android.gms.xml
# Uses-library tags mirrored from the APK manifest (aapt2 dump badging):
# com.android.location.provider required; the rest optional.
LOCAL_USES_LIBRARIES := com.android.location.provider
LOCAL_OPTIONAL_USES_LIBRARIES := org.apache.http.legacy androidx.window.extensions androidx.window.sidecar
LOCAL_DEX_PREOPT := false
# Install the upstream APK verbatim (v1 signing hierarchy on board - the
# repack path would strip the v2/v3 blocks; same reasoning as OrganicMaps).
LOCAL_REPLACE_PREBUILT_APK_INSTALLED := $(LOCAL_PATH)/$(LOCAL_SRC_FILES)
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := Companion
LOCAL_SRC_FILES := com.android.vending.apk
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_PRODUCT_MODULE := true
LOCAL_PRIVILEGED_MODULE := true
LOCAL_REQUIRED_MODULES := privapp-permissions-com.android.vending.xml
LOCAL_OPTIONAL_USES_LIBRARIES := androidx.window.extensions androidx.window.sidecar
LOCAL_DEX_PREOPT := false
LOCAL_REPLACE_PREBUILT_APK_INSTALLED := $(LOCAL_PATH)/$(LOCAL_SRC_FILES)
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := AuroraStore
LOCAL_SRC_FILES := com.aurora.store.apk
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_PRODUCT_MODULE := true
LOCAL_OPTIONAL_USES_LIBRARIES := androidx.window.extensions androidx.window.sidecar
LOCAL_DEX_PREOPT := false
LOCAL_REPLACE_PREBUILT_APK_INSTALLED := $(LOCAL_PATH)/$(LOCAL_SRC_FILES)
include $(BUILD_PREBUILT)
