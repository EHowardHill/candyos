#!/usr/bin/env python3
"""Regenerate the build makefiles for a bundled MindTheGapps tree.

Usage: candyos-gen-gapps-mk.py <gapps-arch-dir> [--aapt2 <aapt2 binary>]

<gapps-arch-dir> is vendor/partner_gms/gapps-<arch>/ as populated by
scripts/candyos-fetch-apps.sh (the system/ tree extracted from the pinned
MindTheGapps zip). This script scans it and writes:

  <dir>/Android.mk          one BUILD_PREBUILT module per APK / etc XML,
                             guarded to the directory's target arch
  <dir>/gapps-packages.mk   PRODUCT_PACKAGES + artifact-path allowed list,
                             included by products/gms.mk for that arch

The APKs and XMLs are binary blobs and are never committed; only the two
generated makefiles are. Re-run after every fetch. APK <uses-library> tags
are mirrored from the manifests when an aapt2 is available (--aapt2 or the
AAPT2 environment variable); manifest_check requires the module to declare
exactly the same set, so the build-tree aapt2 (out/host/linux-x86/bin/aapt2)
is preferred once a tree exists.
"""

import os
import re
import subprocess
import sys

APK_TOPS = ("app", "priv-app", "overlay")


def sanitize(name):
    return re.sub(r"[^A-Za-z0-9_]", "_", name)


def find_aapt2(argv):
    if "--aapt2" in argv:
        return argv[argv.index("--aapt2") + 1]
    return os.environ.get("AAPT2") or None


def dump_uses_libraries(aapt2, apk):
    req, opt = [], []
    try:
        out = subprocess.run(
            [aapt2, "dump", "badging", apk],
            capture_output=True, text=True, timeout=120,
        ).stdout
    except Exception:
        return req, opt
    for line in out.splitlines():
        m = re.match(r"\s*uses-library(-not-required)?\s*:\s*'([^']+)'", line)
        if not m:
            continue
        (opt if m.group(1) else req).append(m.group(2))
    return req, opt


def main():
    root = sys.argv[1]
    if not root.startswith("/") and not os.path.isdir(root):
        sys.exit("usage: candyos-gen-gapps-mk.py <gapps-arch-dir> [--aapt2 <aapt2>]")
    root = os.path.abspath(root)
    aapt2 = find_aapt2(sys.argv[1:])
    arch = re.sub(r"^gapps-", "", os.path.basename(root))
    system = os.path.join(root, "system")
    if not os.path.isdir(system):
        sys.exit("candyos: ERROR: %s has no system/ tree (run candyos-fetch-apps.sh first)" % root)
    if arch not in ("arm", "arm64"):
        sys.exit("candyos: ERROR: %s does not name a GApps arch (gapps-arm/gapps-arm64)" % root)

    apks, etcs, skipped = [], [], []
    for top in APK_TOPS:
        topdir = os.path.join(system, top)
        if not os.path.isdir(topdir):
            continue
        for dirpath, _dirnames, filenames in os.walk(topdir):
            for fn in filenames:
                path = os.path.join(dirpath, fn)
                rel = os.path.relpath(path, root)
                parent = os.path.basename(dirpath)
                stem = os.path.splitext(fn)[0]
                name = sanitize(parent if parent != top else stem)
                if top == "overlay":
                    name = "%s__rro" % name
                req, opt = ([], [])
                if aapt2:
                    req, opt = dump_uses_libraries(aapt2, path)
                apks.append({
                    "name": name, "rel": rel, "src": os.path.basename(fn),
                    "top": top, "uses": req, "opt": opt,
                    "priv": top == "priv-app",
                })
    etcdir = os.path.join(system, "etc")
    if os.path.isdir(etcdir):
        for dirpath, _dirnames, filenames in os.walk(etcdir):
            for fn in filenames:
                path = os.path.join(dirpath, fn)
                # Module name = file name verbatim (dots are legal, and the
                # installed path is derived from it) - as in the old microG
                # XML modules; sanitize only if something odd shows up.
                etcs.append({
                    "name": fn if re.fullmatch(r"[A-Za-z0-9._-]+", fn) else sanitize(fn) + "_xml",
                    "src": fn,
                    "rel": os.path.relpath(path, root),
                    "sub": os.path.relpath(dirpath, etcdir) if dirpath != etcdir else "",
                })
    for dirpath, _dirnames, filenames in os.walk(system):
        rel = os.path.relpath(dirpath, system)
        top = rel.split(os.sep)[0]
        if top in APK_TOPS or top == "etc":
            for fn in filenames:
                if not fn.endswith(".apk") and not fn.endswith(".xml") \
                        and not fn.endswith(".json"):
                    skipped.append(os.path.join(rel, fn))
        else:
            for fn in filenames:
                skipped.append(os.path.join(rel, fn))
    if not apks:
        sys.exit("candyos: ERROR: no APKs under %s/system" % root)

    seen = set()
    for m in apks + etcs:
        if m["name"] in seen:
            sys.exit("candyos: ERROR: duplicate module name %s" % m["name"])
        seen.add(m["name"])
    if not aapt2:
        print("candyos: WARNING: no aapt2 - APK uses-library tags not "
              "mirrored; rerun with --aapt2 (or AAPT2=) once a tree is built",
              file=sys.stderr)

    mk = ["# GENERATED by scripts/candyos-gen-gapps-mk.py - DO NOT EDIT",
          "# Modules for the bundled MindTheGapps tree (arch: %s)." % arch,
          "ifeq ($(TARGET_ARCH),%s)" % arch, ""]
    allowed = []
    for m in apks:
        mk.append("include $(CLEAR_VARS)")
        mk.append("LOCAL_MODULE := %s" % m["name"])
        mk.append("LOCAL_SRC_FILES := %s" % m["rel"])
        mk.append("LOCAL_MODULE_CLASS := APPS")
        mk.append("LOCAL_PRODUCT_MODULE := true")
        if m["priv"]:
            mk.append("LOCAL_PRIVILEGED_MODULE := true")
        mk.append("LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)")
        mk.append("LOCAL_CERTIFICATE := PRESIGNED")
        if m["top"] == "overlay":
            mk.append("LOCAL_MODULE_PATH := $(TARGET_OUT_PRODUCT)/overlay")
            allowed.append("product/overlay/%s.apk" % m["name"])
        else:
            sub = "priv-app" if m["priv"] else "app"
            allowed.append("product/%s/%s/%s.apk" % (sub, m["name"], m["name"]))
        if m["uses"]:
            mk.append("LOCAL_USES_LIBRARIES := %s" % " ".join(m["uses"]))
        if m["opt"]:
            mk.append("LOCAL_OPTIONAL_USES_LIBRARIES := %s" % " ".join(m["opt"]))
        mk.append("LOCAL_DEX_PREOPT := false")
        mk.append("LOCAL_REPLACE_PREBUILT_APK_INSTALLED := $(LOCAL_PATH)/$(LOCAL_SRC_FILES)")
        mk.append("include $(BUILD_PREBUILT)")
        mk.append("")
    for m in etcs:
        mk.append("include $(CLEAR_VARS)")
        mk.append("LOCAL_MODULE := %s" % m["name"])
        mk.append("LOCAL_MODULE_CLASS := ETC")
        mk.append("LOCAL_PRODUCT_MODULE := true")
        mk.append("LOCAL_MODULE_PATH := $(TARGET_OUT_PRODUCT_ETC)%s%s" % ("/" if m["sub"] else "", m["sub"]))
        mk.append("LOCAL_SRC_FILES := %s" % m["rel"])
        mk.append("include $(BUILD_PREBUILT)")
        allowed.append("product/etc/%s" % "/".join(p for p in (m["sub"], m["src"]) if p))
        mk.append("")
    mk.append("endif")
    with open(os.path.join(root, "Android.mk"), "w") as f:
        f.write("\n".join(mk) + "\n")

    pk = ["# GENERATED by scripts/candyos-gen-gapps-mk.py - DO NOT EDIT",
          "# Product packages for the bundled MindTheGapps tree (arch: %s)." % arch,
          "PRODUCT_PACKAGES += \\",
          "    " + " \\\n    ".join(m["name"] for m in apks + etcs),
          "",
          "PRODUCT_ARTIFACT_PATH_REQUIREMENT_ALLOWED_LIST += \\",
          "    " + " \\\n    ".join(allowed),
          ""]
    with open(os.path.join(root, "gapps-packages.mk"), "w") as f:
        f.write("\n".join(pk) + "\n")

    print("candyos: generated %s/Android.mk and %s/gapps-packages.mk "
          "(%d APK modules, %d etc modules)" % (root, root, len(apks), len(etcs)))
    if skipped:
        print("candyos: WARNING: skipped unhandled files:" , file=sys.stderr)
        for s in skipped:
            print("  %s" % s, file=sys.stderr)


if __name__ == "__main__":
    main()