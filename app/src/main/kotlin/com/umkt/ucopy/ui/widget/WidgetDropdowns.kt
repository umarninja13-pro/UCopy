package com.umkt.ucopy.ui.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactDropdown(
    selectedValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedValue.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onValueSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ThemeDropdown(selectedTheme: String, onThemeSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    CompactDropdown(
        selectedValue = selectedTheme,
        options = listOf("LIGHT", "DARK", "AMOLED", "TRANSPARENT"),
        onValueSelected = onThemeSelected,
        modifier = modifier
    )
}

@Composable
fun FontDropdown(selectedFont: String, onFontSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    CompactDropdown(
        selectedValue = selectedFont,
        options = listOf("SMALL", "MEDIUM", "LARGE", "EXTRA_LARGE"),
        onValueSelected = onFontSelected,
        modifier = modifier
    )
}

@Composable
fun FontFamilyDropdown(selectedFamily: String, onFamilySelected: (String) -> Unit, modifier: Modifier = Modifier) {
    CompactDropdown(
        selectedValue = selectedFamily,
        options = listOf("DEFAULT", "SERIF", "MONOSPACE", "CURSIVE"),
        onValueSelected = onFamilySelected,
        modifier = modifier
    )
}
