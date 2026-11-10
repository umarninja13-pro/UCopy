package com.umkt.ucopy.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.umkt.ucopy.ui.design.DesignMode

@Composable
fun DetailTitleSection(
    isEditing: Boolean,
    titleFieldValue: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit,
    title: String,
    designMode: DesignMode,
    titleTextStyle: TextStyle,
    focusRequester: FocusRequester,
    onEditClick: () -> Unit
) {
    if (isEditing) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onEditClick() }
        ) {
            BasicTextField(
                value = titleFieldValue,
                onValueChange = onTitleChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .padding(horizontal = if (designMode == DesignMode.NOTEBOOKS) 8.dp else 0.dp),
                textStyle = titleTextStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = Int.MAX_VALUE
            )
            if (title.isEmpty()) {
                Text(
                    text = stringResource(R.string.title),
                    style = titleTextStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    } else {
        Text(
            text = title.ifEmpty { stringResource(R.string.untitled) },
            style = titleTextStyle,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Visible,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onEditClick() }
        )
    }
}