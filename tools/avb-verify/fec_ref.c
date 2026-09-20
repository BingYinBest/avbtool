/* SPDX-License-Identifier: Apache-2.0
 *
 * Host reference encoder for AVB FEC data.
 *
 * Mirrors the byte layout of the device-native encoder
 * (app/src/main/cpp/avb_fec.cpp) and links against the AOSP external/fec
 * RS(255, N) Reed-Solomon codec (LGPL-2.1, Phil Karn KA9Q) so the host
 * reference is byte-exact with what the APK produces on device.
 *
 * Build (from the repository root):
 *   gcc -O2 -I<external/fec> tools/avb-verify/fec_ref.c \
 *       <external/fec>/init_rs_char.c <external/fec>/encode_rs_char.c \
 *       -o tools/avb-verify/fec_ref
 *
 * Usage:
 *   ./fec_ref image <out.fec> <roots>
 */

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>

extern void *init_rs_char(int symsize, int gfpoly, int fcr, int prim,
                          int nroots, int pad);
extern void encode_rs_char(void *p, unsigned char *data, unsigned char *parity);
extern void free_rs_char(void *p);

#define FEC_MAGIC 0xFECFECFE
#define FEC_BLOCKSIZE 4096
#define FEC_RSM 255

static uint64_t div_round_up(uint64_t x, uint64_t y) {
    return (x / y) + (x % y > 0 ? 1 : 0);
}

int main(int argc, char **argv) {
    if (argc != 4) {
        fprintf(stderr, "usage: %s image out.fec roots\n", argv[0]);
        return 2;
    }
    uint64_t roots = strtoull(argv[3], NULL, 0);
    FILE *in = fopen(argv[1], "rb");
    if (!in) {
        perror("open image");
        return 1;
    }
    fseeko(in, 0, SEEK_END);
    int64_t sz = ftello(in);
    fseeko(in, 0, SEEK_SET);
    if (sz <= 0 || sz % FEC_BLOCKSIZE) {
        fprintf(stderr, "image size %lld not a positive multiple of %d\n",
                (long long)sz, FEC_BLOCKSIZE);
        return 1;
    }
    uint64_t blocks = div_round_up((uint64_t)sz, FEC_BLOCKSIZE);
    uint64_t rsn = FEC_RSM - roots;
    uint64_t rounds = div_round_up(blocks, rsn);
    uint64_t fec_size = rounds * roots * FEC_BLOCKSIZE;

    void *rs = init_rs_char(8, 0x11d, 0, 1, (int)roots, 0);
    if (!rs) {
        fprintf(stderr, "init_rs_char failed\n");
        return 1;
    }

    unsigned char *fec = malloc(fec_size);
    unsigned char *data = malloc((size_t)rsn * sizeof(unsigned char));
    unsigned char parity[FEC_RSM];

    uint64_t codewords = rounds * FEC_BLOCKSIZE;
    for (uint64_t c = 0; c < codewords; c++) {
        for (uint64_t j = 0; j < rsn; j++) {
            uint64_t off = j * codewords + c;
            unsigned char b = 0;
            if (off < (uint64_t)sz) {
                fseeko(in, (int64_t)off, SEEK_SET);
                if (fread(&b, 1, 1, in) != 1) {
                    fprintf(stderr, "read failed at %llu\n", (unsigned long long)off);
                    return 1;
                }
            }
            data[j] = b;
        }
        encode_rs_char(rs, data, parity);
        memcpy(fec + c * roots, parity, roots);
    }

    /* Write the bare parity payload (no header): the device self-test
     * reports sha256 of exactly this blob as fec_parity_sha256. */
    FILE *out = fopen(argv[2], "wb");
    if (!out) {
        perror("open out");
        return 1;
    }
    if (fwrite(fec, 1, fec_size, out) != fec_size) {
        fprintf(stderr, "write failed\n");
        return 1;
    }
    fclose(out);
    free(fec);
    free(data);
    free_rs_char(rs);
    fclose(in);
    printf("fec_parity_bytes=%llu\n", (unsigned long long)fec_size);
    return 0;
}
