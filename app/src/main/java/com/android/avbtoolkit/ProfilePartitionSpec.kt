package com.android.avbtoolkit

/**
 * One partition of an imported signing profile. Mirrors the config
 * generator's v3 `PartitionConfig` schema, so every field the generator
 * can write is parsed and mapped onto avbtool flags in `buildAvbArgs`.
 *
 * Ported from the AVBTool Android (Compose) project, Apache-2.0.
 */
data class ProfilePartitionSpec(
    val partition: String,
    val image: String,
    val descriptor: String,
    val algorithm: String,
    val keyId: String?,
    val partitionName: String,
    val partitionSize: Long?,
    val rollbackIndex: Long?,
    val salt: String?,
    val flags: Long?,
    val props: List<Pair<String, String>>,
    val setHashtreeDisabledFlag: Boolean,
    val includedPartitions: List<String>,
    val chainPartitions: List<String>,
    // hash: partition size is derived from the image instead of fixed.
    val dynamicPartitionSize: Boolean = false,
    // Location of the main vbmeta rollback index (all three commands).
    val rollbackIndexLocation: Long? = null,
    // Footer hash algorithm; the generator defaults to sha256, while bare
    // avbtool would silently fall back to sha1 for hashtree partitions.
    val hashAlgorithm: String = "sha256",
    val propFromFile: List<Pair<String, String>> = emptyList(),
    val setVerificationDisabledFlag: Boolean = false,
    // hashtree-specific
    val blockSize: Long = 4096,
    val doNotGenerateFec: Boolean = false,
    val fecNumRoots: Long = 2,
    val noHashtree: Boolean = false,
    val checkAtMostOnce: Boolean = false,
    val setupAsRootfsFromKernel: Boolean = false,
    // vbmeta / footer common
    val includeDescriptorsFromImage: List<String> = emptyList(),
    val chainPartitionsDoNotUseAb: List<String> = emptyList(),
    val kernelCmdlines: List<String> = emptyList(),
    val setupRootfsFromKernel: String? = null,
    val paddingSize: Long? = null,
    val outputVbmetaImage: String? = null,
    // behavior switches
    val calcMaxImageSize: Boolean = false,
    val doNotAppendVbmetaImage: Boolean = false,
    val printRequiredLibavbVersion: Boolean = false,
    val usePersistentDigest: Boolean = false,
    val doNotUseAb: Boolean = false,
    // signing helper
    val signingHelper: String? = null,
    val signingHelperWithFiles: String? = null,
    val publicKeyMetadata: String? = null,
    val appendToReleaseString: String? = null,
)
