package com.example.hikerview.utils.view

import android.graphics.Bitmap
import androidx.palette.graphics.Palette

/**
 * 作者：By 15968
 * 日期：On 2021/12/6
 * 时间：At 14:50
 */
object ImageUtil {

    fun createPaletteAsync(bitmap: Bitmap, defaultColor: Int, consumer: (Int) -> Int) {
        Palette.from(bitmap)
            .generate { palette ->
                when {
                    palette == null -> {
                        consumer(defaultColor)
                    }
                    palette.getDarkVibrantColor(defaultColor) != defaultColor -> {
                        consumer(palette.getDarkVibrantColor(defaultColor))
                    }
                    palette.getDarkMutedColor(defaultColor) != defaultColor -> {
                        consumer(palette.getDarkMutedColor(defaultColor))
                    }
                    else -> {
                        consumer(palette.getLightVibrantColor(defaultColor))
                    }
                }
            }
    }
}