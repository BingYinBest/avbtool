#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
# Compare device-generated avbtool outputs against the host reference
# artifacts in host/ (produced by AOSP external/avb main avbtool.py
# 1.3.0 with openssl on the host; byte-verified identical to the in-app
# patched avbtool.py 1.3.0 + cryptography 42.0.8 run on the host).
#
# NOTE: add_hashtree_footer APPENDS the hash tree to the input image,
# so e2e_tree.img is mutated by each run. Regenerate the images from
# scratch on the device before every comparison run (e.g. via a text
# editor or the console tip), otherwise hashes will legitimately differ.
#
# Device procedure (in AVBToolkit console), with testkey.pem and the
# images copied to the device (SAF):
#   avbtool add_hash_footer --image e2e_hash.img --partition_name boot \
#     --partition_size 81920 --salt 00 --hash_algorithm sha256 \
#     --algorithm NONE --output_vbmeta_image boot.hashfooter.vbmeta \
#     --do_not_append_vbmeta_image
#   avbtool add_hash_footer --image e2e_hash.img --partition_name boot \
#     --partition_size 81920 --salt 00 --hash_algorithm sha256 \
#     --algorithm SHA256_RSA4096 --key testkey.pem \
#     --output_vbmeta_image boot.hashfooter.signed.vbmeta \
#     --do_not_append_vbmeta_image
#   avbtool add_hashtree_footer --image e2e_tree.img --partition_name system \
#     --partition_size 196608 --salt 00 --block_size 4096 \
#     --hash_algorithm sha256 --algorithm NONE --do_not_generate_fec \
#     --output_vbmeta_image system.tree.vbmeta --do_not_append_vbmeta_image
#   avbtool make_vbmeta_image --key testkey.pem --algorithm SHA256_RSA4096 \
#     --rollback_index 5 --output vbmeta_signed.img
#   avbtool extract_public_key --key testkey_pub.pem --output pkmd.bin
#
# then export the five files and run:
#   sh tools/avb-verify/compare_avb_outputs.sh <dir-with-device-files>

set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
DEV="$1"
[ -n "$DEV" ] || { echo "usage: $0 <dir-with-device-files>"; exit 2; }

fail=0
for f in boot.hashfooter.vbmeta boot.hashfooter.signed.vbmeta \
         system.tree.vbmeta vbmeta_signed.img pkmd.bin; do
  ref="$DIR/host/$f"
  got="$DEV/$f"
  if [ ! -f "$got" ]; then
    echo "MISSING  $f (device file not found in $DEV)"; fail=1; continue
  fi
  ref_sha=$(sha256sum "$ref" | cut -d' ' -f1)
  got_sha=$(sha256sum "$got" | cut -d' ' -f1)
  if [ "$ref_sha" = "$got_sha" ]; then
    echo "OK       $f  $got_sha"
  else
    echo "MISMATCH $f"; echo "  ref: $ref_sha"; echo "  got: $got_sha"; fail=1
  fi
done
echo
if [ "$fail" = 0 ]; then
  echo "All device outputs byte-match the AOSP host reference."
else
  echo "Differences found (see above)."
fi
exit "$fail"
