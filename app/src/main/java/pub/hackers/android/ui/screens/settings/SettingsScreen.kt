package pub.hackers.android.ui.screens.settings
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.data.local.StoredAccount
import pub.hackers.android.ui.components.LargeTitleHeader
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography
import pub.hackers.android.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    onSignInClick: () -> Unit,
    onSignOutComplete: () -> Unit,
    onAccountSwitched: () -> Unit,
    onAddAccountClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onDraftsClick: () -> Unit = {},
    onBookmarksClick: () -> Unit = {},
    onLicensesClick: () -> Unit = {},
    isLoggedIn: Boolean,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAddPasskeyDialog by remember { mutableStateOf(false) }
    var showRevokePasskeyId by remember { mutableStateOf<String?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    // Passkey requires the feature flag AND Android 9+
    // (androidx.credentials.PublicKeyCredential is API 28+).
    val passkeyEnabled = pub.hackers.android.FeatureFlags.PASSKEY_AUTH_ENABLED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    LaunchedEffect(isLoggedIn, passkeyEnabled) {
        if (isLoggedIn && passkeyEnabled) viewModel.loadPasskeys()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.SignedOut -> onSignOutComplete()
                SettingsEvent.AccountSwitched -> onAccountSwitched()
            }
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.sign_out)) },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                    }
                ) {
                    Text(
                        stringResource(R.string.sign_out),
                        color = colors.accent
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(
                        stringResource(R.string.cancel),
                        color = colors.accent
                    )
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTitleHeader(
                title = stringResource(R.string.settings),
                leadingContent = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = colors.accent
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoggedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            uiState.userHandle?.let { onProfileClick(it) }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = uiState.userAvatar,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.userName ?: "",
                            style = typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = uiState.userHandle ?: "",
                            style = typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                    AccountSwitcherMenu(
                        accounts = uiState.accounts.filter { it.userId != uiState.activeUserId },
                        enabled = !uiState.isChangingAccount,
                        onSwitchAccount = viewModel::switchAccount,
                        onAddAccountClick = onAddAccountClick,
                    )
                }
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
            }

            if (isLoggedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSignOutDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = colors.reaction
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.sign_out),
                        style = typography.bodyLarge,
                        color = colors.textPrimary
                    )
                }

                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                // My Drafts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDraftsClick() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Article,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.my_drafts),
                        style = typography.bodyLarge,
                        color = colors.textPrimary
                    )
                }

                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBookmarksClick() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.bookmarks),
                        style = typography.bodyLarge,
                        color = colors.textPrimary
                    )
                }

                if (passkeyEnabled) {
                    HorizontalDivider(color = colors.divider, thickness = 1.dp)

                    // Passkeys section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = colors.textSecondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.passkeys),
                                style = typography.bodyLarge,
                                color = colors.textPrimary
                            )
                        }
                        IconButton(onClick = { showAddPasskeyDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_passkey),
                                tint = colors.accent
                            )
                        }
                    }

                    if (uiState.isLoadingPasskeys) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else if (uiState.passkeys.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_passkeys),
                            style = typography.bodyMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 56.dp, vertical = 8.dp)
                        )
                    } else {
                        uiState.passkeys.forEach { passkey ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 56.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = passkey.name,
                                        style = typography.bodyMedium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = passkey.created,
                                        style = typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                                IconButton(onClick = { showRevokePasskeyId = passkey.id }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.remove),
                                        tint = colors.reaction
                                    )
                                }
                            }
                        }
                    }
                } // passkeyEnabled
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSignInClick)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Login,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.sign_in),
                        style = typography.bodyLarge,
                        color = colors.textPrimary
                    )
                }
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showThemeDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.settings_theme),
                        style = typography.bodyLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = stringResource(uiState.themeMode.labelRes()),
                        style = typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setShowLanguageFilter(!uiState.showLanguageFilter) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_show_language_filter),
                        style = typography.bodyLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.settings_show_language_filter_description),
                        style = typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = uiState.showLanguageFilter,
                    onCheckedChange = viewModel::setShowLanguageFilter
                )
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.clearCache() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.clear_cache),
                    style = typography.bodyLarge,
                    color = colors.textPrimary
                )
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLicensesClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.LibraryBooks,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.licenses),
                    style = typography.bodyLarge,
                    color = colors.textPrimary
                )
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.about),
                        style = typography.bodyLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${stringResource(R.string.version)} ${uiState.appVersion}",
                        style = typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }

    // Add passkey dialog
    if (passkeyEnabled && showAddPasskeyDialog) {
        var passkeyName by remember { mutableStateOf("") }
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { showAddPasskeyDialog = false },
            title = { Text(stringResource(R.string.add_passkey)) },
            text = {
                OutlinedTextField(
                    value = passkeyName,
                    onValueChange = { passkeyName = it },
                    placeholder = { Text(stringResource(R.string.passkey_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        cursorColor = colors.accent
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAddPasskeyDialog = false
                        viewModel.registerPasskey(passkeyName.ifBlank { "Android" }, context)
                    },
                    enabled = !uiState.isRegisteringPasskey
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPasskeyDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            current = uiState.themeMode,
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // Revoke passkey confirmation dialog
    val passkeyIdToRevoke = showRevokePasskeyId
    if (passkeyEnabled && passkeyIdToRevoke != null) {
        RevokePasskeyDialog(
            passkeyId = passkeyIdToRevoke,
            onConfirm = { id ->
                viewModel.revokePasskey(id)
                showRevokePasskeyId = null
            },
            onDismiss = { showRevokePasskeyId = null }
        )
    }
}

@Composable
@androidx.annotation.VisibleForTesting
internal fun RevokePasskeyDialog(
    passkeyId: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.remove_passkey)) },
        text = { Text(stringResource(R.string.remove_passkey_confirm)) },
        confirmButton = {
            TextButton(onClick = { onConfirm(passkeyId) }) {
                Text(stringResource(R.string.remove), color = colors.reaction)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Switcher button for the profile header. Opens a menu listing the stored
 * accounts other than the active one, plus an entry that starts a fresh
 * sign-in to add another account.
 */
@Composable
internal fun AccountSwitcherMenu(
    accounts: List<StoredAccount>,
    enabled: Boolean,
    onSwitchAccount: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }, enabled = enabled) {
            Icon(
                imageVector = Icons.Default.SwitchAccount,
                contentDescription = stringResource(R.string.switch_account),
                tint = colors.accent
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = account.name,
                                style = typography.bodyLarge,
                                color = colors.textPrimary
                            )
                            Text(
                                text = account.handle,
                                style = typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    },
                    leadingIcon = {
                        AsyncImage(
                            model = account.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    },
                    onClick = {
                        expanded = false
                        onSwitchAccount(account.userId)
                    }
                )
            }
            if (accounts.isNotEmpty()) {
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
            }
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.add_account),
                        style = typography.bodyLarge,
                        color = colors.textPrimary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                },
                onClick = {
                    expanded = false
                    onAddAccountClick()
                }
            )
        }
    }
}

@androidx.annotation.StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
    ThemeMode.DYNAMIC -> R.string.settings_theme_dynamic
}

@Composable
@androidx.annotation.VisibleForTesting
internal fun ThemeSelectionDialog(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme)) },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    val enabled = mode != ThemeMode.DYNAMIC || dynamicSupported
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabled) { onSelect(mode) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == mode,
                            onClick = if (enabled) ({ onSelect(mode) }) else null,
                            enabled = enabled,
                            colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(mode.labelRes()),
                                style = typography.bodyLarge,
                                color = if (enabled) colors.textPrimary else colors.textSecondary
                            )
                            if (mode == ThemeMode.DYNAMIC && !dynamicSupported) {
                                Text(
                                    text = stringResource(R.string.settings_theme_dynamic_unavailable),
                                    style = typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = colors.accent)
            }
        }
    )
}
