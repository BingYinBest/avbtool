package com.android.avbtoolkit

import androidx.annotation.StringRes

/**
 * Command model for the bundled AOSP external/avb avbtool (1.3.0, main).
 * Derived directly from the official `avbtool.py` argparse definitions;
 * argument labels use the canonical ``--option`` names so the form and
 * console stay in sync with upstream.
 */

enum class AvbCategory(@StringRes val labelRes: Int) {
    IMAGE(R.string.tab_image),
    VBMETA(R.string.tab_vbmeta),
    OTHER(R.string.tab_others),
}

enum class AvbArgType {
    /** Required positional input/output file (SAF picker). */
    FILE,
    /** Integer/number argument. */
    UINT,
    /** Free-form text (or boolean flag toggles). */
    TEXT,
    /** boolean flag. */
    BOOL,
    /** --algorithm values: signing algorithm dropdown. */
    ALGO,
    /** --hash_algorithm values. */
    HASH,
    /** --flags value. */
    FLAGS,
    /** positional argument (no leading --). */
    POSITIONAL,
}

/** One command-line option of an avbtool subcommand. */
data class AvbArg(
    val key: String,
    val type: AvbArgType = AvbArgType.TEXT,
    /** Human-readable label; falls back to [key] when null. */
    @param:StringRes val labelRes: Int? = null,
    val required: Boolean = false,
    val repeatable: Boolean = false,
    val choices: List<String>? = null,
    /** Boolean flags use --flag or --no-flag conventions; keep as-is. */
    val boolean: Boolean = false,
)

/** One avbtool subcommand plus its argument list. */
data class AvbCommand(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val category: AvbCategory,
    val args: List<AvbArg>,
    /** Output files that only make sense with SAF export after run. */
    val outputs: Boolean = false,
)

object AvbCatalog {

    val all: List<AvbCommand> by lazy {
        listOf(
            // ---------- Image ----------
            AvbCommand(
                "generate_test_image",
                R.string.command_generate_test_image_title,
                R.string.command_generate_test_image_description,
                AvbCategory.IMAGE,
                args = listOf(
                    AvbArg("--image_size", AvbArgType.UINT, required = true),
                    AvbArg("--start_byte", AvbArgType.UINT),
                    AvbArg("--output", AvbArgType.FILE, required = true),
                ),
                outputs = true,
            ),
            AvbCommand(
                "add_hash_footer",
                R.string.command_add_hash_footer_title,
                R.string.command_add_hash_footer_description,
                AvbCategory.IMAGE,
                args = listOf(
                    AvbArg("--image", AvbArgType.FILE, required = true),
                    AvbArg("--partition_size", AvbArgType.UINT, required = true),
                    AvbArg("--partition_name", AvbArgType.TEXT),
                    AvbArg("--hash_algorithm", AvbArgType.HASH,
                        choices = listOf("sha1", "sha256")),
                    AvbArg("--salt", AvbArgType.TEXT),
                    AvbArg("--algorithm", AvbArgType.ALGO,
                        choices = SIGNING_ALGORITHMS),
                    AvbArg("--key", AvbArgType.FILE),
                    AvbArg("--rollback_index", AvbArgType.UINT),
                    AvbArg("--rollback_index_location", AvbArgType.UINT),
                    AvbArg("--output_vbmeta_image", AvbArgType.FILE),
                    AvbArg("--do_not_append_vbmeta_image", AvbArgType.BOOL, boolean = true),
                    AvbArg("--calc_max_image_size", AvbArgType.BOOL, boolean = true),
                    AvbArg("--use_persistent_digest", AvbArgType.BOOL, boolean = true),
                    AvbArg("--do_not_use_ab", AvbArgType.BOOL, boolean = true),
                    AvbArg("--flags", AvbArgType.FLAGS),
                    AvbArg("--prop", AvbArgType.TEXT, repeatable = true),
                    AvbArg("--kernel_cmdline", AvbArgType.TEXT, repeatable = true),
                    AvbArg("--append_to_release_string", AvbArgType.TEXT),
                    AvbArg("--print_required_libavb_version", AvbArgType.BOOL, boolean = true),
                    AvbArg("--set_hashtree_disabled_flag", AvbArgType.BOOL, boolean = true),
                    AvbArg("--set_verification_disabled_flag", AvbArgType.BOOL, boolean = true),
                ),
            ),
            AvbCommand(
                "add_hashtree_footer",
                R.string.command_add_hashtree_footer_title,
                R.string.command_add_hashtree_footer_description,
                AvbCategory.IMAGE,
                args = listOf(
                    AvbArg("--image", AvbArgType.FILE, required = true),
                    AvbArg("--partition_size", AvbArgType.UINT, required = true),
                    AvbArg("--partition_name", AvbArgType.TEXT),
                    AvbArg("--hash_algorithm", AvbArgType.HASH,
                        choices = listOf("sha1", "sha256")),
                    AvbArg("--salt", AvbArgType.TEXT),
                    AvbArg("--block_size", AvbArgType.UINT),
                    AvbArg("--fec_num_roots", AvbArgType.UINT),
                    AvbArg("--do_not_generate_fec", AvbArgType.BOOL, boolean = true),
                    AvbArg("--no_hashtree", AvbArgType.BOOL, boolean = true),
                    AvbArg("--check_at_most_once", AvbArgType.BOOL, boolean = true),
                    AvbArg("--algorithm", AvbArgType.ALGO,
                        choices = SIGNING_ALGORITHMS),
                    AvbArg("--key", AvbArgType.FILE),
                    AvbArg("--rollback_index", AvbArgType.UINT),
                    AvbArg("--rollback_index_location", AvbArgType.UINT),
                    AvbArg("--output_vbmeta_image", AvbArgType.FILE),
                    AvbArg("--do_not_append_vbmeta_image", AvbArgType.BOOL, boolean = true),
                    AvbArg("--calc_max_image_size", AvbArgType.BOOL, boolean = true),
                    AvbArg("--use_persistent_digest", AvbArgType.BOOL, boolean = true),
                    AvbArg("--do_not_use_ab", AvbArgType.BOOL, boolean = true),
                    AvbArg("--setup_as_rootfs_from_kernel", AvbArgType.BOOL, boolean = true),
                    AvbArg("--flags", AvbArgType.FLAGS),
                    AvbArg("--prop", AvbArgType.TEXT, repeatable = true),
                    AvbArg("--kernel_cmdline", AvbArgType.TEXT, repeatable = true),
                    AvbArg("--append_to_release_string", AvbArgType.TEXT),
                    AvbArg("--print_required_libavb_version", AvbArgType.BOOL, boolean = true),
                    AvbArg("--set_hashtree_disabled_flag", AvbArgType.BOOL, boolean = true),
                    AvbArg("--set_verification_disabled_flag", AvbArgType.BOOL, boolean = true),
                ),
            ),
            AvbCommand(
                "append_vbmeta_image",
                R.string.command_append_vbmeta_image_title,
                R.string.command_append_vbmeta_image_description,
                AvbCategory.IMAGE,
                args = listOf(
                    AvbArg("--image", AvbArgType.FILE, required = true),
                    AvbArg("--partition_size", AvbArgType.UINT, required = true),
                    AvbArg("--vbmeta_image", AvbArgType.FILE, required = true),
                ),
            ),
            AvbCommand(
                "erase_footer",
                R.string.command_erase_footer_title,
                R.string.command_erase_footer_description,
                AvbCategory.IMAGE,
                args = listOf(
                    AvbArg("--image", AvbArgType.FILE, required = true),
                    AvbArg("--keep_hashtree", AvbArgType.BOOL, boolean = true),
                ),
            ),
            AvbCommand(
                "zero_hashtree",
                R.string.command_zero_hashtree_title,
                R.string.command_zero_hashtree_description,
                AvbCategory.IMAGE,
                args = listOf(
                    AvbArg("--image", AvbArgType.FILE, required = true),
                ),
            ),
            AvbCommand(
                "resize_image",
                R.string.command_resize_image_title,
                R.string.command_resize_image_description,
                AvbCategory.IMAGE,
                args = listOf(
                    AvbArg("--image", AvbArgType.FILE, required = true),
                    AvbArg("--partition_size", AvbArgType.UINT, required = true),
                ),
            ),
            AvbCommand(
                "extract_vbmeta_image",
                R.string.command_extract_vbmeta_image_title,
                R.string.command_extract_vbmeta_image_description,
                AvbCategory.IMAGE,
                args = listOf(
                    AvbArg("--image", AvbArgType.FILE, required = true),
                    AvbArg("--output", AvbArgType.FILE, required = true),
                    AvbArg("--padding_size", AvbArgType.UINT),
                ),
                outputs = true,
            ),
            AvbCommand(
                "info_image",
                R.string.command_info_image_title,
                R.string.command_info_image_description,
                AvbCategory.IMAGE,
                args = listOf(
                    AvbArg("--image", AvbArgType.FILE, required = true),
                    AvbArg("--cert", AvbArgType.FILE),
                    AvbArg("--output", AvbArgType.FILE),
                ),
            ),
            AvbCommand(
                "verify_image",
                R.string.command_verify_image_title,
                R.string.command_verify_image_description,
                AvbCategory.IMAGE,
                args = listOf(
                    AvbArg("--image", AvbArgType.FILE, required = true),
                    AvbArg("--key", AvbArgType.FILE),
                    AvbArg("--expected_chain_partition", AvbArgType.TEXT, repeatable = true),
                    AvbArg("--follow_chain_partitions", AvbArgType.BOOL, boolean = true),
                    AvbArg("--accept_zeroed_hashtree", AvbArgType.BOOL, boolean = true),
                ),
            ),
            AvbCommand(
                "print_partition_digests",
                R.string.command_print_partition_digests_title,
                R.string.command_print_partition_digests_description,
                AvbCategory.IMAGE,
                args = listOf(
                    AvbArg("--image", AvbArgType.FILE, required = true),
                    AvbArg("--output", AvbArgType.FILE),
                    AvbArg("--json", AvbArgType.BOOL, boolean = true),
                ),
            ),
            AvbCommand(
                "calculate_kernel_cmdline",
                R.string.command_calculate_kernel_cmdline_title,
                R.string.command_calculate_kernel_cmdline_description,
                AvbCategory.IMAGE,
                args = listOf(
                    AvbArg("--image", AvbArgType.FILE, required = true),
                    AvbArg("--hashtree_disabled", AvbArgType.BOOL, boolean = true),
                    AvbArg("--output", AvbArgType.FILE),
                ),
            ),

            // ---------- VBMeta ----------
            AvbCommand(
                "make_vbmeta_image",
                R.string.command_make_vbmeta_image_title,
                R.string.command_make_vbmeta_image_description,
                AvbCategory.VBMETA,
                args = listOf(
                    AvbArg("--output", AvbArgType.FILE, required = true),
                    AvbArg("--algorithm", AvbArgType.ALGO,
                        choices = SIGNING_ALGORITHMS, required = true),
                    AvbArg("--key", AvbArgType.FILE),
                    AvbArg("--public_key_metadata", AvbArgType.FILE),
                    AvbArg("--rollback_index", AvbArgType.UINT),
                    AvbArg("--rollback_index_location", AvbArgType.UINT),
                    AvbArg("--padding_size", AvbArgType.UINT),
                    AvbArg("--flags", AvbArgType.FLAGS),
                    AvbArg("--set_hashtree_disabled_flag", AvbArgType.BOOL, boolean = true),
                    AvbArg("--set_verification_disabled_flag", AvbArgType.BOOL, boolean = true),
                    AvbArg("--prop", AvbArgType.TEXT, repeatable = true),
                    AvbArg("--kernel_cmdline", AvbArgType.TEXT, repeatable = true),
                    AvbArg("--chain_partition", AvbArgType.TEXT, repeatable = true),
                    AvbArg("--include_descriptors_from_image", AvbArgType.FILE, repeatable = true),
                    AvbArg("--append_to_release_string", AvbArgType.TEXT),
                    AvbArg("--print_required_libavb_version", AvbArgType.BOOL, boolean = true),
                ),
                outputs = true,
            ),
            AvbCommand(
                "extract_public_key",
                R.string.command_extract_public_key_title,
                R.string.command_extract_public_key_description,
                AvbCategory.VBMETA,
                args = listOf(
                    AvbArg("--key", AvbArgType.FILE, required = true),
                    AvbArg("--output", AvbArgType.FILE, required = true),
                ),
                outputs = true,
            ),
            AvbCommand(
                "extract_public_key_digest",
                R.string.command_extract_public_key_digest_title,
                R.string.command_extract_public_key_digest_description,
                AvbCategory.VBMETA,
                args = listOf(
                    AvbArg("--key", AvbArgType.FILE, required = true),
                    AvbArg("--output", AvbArgType.FILE, required = true),
                ),
                outputs = true,
            ),
            AvbCommand(
                "calculate_vbmeta_digest",
                R.string.command_calculate_vbmeta_digest_title,
                R.string.command_calculate_vbmeta_digest_description,
                AvbCategory.VBMETA,
                args = listOf(
                    AvbArg("--image", AvbArgType.FILE, required = true),
                    AvbArg("--hash_algorithm", AvbArgType.HASH,
                        choices = listOf("sha256", "sha512")),
                    AvbArg("--output", AvbArgType.FILE),
                    AvbArg("--format", AvbArgType.TEXT,
                        choices = listOf("json", "json_pretty")),
                ),
            ),
            AvbCommand(
                "update_partition_descriptor",
                R.string.command_update_partition_descriptor_title,
                R.string.command_update_partition_descriptor_description,
                AvbCategory.VBMETA,
                args = listOf(
                    AvbArg("--image", AvbArgType.FILE, required = true),
                    AvbArg("--partition_image", AvbArgType.FILE, required = true),
                    AvbArg("--output", AvbArgType.FILE, required = true),
                    AvbArg("--algorithm", AvbArgType.ALGO,
                        choices = SIGNING_ALGORITHMS),
                    AvbArg("--key", AvbArgType.FILE),
                    AvbArg("--rollback_index", AvbArgType.UINT),
                    AvbArg("--rollback_index_location", AvbArgType.UINT),
                    AvbArg("--set_hashtree_disabled_flag", AvbArgType.BOOL, boolean = true),
                    AvbArg("--set_verification_disabled_flag", AvbArgType.BOOL, boolean = true),
                    AvbArg("--flags", AvbArgType.FLAGS),
                ),
                outputs = true,
            ),
            AvbCommand(
                "resign_image",
                R.string.command_resign_image_title,
                R.string.command_resign_image_description,
                AvbCategory.VBMETA,
                args = listOf(
                    AvbArg("--image", AvbArgType.FILE, required = true),
                    AvbArg("--key", AvbArgType.FILE, required = true),
                    AvbArg("--algorithm", AvbArgType.ALGO,
                        choices = SIGNING_ALGORITHMS, required = true),
                    AvbArg("--auto_resize", AvbArgType.BOOL, boolean = true),
                ),
            ),

            // ---------- Other ----------
            AvbCommand(
                "version",
                R.string.command_version_title,
                R.string.command_version_description,
                AvbCategory.OTHER,
                args = emptyList(),
            ),
            AvbCommand(
                "set_ab_metadata",
                R.string.command_set_ab_metadata_title,
                R.string.command_set_ab_metadata_description,
                AvbCategory.OTHER,
                args = listOf(
                    AvbArg("--misc_image", AvbArgType.FILE, required = true),
                    AvbArg("--slot_data", AvbArgType.TEXT, required = true,
                        choices = listOf("15:7:0:14:7:0", "15:0:0:14:0:0")),
                ),
            ),
            AvbCommand(
                "make_certificate",
                R.string.command_make_certificate_title,
                R.string.command_make_certificate_description,
                AvbCategory.OTHER,
                args = listOf(
                    AvbArg("--output", AvbArgType.FILE, required = true),
                    AvbArg("--subject", AvbArgType.TEXT, required = true),
                    AvbArg("--subject_key", AvbArgType.FILE, required = true),
                    AvbArg("--subject_key_version", AvbArgType.UINT),
                    AvbArg("--subject_is_intermediate_authority", AvbArgType.BOOL, boolean = true),
                    AvbArg("--usage", AvbArgType.TEXT,
                        choices = listOf("ecdsa-sha256", "ecdsa-sha512")),
                    AvbArg("--usage_for_unlock", AvbArgType.BOOL, boolean = true),
                    AvbArg("--authority_key", AvbArgType.FILE),
                    AvbArg("--signing_helper", AvbArgType.TEXT),
                    AvbArg("--signing_helper_with_files", AvbArgType.TEXT),
                ),
                outputs = true,
            ),
            AvbCommand(
                "make_cert_permanent_attributes",
                R.string.command_make_cert_permanent_attributes_title,
                R.string.command_make_cert_permanent_attributes_description,
                AvbCategory.OTHER,
                args = listOf(
                    AvbArg("--output", AvbArgType.FILE, required = true),
                    AvbArg("--root_authority_key", AvbArgType.FILE, required = true),
                    AvbArg("--product_id", AvbArgType.TEXT, required = true),
                ),
                outputs = true,
            ),
            AvbCommand(
                "make_cert_metadata",
                R.string.command_make_cert_metadata_title,
                R.string.command_make_cert_metadata_description,
                AvbCategory.OTHER,
                args = listOf(
                    AvbArg("--output", AvbArgType.FILE, required = true),
                    AvbArg("--intermediate_key_certificate", AvbArgType.FILE, required = true),
                    AvbArg("--product_key_certificate", AvbArgType.FILE, required = true),
                ),
                outputs = true,
            ),
            AvbCommand(
                "make_cert_unlock_credential",
                R.string.command_make_cert_unlock_credential_title,
                R.string.command_make_cert_unlock_credential_description,
                AvbCategory.OTHER,
                args = listOf(
                    AvbArg("--output", AvbArgType.FILE, required = true),
                    AvbArg("--intermediate_key_certificate", AvbArgType.FILE, required = true),
                    AvbArg("--unlock_key_certificate", AvbArgType.FILE, required = true),
                    AvbArg("--challenge", AvbArgType.TEXT, required = true),
                    AvbArg("--unlock_key", AvbArgType.FILE, required = true),
                    AvbArg("--signing_helper", AvbArgType.TEXT),
                    AvbArg("--signing_helper_with_files", AvbArgType.TEXT),
                ),
                outputs = true,
            ),
        )
    }

    fun byId(id: String): AvbCommand? = all.find { it.id == id }

    fun forCategory(category: AvbCategory): List<AvbCommand> =
        all.filter { it.category == category }

    /** Signing algorithms advertised by avbtool 1.3.0. */
    val SIGNING_ALGORITHMS = listOf(
        "NONE",
        "SHA256_RSA2048",
        "SHA256_RSA4096",
        "SHA512_RSA2048",
        "SHA512_RSA4096",
        "SHA256_RSA8192",
        "SHA512_RSA8192",
        "SHA256_ECDSA_P256",
        "SHA256_ECDSA_P384",
        "SHA512_ECDSA_P256",
        "SHA512_ECDSA_P384",
    )
}