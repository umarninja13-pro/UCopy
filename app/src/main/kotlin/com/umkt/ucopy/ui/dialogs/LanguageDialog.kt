package com.umkt.ucopy.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.umkt.ucopy.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDialog(
    currentLanguage: String, // "system", "en", "ur"
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.language_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            LanguageOption("system", "🌐", stringResource(R.string.language_system), currentLanguage, onLanguageSelected, onDismiss)
            LanguageOption("en", "🇬🇧", stringResource(R.string.language_english), currentLanguage, onLanguageSelected, onDismiss)
            LanguageOption("ur", "🇵🇰", stringResource(R.string.language_urdu), currentLanguage, onLanguageSelected, onDismiss)
        }
    }
}

@Composable
private fun LanguageOption(
    code: String,
    flag: String,
    label: String,
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isSelected = current == code
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSelect(code)
                onDismiss()
            }
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(40.dp))
        }
        Text(
            text = flag,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
