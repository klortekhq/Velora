package com.klortek.velora.ui

import android.content.Context
import android.content.pm.PackageManager

object DeviceUtils {
    fun isTvDevice(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}
