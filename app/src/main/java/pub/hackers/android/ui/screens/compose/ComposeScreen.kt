package pub.hackers.android.ui.screens.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import pub.hackers.android.ui.components.CompactTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import pub.hackers.android.R
import pub.hackers.android.domain.model.Post
import pub.hackers.android.domain.model.PostVisibility
import pub.hackers.android.ui.components.HtmlContent
import pub.hackers.android.ui.components.MentionAutocomplete
import kotlin.math.roundToInt

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

    // Track TextFieldValue for cursor position
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = uiState.content))
    }

    // Track cursor and text field positions for popup placement
    var cursorRect by remember { mutableStateOf(Rect.Zero) }
    var textFieldBounds by remember { mutableStateOf(Rect.Zero) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val popupHeight = with(density) { 200.dp.toPx() } // Estimated popup height

    // Sync TextFieldValue with ViewModel state changes (e.g., when mention is selected)
    LaunchedEffect(uiState.content, uiState.cursorPosition) {
        if (textFieldValue.text != uiState.content) {
            textFieldValue = TextFieldValue(
                text = uiState.content,
                selection = TextRange(uiState.cursorPosition)
            )
        }
    }

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
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CompactTopBar(
                title = if (replyToId != null) stringResource(R.string.reply) else stringResource(R.string.compose),
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
            // Reply target preview
            if (uiState.isLoadingReplyTarget) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else if (uiState.replyTargetPost != null) {
                ReplyTargetPreview(
                    post = uiState.replyTargetPost!!,
                    modifier = Modifier.alpha(0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Box(modifier = Modifier.weight(1f)) {
                // Custom text field with cursor position tracking
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            textFieldBounds = coordinates.boundsInWindow()
                        },
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(scrollState)
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newValue: TextFieldValue ->
                                textFieldValue = newValue
                                viewModel.updateContent(
                                    content = newValue.text,
                                    cursorPosition = newValue.selection.start
                                )
                            },
                            onTextLayout = { result: TextLayoutResult ->
                                textLayoutResult = result
                                // Update cursor position
                                val cursorPos = textFieldValue.selection.start
                                    .coerceIn(0, textFieldValue.text.length)
                                cursorRect = if (textFieldValue.text.isNotEmpty() || cursorPos == 0) {
                                    result.getCursorRect(cursorPos)
                                } else {
                                    Rect.Zero
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isPosting,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (textFieldValue.text.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.compose_hint),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }

                // Mention autocomplete popup
                if (uiState.mentionSuggestions.isNotEmpty() || uiState.isLoadingMentions) {
                    // Calculate cursor position accounting for padding and scroll
                    val paddingPx = with(density) { 16.dp.toPx() }
                    val cursorYInBox = cursorRect.bottom - scrollState.value + paddingPx
                    val cursorXInBox = cursorRect.left + paddingPx

                    // Clamp to visible area
                    val visibleCursorY = cursorYInBox.coerceIn(paddingPx, textFieldBounds.height - paddingPx)

                    Popup(
                        alignment = Alignment.TopStart,
                        offset = IntOffset(
                            x = cursorXInBox.roundToInt().coerceIn(0, (textFieldBounds.width - with(density) { 280.dp.toPx() }).toInt().coerceAtLeast(0)),
                            y = (visibleCursorY + with(density) { 20.dp.toPx() }).roundToInt()
                        ),
                        properties = PopupProperties(focusable = false)
                    ) {
                        MentionAutocomplete(
                            suggestions = uiState.mentionSuggestions,
                            isLoading = uiState.isLoadingMentions,
                            onSuggestionSelected = { actor ->
                                val (newContent, newCursor) = viewModel.selectMention(actor)
                                textFieldValue = TextFieldValue(
                                    text = newContent,
                                    selection = TextRange(newCursor)
                                )
                            },
                            modifier = Modifier.width(280.dp)
                        )
                    }
                }
            }

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

@Composable
private fun ReplyTargetPreview(
    post: Post,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = post.actor.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.actor.name ?: post.actor.handle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                HtmlContent(
                    html = post.content,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
