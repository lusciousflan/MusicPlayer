package com.example.musicplayer.ui.view

import android.view.View
import android.util.TypedValue
import androidx.core.content.ContextCompat

fun View.setOnClickListenerWithPressAnimation(onClick: () -> Unit) {
    // Android標準のRippleを使い、タップ位置から短時間だけ色を変える。
    val backgroundAttribute = TypedValue()
    if (context.theme.resolveAttribute(android.R.attr.selectableItemBackground, backgroundAttribute, true)) {
        foreground = ContextCompat.getDrawable(context, backgroundAttribute.resourceId)
    }
    setOnClickListener {
        onClick()
    }
}
