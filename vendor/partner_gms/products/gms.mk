# CandyOS bundled "GMS": microG (Services + Companion/FakeStore),
# Aurora Store, and Organic Maps (the open-source Maps app, docs/apps.md),
# shipped as prebuilt, PRESIGNED product-partition packages.
#
# This file is picked up by the stock LineageOS hook
# vendor/lineage/config/partner_gms.mk when WITH_GMS is true (the flag is
# set by patches/vendor-lineage/). The APKs themselves are fetched by
# scripts/candyos-fetch-apps.sh and are not committed to this repo.

PRODUCT_PACKAGES += \
    GmsCore \
    FakeStore \
    AuroraStore \
    OrganicMaps

# Keep the artifact-path checker happy on targets that enforce it.
PRODUCT_ARTIFACT_PATH_REQUIREMENT_ALLOWED_LIST += \
    product/app/AuroraStore/AuroraStore.apk \
    product/app/OrganicMaps/OrganicMaps.apk \
    product/priv-app/FakeStore/FakeStore.apk \
    product/priv-app/GmsCore/GmsCore.apk
