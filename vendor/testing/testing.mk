# CandyOS: the bundled Testing app (see docs/phases/TASKS.md, Phase 23).
# Wired into the build by the patches/vendor-lineage series
# (-include vendor/testing/testing.mk in vendor/lineage/config/common.mk).
PRODUCT_PACKAGES += Testing
PRODUCT_PACKAGES += default-permissions-com.candyos.testing
