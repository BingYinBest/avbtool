package com.android.avbtoolkit.ui.util

import android.content.Context
import android.content.pm.PackageManager

fun getAppVersion(context: Context): String {
    val packageManager = context.packageManager
    val info = packageManager.getPackageInfo(context.packageName, 0)
    return info.versionName ?: "unknown"
}
