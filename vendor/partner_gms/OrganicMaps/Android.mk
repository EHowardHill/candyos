LOCAL_PATH := $(call my-dir)

# Organic Maps (app.organicmaps), Apache-2.0. Prebuilt release APK,
# fetched pinned by scripts/candyos-fetch-apps.sh (never committed).
# Replaces the source-built CandyMaps in PRODUCT_PACKAGES (docs/apps.md).
include $(CLEAR_VARS)
LOCAL_MODULE := OrganicMaps
LOCAL_SRC_FILES := OrganicMaps.apk
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_PRODUCT_MODULE := true
# Install the upstream APK verbatim. The default PRESIGNED prebuilt path
# repacks (uncompresses every embedded .so) to satisfy
# check-jni-dex-compression, but the repack strips the APK Signature
# Scheme v2 block while the v1 JAR header survives - Android 14+ then
# refuses to scan the package. Organic Maps ships extractNativeLibs=true
# (it extracts its .so at install), so the compressed libs are legal and
# no transform is needed. (LOCAL_SDK_VERSION/current does NOT help here:
# that only silences the transform for v2-only APKs; this one carries a
# v1+ hierarchy whose compression must stay untouched.)
LOCAL_REPLACE_PREBUILT_APK_INSTALLED := $(LOCAL_PATH)/$(LOCAL_SRC_FILES)
# Declared optional in the APK manifest; manifest_check requires the module
# to declare the same set (same as Aurora Store).
LOCAL_OPTIONAL_USES_LIBRARIES := androidx.window.extensions androidx.window.sidecar
LOCAL_DEX_PREOPT := false
include $(BUILD_PREBUILT)