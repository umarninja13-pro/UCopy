package com.umkt.ucopy.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R

@Composable
fun AppStyleWidgetPreview(
    theme: String,
    font: String,
    fontFamily: String = "DEFAULT",
    showCopy: Boolean = true,
    showAdd: Boolean = true,
    showExpand: Boolean = true,
    showLabel: Boolean = true,
    titleColor: Int = 0,
    bodyColor: Int = 0
) {
    var bgColor = Color(0xFF1E293B)
    var themeTitleColor = Color(0xFFF8FAFC)
    var themeContentColor = Color(0xFFCBD5E1)
    var isGradient = false

    when (theme) {
        "LIGHT" -> { bgColor = Color(0xFFF8FAFC); themeTitleColor = Color(0xFF0F172A); themeContentColor = Color(0xFF475569) }
        "AMOLED" -> { bgColor = Color(0xFF000000); themeTitleColor = Color(0xFFFFFFFF); themeContentColor = Color(0xFFD4D4D8) }
        "TRANSPARENT" -> { isGradient = true; themeTitleColor = Color(0xFFFFFFFF); themeContentColor = Color(0xFFE4E4E7) }
    }

    val finalTitleColor = if (titleColor != 0) Color(titleColor) else themeTitleColor
    val finalContentColor = if (bodyColor != 0) Color(bodyColor) else themeContentColor

    val titleSize = when (font) {
        "SMALL" -> 15.sp
        "LARGE" -> 24.sp
        "EXTRA_LARGE" -> 30.sp
        else -> 19.sp
    }
    val contentSize = when (font) {
        "SMALL" -> 11.sp
        "LARGE" -> 18.sp
        "EXTRA_LARGE" -> 22.sp
        else -> 14.sp
    }
    val family = when (fontFamily) {
        "SERIF" -> FontFamily.Serif
        "MONOSPACE" -> FontFamily.Monospace
        "CURSIVE" -> FontFamily.Cursive
        else -> FontFamily.Default
    }

    val brush = if (isGradient) {
        Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF9333EA)))
    } else {
        Brush.linearGradient(listOf(bgColor, bgColor))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(brush)
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.widget_preview_title),
                    color = finalTitleColor,
                    fontSize = titleSize,
                    fontFamily = family,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (showCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = finalTitleColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                if (showAdd) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = finalTitleColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.widget_preview_body),
                color = finalContentColor,
                fontSize = contentSize,
                fontFamily = family,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showLabel) {
                    Text(
                        text = "U Copy",
                        color = finalTitleColor.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (showExpand) {
                    Icon(Icons.Default.OpenInFull, contentDescription = null, tint = finalTitleColor, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (isGradient) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(stringResource(R.string.wallpaper_preview_label), color = Color.White, fontSize = 9.sp)
            }
        }
    }
}
