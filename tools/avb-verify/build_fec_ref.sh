#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
# Build the host FEC reference encoder and regenerate expected values.
#
# Usage: sh tools/avb-verify/build_fec_ref.sh
# Requires: git, gcc
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
FEC_SRC="${FEC_SRC:-$DIR/.dep/external-fec}"

if [ ! -d "$FEC_SRC" ]; then
  mkdir -p "$DIR/.dep"
  git clone --depth 1 https://android.googlesource.com/platform/external/fec "$FEC_SRC"
fi

gcc -O2 -Wall -I"$FEC_SRC" "$DIR/fec_ref.c" \
  "$FEC_SRC/init_rs_char.c" "$FEC_SRC/encode_rs_char.c" \
  -o "$DIR/fec_ref"

cd "$DIR"
# Deterministic 1 MiB self-test image (same as avbtool_fec_self_test on device)
python3 -c "open('selftest.img','wb').write((bytes(range(256))*16)*256)"
./fec_ref selftest.img selftest_ref_parity.bin
echo "== expected values (update expected_fec_sha256.txt if these change) =="
echo "fec_parity_bytes (roots=2): $(wc -c < selftest_ref_parity.bin)"
echo "fec_parity_sha256: $(sha256sum selftest_ref_parity.bin | cut -d' ' -f1)"
