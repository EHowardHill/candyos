# CandyOS bundled "GMS": Google Play compatibility via microG Services +
# microG Companion + Aurora Store, plus the open-source Maps app
# (docs/apps.md), shipped as prebuilt, PRESIGNED product-partition blobs.
#
# This file is picked up by the stock LineageOS hook
# vendor/lineage/config/partner_gms.mk when WITH_GMS is true (the flag is
# set by patches/vendor-lineage/). The blobs are fetched pinned by
# scripts/candyos-fetch-apps.sh; none of them are committed to this repo.
#
# Unlike the retired MindTheGapps setup these packages are: (a) universal
# APKs, so every arch - including the x86/x86_64 emulator QA images - gets
# them; (b) enabled per user, not force-on: the OTE wizard and the
# CandyOS Experience menu ship them DISABLED until the user opts in; and
# (c) unprivileged apart from the tightly-scoped microG whitelist
# (vendor/partner_gms/microg/Android.mk).

PRODUCT_PACKAGES += \
    OrganicMaps \
    GmsCore \
    Companion \
    AuroraStore

PRODUCT_ARTIFACT_PATH_REQUIREMENT_ALLOWED_LIST += \
    product/app/AuroraStore/AuroraStore.apk \
    product/etc/permissions/privapp-permissions-com.android.vending.xml \
    product/etc/permissions/privapp-permissions-com.google.android.gms.xml \
    product/etc/sysconfig/sysconfig-com.google.android.gms.xml \
    product/priv-app/Companion/Companion.apk \
    product/priv-app/GmsCore/GmsCore.apk
