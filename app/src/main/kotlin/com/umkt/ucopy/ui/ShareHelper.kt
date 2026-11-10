package com.umkt.ucopy.ui

import android.content.Context
import android.content.Intent
import com.umkt.ucopy.R
import com.umkt.ucopy.model.Clip

fun shareItem(context: Context, item: Clip) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, item.content)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_via)))
}