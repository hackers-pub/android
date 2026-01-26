package pub.hackers.android.ui.screens.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pub.hackers.android.R
import pub.hackers.android.domain.model.PostVisibility

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    replyToId: String?,
    onPostSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ComposeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showVisibilityMenu by remember { mutableStateOf(false) }

    LaunchedEffect(replyToId) {
        replyToId?.let { viewModel.setReplyTarget(it) }
    }

    LaunchedEffect(uiState.isPosted) {
        if (uiState.isPosted) {
            onPostSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (replyToId != null) stringResource(R.string.reply)
                        else stringResource(R.string.compose)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.post() },
                        enabled = uiState.content.isNotBlank() && !uiState.isPosting
                    ) {
                        Text(stringResource(R.string.post))
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
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.content,
                onValueChange = viewModel::updateContent,
                placeholder = { Text(stringResource(R.string.compose_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                enabled = !uiState.isPosting
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { showVisibilityMenu = true }
                ) {
                    Icon(
                        imageVector = when (uiState.visibility) {
                            PostVisibility.PUBLIC -> Icons.Filled.Public
                            PostVisibility.UNLISTED -> Icons.Outlined.Lock
                            PostVisibility.FOLLOWERS -> Icons.Outlined.Group
                            else -> Icons.Filled.Public
                        },
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        when (uiState.visibility) {
                            PostVisibility.PUBLIC -> stringResource(R.string.visibility_public)
                            PostVisibility.UNLISTED -> stringResource(R.string.visibility_unlisted)
                            PostVisibility.FOLLOWERS -> stringResource(R.string.visibility_followers)
                            else -> stringResource(R.string.visibility_public)
                        }
                    )

                    DropdownMenu(
                        expanded = showVisibilityMenu,
                        onDismissRequest = { showVisibilityMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.visibility_public)) },
                            onClick = {
                                viewModel.updateVisibility(PostVisibility.PUBLIC)
                                showVisibilityMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Public, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.visibility_unlisted)) },
                            onClick = {
                                viewModel.updateVisibility(PostVisibility.UNLISTED)
                                showVisibilityMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.visibility_followers)) },
                            onClick = {
                                viewModel.updateVisibility(PostVisibility.FOLLOWERS)
                                showVisibilityMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Group, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
    }
}
