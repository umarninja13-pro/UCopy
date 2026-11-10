package com.umkt.ucopy.ui.home

import androidx.compose.ui.res.stringResource
import com.umkt.ucopy.R

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.umkt.ucopy.ui.design.DesignMode
import com.umkt.ucopy.ui.design.LocalDesignConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.umkt.ucopy.ui.common.ViewDensity
import com.umkt.ucopy.ui.state.ClipViewModel
import com.umkt.ucopy.ui.state.UiState
import com.umkt.ucopy.ui.state.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDrawer(
    drawerState: DrawerState,
    scopeDrawer: CoroutineScope,
    viewModel: ClipViewModel,
    categories: List<String>,
    viewDensity: ViewDensity,
    state: UiState,
    isClipboardListenerRunning: Boolean,
    deletedItemsCount: Int,
    onThemeClick: () -> Unit,
    onRecycleBinClick: () -> Unit,
    onAboutClick: () -> Unit,
    onListenerClick: () -> Unit,
    onBackupRestoreClick: () -> Unit,
    onImportClick: () -> Unit,
    onDesignClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onEditLabelsClick: () -> Unit,
    onCreateLabelClick: () -> Unit,
    onTodoClick: () -> Unit,
    content: @Composable () -> Unit
) {
    var viewModeExpanded by remember { mutableStateOf(false) }
    val design = LocalDesignConfig.current
    val context = LocalContext.current

    val drawerShape = when (design.mode) {
        DesignMode.NOTEBOOKS -> RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
        DesignMode.LIST -> RoundedCornerShape(0.dp)
        DesignMode.TASK -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    }
    val drawerColor = when (design.mode) {
        DesignMode.LIST -> MaterialTheme.colorScheme.background
        DesignMode.NOTEBOOKS -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                drawerShape = drawerShape,
                drawerContainerColor = drawerColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp)
                ) {
                    // Header (Different per design)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = when (design.mode) {
                                DesignMode.NOTEBOOKS -> stringResource(R.string.design_notebooks_name)
                                DesignMode.LIST -> stringResource(R.string.design_simple_name)
                                DesignMode.TASK -> stringResource(R.string.design_sticky_name)
                                else -> "U Copy"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (design.mode == DesignMode.NOTEBOOKS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                        // Item count removed per user request
                    }

                    if (design.mode != DesignMode.LIST) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // LIBRARY section
                    DrawerSectionHeader(stringResource(R.string.library))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.all_items)) },
                        selected = state.selectedCategory == null &&
                                state.sortOption == SortOption.AZ,
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = {
                            viewModel.setCategory(null)
                            viewModel.setSortOption(SortOption.AZ)
                                                        scopeDrawer.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(if (design.mode == DesignMode.LIST) 0.dp else 100.dp)
                    )


                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.labels),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.edit),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                scopeDrawer.launch { drawerState.close() }
                                onEditLabelsClick()
                            }
                        )
                    }
                    
                    categories.forEach { cat ->
                        NavigationDrawerItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                            label = { Text(cat) },
                            selected = state.selectedCategory == cat,
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = {
                                viewModel.setCategory(cat)
                                scopeDrawer.launch { drawerState.close() }
                            },
                            shape = RoundedCornerShape(if (design.mode == DesignMode.LIST) 0.dp else 100.dp)
                        )
                    }
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        label = { Text(stringResource(R.string.create_new_label)) },
                        selected = false,
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = {
                            scopeDrawer.launch { drawerState.close() }
                            onCreateLabelClick()
                        },
                        shape = RoundedCornerShape(if (design.mode == DesignMode.LIST) 0.dp else 100.dp)
                    )


                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                    // TOOLS section
                    DrawerSectionHeader(stringResource(R.string.settings))
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                        label = { Text(stringResource(R.string.design)) },
                        selected = false,
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = {
                            onDesignClick()
                            scopeDrawer.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(if (design.mode == DesignMode.LIST) 0.dp else 100.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Language, contentDescription = null) },
                        label = { Text(stringResource(R.string.language)) },
                        selected = false,
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = {
                            onLanguageClick()
                            scopeDrawer.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(if (design.mode == DesignMode.LIST) 0.dp else 100.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.theme)) },
                        selected = false,
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = {
                            onThemeClick()
                            scopeDrawer.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(if (design.mode == DesignMode.LIST) 0.dp else 100.dp)
                    )
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                if (isClipboardListenerRunning) Icons.Default.NotificationsActive
                                else Icons.Default.NotificationsOff,
                                contentDescription = null
                            )
                        },
                        label = { Text(if (isClipboardListenerRunning) stringResource(R.string.listener_on) else stringResource(R.string.listener_off)) },
                        selected = false,
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = {
                            onListenerClick()
                            scopeDrawer.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(if (design.mode == DesignMode.LIST) 0.dp else 100.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        label = { Text(stringResource(R.string.recycle_bin_count, deletedItemsCount)) },
                        selected = false,
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = {
                            onRecycleBinClick()
                            scopeDrawer.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(if (design.mode == DesignMode.LIST) 0.dp else 100.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                        label = { Text(stringResource(R.string.import_drawer_label)) },
                        selected = false,
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = {
                            onImportClick()
                            scopeDrawer.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(if (design.mode == DesignMode.LIST) 0.dp else 100.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Backup, contentDescription = null) },
                        label = { Text(stringResource(R.string.backup_restore)) },
                        selected = false,
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = {
                            onBackupRestoreClick()
                            scopeDrawer.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(if (design.mode == DesignMode.LIST) 0.dp else 100.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        label = { Text(stringResource(R.string.about)) },
                        selected = false,
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = {
                            onAboutClick()
                            scopeDrawer.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(if (design.mode == DesignMode.LIST) 0.dp else 100.dp)
                    )

                }
            }
        }
    ) {
        content()
    }
}
