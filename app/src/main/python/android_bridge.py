# SPDX-License-Identifier: Apache-2.0
"""On-device helpers for the bundled AOSP avbtool.py.

This module replaces the two external processes avbtool needs on a
normal host with in-process implementations that run inside the app's
Python runtime:

* RSA key handling and signing use the ``cryptography`` package
  (installed by Chaquopy) instead of the ``openssl`` binary.
* FEC encoding uses the native ``libavbfec.so`` (which links the AOSP
  external/fec RS(255, N) codec, LGPL-2.1) instead of the ``fec``
  binary.

It also installs a ``builtins.open`` hook that maps pseudo-paths of the
form ``/saf/fd/<fd>`` to ``os.fdopen`` so the Kotlin side can hand SAF
file descriptors to Python without copying multi-GiB images.
"""

import builtins
import ctypes
import os

import cryptography.hazmat.primitives.asymmetric.padding as _padding
import cryptography.hazmat.primitives.hashes as _hashes
import cryptography.hazmat.primitives.serialization as _serialization

_libavbfec = None
_libfec_rs = None
_INITIALIZED = False


def is_initialized():
    """Whether init() has been called successfully."""
    return _INITIALIZED


def init(native_lib_dir):
    """Load the native FEC libraries. Call once, before any FEC use."""
    global _libavbfec, _libfec_rs, _INITIALIZED
    if _INITIALIZED:
        return
    # libavbfec.so has a DT_NEEDED dependency on libfec_rs.so (LGPL codec,
    # kept as a separate shared object). Load the dependency first so the
    # dynamic linker can resolve it regardless of its search path.
    _libfec_rs = ctypes.CDLL(os.path.join(native_lib_dir, "libfec_rs.so"))
    _libavbfec = ctypes.CDLL(os.path.join(native_lib_dir, "libavbfec.so"))
    _libavbfec.avb_fec_print_size.argtypes = [ctypes.c_uint64, ctypes.c_int]
    _libavbfec.avb_fec_print_size.restype = ctypes.c_uint64
    _libavbfec.avb_fec_encode.argtypes = [ctypes.c_int, ctypes.c_char_p, ctypes.c_int]
    _libavbfec.avb_fec_encode.restype = ctypes.c_int
    _INITIALIZED = True
    install_fd_open_hook()


# ---------------------------------------------------------------------------
# SAF file descriptor bridge
# ---------------------------------------------------------------------------

class _SafFileWrapper(object):
    """File object wrapper keeping the /saf/fd pseudo-path as .name.

    avbtool uses ``file.name`` as a filesystem path in several places;
    ``os.fdopen`` would expose an int instead. Delegating all other file
    operations keeps sparse detection, seek/tell and reads transparent.
    """

    def __init__(self, path, fileobj):
        object.__setattr__(self, '_fileobj', fileobj)
        object.__setattr__(self, '_path', path)

    def __getattr__(self, item):
        return getattr(self._fileobj, item)

    def __getattribute__(self, item):
        if item == 'name':
            return object.__getattribute__(self, '_path')
        return object.__getattribute__(self, '_fileobj').__getattribute__(item) \
            if item != '_fileobj' else object.__getattribute__(self, '_fileobj')

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self.close()
        return False

    def __iter__(self):
        return iter(self._fileobj)

    def close(self):
        self._fileobj.close()


def install_fd_open_hook():
    """Route ``/saf/fd/<fd>`` pseudo-paths through os.fdopen."""
    if getattr(builtins, '_avb_saf_hook_installed', False):
        return

    original_open = builtins.open

    def _open(path, mode='r', buffering=-1, encoding=None, errors=None,
              newline=None, closefd=True, opener=None):
        if isinstance(path, str) and path.startswith('/saf/fd/'):
            rest = path.rsplit('/', 1)[1]
            if rest.isdigit():
                fd = int(rest)
                # dup() keeps the shared file offset; avbtool reopens the
                # same path several times, so rewind each dup to the start.
                dup = os.dup(fd)
                if 'a' not in mode:
                    try:
                        os.lseek(dup, 0, os.SEEK_SET)
                    except OSError:
                        pass
                if 'b' in mode:
                    fobj = os.fdopen(dup, mode, buffering=buffering)
                else:
                    fobj = os.fdopen(dup, mode, buffering=buffering,
                                     encoding=encoding or 'utf-8',
                                     errors=errors or 'strict',
                                     newline=newline)
                return _SafFileWrapper(path, fobj)
        return original_open(path, mode, buffering=buffering, encoding=encoding,
                             errors=errors, newline=newline, closefd=closefd,
                             opener=opener)

    builtins.open = _open
    builtins._avb_saf_hook_installed = True


# ---------------------------------------------------------------------------
# RSA via cryptography
# ---------------------------------------------------------------------------

def load_rsa_key(key_path):
    """Return (modulus, num_bits, exponent) for an RSA private/public key."""
    with open(key_path, 'rb') as f:
        data = f.read()
    try:
        key = _serialization.load_pem_private_key(data, password=None)
        pub = key.public_key()
    except Exception:
        key = _serialization.load_pem_public_key(data)
        pub = key
    numbers = pub.public_numbers()
    modulus = numbers.n
    num_bits = round_up_to_pow2(modulus.bit_length())
    return modulus, num_bits, numbers.e


def rsa_sign(key_path, algorithm_name, data_to_sign):
    """Sign |data_to_sign| exactly like avbtool's openssl rsautl -sign path.

    The PKCS#1 v1.5 EM built by cryptography (DigestInfo + digest +
    padding) is byte-identical to avbtool's ``algorithm.padding + digest``
    followed by raw RSA (RSASP1), matching the openssl -sign -raw output.
    """
    with open(key_path, 'rb') as f:
        key = _serialization.load_pem_private_key(f.read(), password=None)
    hash_name = algorithm_name.split('_')[0].lower()
    chosen_hash = {'sha256': _hashes.SHA256(), 'sha512': _hashes.SHA512()}[hash_name]
    return key.sign(data_to_sign, _padding.PKCS1v15(), chosen_hash)


def rsa_verify(num_bits, modulus, sig_blob, expected):
    """Raw RSA verification (exponent 65537).

    Equivalent to avbtool's old ``openssl rsautl -verify -pubin -raw``:
    checks pow(signature, e, n) == expected_blob.
    """
    if num_bits <= 0 or modulus <= 0:
        return False
    signature = int.from_bytes(sig_blob, 'big')
    expected_int = int.from_bytes(expected, 'big')
    return pow(signature, 65537, modulus) == expected_int


# ---------------------------------------------------------------------------
# ML-DSA (FIPS 204) via pure-Python dilithium-py
# ---------------------------------------------------------------------------

def mldsa_available():
    """Whether the bundled pure-Python ML-DSA implementation is usable."""
    try:
        import dilithium_py  # noqa: F401
        return True
    except Exception:
        return False


_MLDSA_ALGS = {'MLDSA65': None, 'MLDSA87': None}


def _mldsa_alg_from_name(alg_name):
    from dilithium_py.ml_dsa import ML_DSA_65, ML_DSA_87
    _MLDSA_ALGS['MLDSA65'] = ML_DSA_65
    _MLDSA_ALGS['MLDSA87'] = ML_DSA_87
    return _MLDSA_ALGS.get(alg_name)


def _mldsa_alg_from_bytes(pub):
    from dilithium_py.ml_dsa import ML_DSA_65, ML_DSA_87
    if len(pub) == ML_DSA_65._pk_size():
        return 'MLDSA65', ML_DSA_65
    if len(pub) == ML_DSA_87._pk_size():
        return 'MLDSA87', ML_DSA_87
    raise ValueError('unrecognised ML-DSA public key size %d' % len(pub))


def mldsa_load(key_path):
    """Load an ML-DSA key; returns (alg_name, pub, sk_or_None).

    Accepts PKCS#8 private keys and SubjectPublicKeyInfo public keys in
    PEM form (both as produced by openssl 3.5+ `genpkey -algorithm
    ML-DSA-*`), matching what upstream avbtool reads.
    """
    from dilithium_py.ml_dsa import pkcs

    with open(key_path, 'rb') as f:
        data = f.read()

    # Try private key first (PKCS#8 PEM).
    try:
        text = data.decode('utf-8')
    except UnicodeDecodeError:
        text = None
    if text is not None:
        try:
            (ml_dsa, sk, _seed, pk) = pkcs.sk_from_pem(text)
            for name in ('MLDSA65', 'MLDSA87'):
                if _mldsa_alg_from_name(name) is ml_dsa:
                    return name, pk, sk
        except Exception:
            pass
        try:
            (ml_dsa, pk) = pkcs.pk_from_pem(text)
            for name in ('MLDSA65', 'MLDSA87'):
                if _mldsa_alg_from_name(name) is ml_dsa:
                    return name, pk, None
        except Exception:
            pass
    raise ValueError('not a recognised ML-DSA PEM key: ' + key_path)


def mldsa_sign(sk, data_to_sign):
    """Sign |data_to_sign| with an ML-DSA private key (FIPS 204 pure)."""
    from dilithium_py.ml_dsa import ML_DSA_65, ML_DSA_87
    for cls in (ML_DSA_65, ML_DSA_87):
        if len(sk) == cls._sk_size():
            return cls.sign(sk, data_to_sign)
    raise ValueError('unrecognised ML-DSA private key size %d' % len(sk))


def mldsa_verify(pub, data, signature):
    """Verify an ML-DSA signature over |data| with a raw public key."""
    _alg, cls = _mldsa_alg_from_bytes(pub)
    return cls.verify(pub, data, signature)


# ---------------------------------------------------------------------------
# Native FEC
# ---------------------------------------------------------------------------

def fec_print_size(image_size, roots):
    """Same value as AOSP ``fec --print-fec-size`` (incl. header block)."""
    if _libavbfec is None:
        raise RuntimeError('android_bridge.init() has not been called')
    size = _libavbfec.avb_fec_print_size(image_size, roots)
    if size == 0:
        raise ValueError('invalid parameters for native fec')
    return int(size)


def fec_encode_fd(in_fd, out_path, roots):
    """Encode FEC data from an open fd into out_path (AOSP fec layout)."""
    if _libavbfec is None:
        raise RuntimeError('android_bridge.init() has not been called')
    return int(_libavbfec.avb_fec_encode(int(in_fd), out_path.encode('utf-8'),
                                         int(roots)))


# ---------------------------------------------------------------------------
# Entry point used by the Kotlin side
# ---------------------------------------------------------------------------

def run_avbtool(argv):
    """Run avbtool with argv; returns (exit_code, stdout, stderr)."""
    import io
    import sys
    import traceback

    argv = list(argv)
    if argv and 'avbtool_fec_self_test' in argv[:3]:
        out, err = fec_self_test()
        return 0, out, err

    # avbtool.py parses argv[1:], so argv[0] must be the program name.
    # Normalise whatever the console/GUI passed in.
    if argv and argv[0] not in ('avbtool', 'avbtool.py'):
        argv = ['avbtool.py'] + argv

    import avbtool

    old_stdout, old_stderr = sys.stdout, sys.stderr
    out, err = io.StringIO(), io.StringIO()
    sys.stdout, sys.stderr = out, err
    code = 0
    try:
        tool = avbtool.AvbTool()
        tool.run(argv)
    except SystemExit as e:
        code = int(e.code or 0)
    except Exception:
        err.write(traceback.format_exc())
        code = 1
    finally:
        sys.stdout, sys.stderr = old_stdout, old_stderr
    return code, out.getvalue(), err.getvalue()


def fec_self_test():
    """Deterministic native FEC self-check.

    Encodes the same 1 MiB pattern that tools/avb-verify uses on the host
    and prints the parity size and SHA-256; compare with
    tools/avb-verify/expected_fec_sha256.txt.
    """
    import hashlib
    import struct
    import tempfile

    if _libavbfec is None:
        raise RuntimeError('android_bridge.init() has not been called')

    nl = chr(10)
    roots = 2
    data = (bytes(range(256)) * 16) * 256  # 1 MiB deterministic image
    img_path = None
    fec_path = None
    try:
        with tempfile.NamedTemporaryFile(delete=False, suffix='.img') as f:
            img_path = f.name
            f.write(data)
        fec_path = img_path + '.fec'
        with open(img_path, 'rb') as f:
            rc = fec_encode_fd(f.fileno(), fec_path, roots)
        if rc != 0:
            return 'native fec encoder returned %d%s' % (rc, nl), ''
        fec_bytes = open(fec_path, 'rb').read()
        footer_format = '<LLLLLQ32s'
        footer_size = struct.calcsize(footer_format)
        (magic, version, size, roots_out, fec_size, inp_size,
         _hash) = struct.unpack_from(footer_format, fec_bytes[-footer_size:])
        parity = fec_bytes[0:fec_size]
        lines = [
            'fec_parity_bytes=%d' % fec_size,
            'fec_parity_sha256=' + hashlib.sha256(parity).hexdigest(),
            'fec_file_bytes=%d' % len(fec_bytes),
            'header_magic=' + hex(magic),
            'header_version=%d' % version,
            'header_size=%d' % size,
            'header_roots=%d' % roots_out,
            'header_inp_size=%d' % inp_size,
            'header_hash_field_ok=%s' % (
                hashlib.sha256(parity).digest() == _hash),
        ]
        return nl.join(lines) + nl, ''
    finally:
        for p in (img_path, fec_path):
            if p:
                try:
                    os.unlink(p)
                except OSError:
                    pass


def round_up_to_pow2(n):
    p = 1
    while p < n:
        p <<= 1
    return p