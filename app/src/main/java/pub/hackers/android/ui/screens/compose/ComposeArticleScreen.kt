package pub.hackers.android.ui.screens.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.CircularProgressIndicator
import pub.hackers.android.R
import pub.hackers.android.ui.components.HtmlContent
import pub.hackers.android.ui.components.HtmlContentStyle
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography

@Composable
fun ComposeArticleScreen(
    draftId: String? = null,
    articleId: String? = null,
    onSaveSuccess: () -> Unit,
    onPublishSuccess: (articleId: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ComposeArticleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    val titleFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(draftId) {
        draftId?.let { viewModel.loadDraft(it) }
    }

    LaunchedEffect(articleId) {
        articleId?.let { viewModel.loadArticleForEdit(it) }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Draft saved")
            viewModel.clearSavedFlag()
        }
    }

    LaunchedEffect(uiState.isPublished) {
        if (uiState.isPublished) {
            val articleId = uiState.publishedArticleId
            if (articleId != null) {
                onPublishSuccess(articleId)
            } else {
                onSaveSuccess()
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        if (draftId == null && articleId == null) {
            titleFocusRequester.requestFocus()
        }
    }

    val saveEnabled = uiState.title.isNotBlank() && !uiState.isSaving && !uiState.isPublishing
    val publishEnabled = uiState.title.isNotBlank() && !uiState.isSaving && !uiState.isPublishing
    val updateEnabled = uiState.title.isNotBlank() && !uiState.isLoadingArticle && !uiState.isPublishing

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel),
                        tint = colors.textPrimary
                    )
                }
                Text(
                    text = stringResource(
                        if (uiState.isEditMode) R.string.edit_article else R.string.compose_article
                    ),
                    style = typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title field
                BasicTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester),
                    enabled = !uiState.isSaving && !uiState.isPublishing,
                    textStyle = typography.titleLarge.copy(
                        color = colors.textPrimary
                    ),
                    cursorBrush = SolidColor(colors.composeAccent),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (uiState.title.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.article_title_hint),
                                    style = typography.titleLarge,
                                    color = colors.textSecondary
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tags field
                BasicTextField(
                    value = uiState.tags,
                    onValueChange = { viewModel.updateTags(it) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving && !uiState.isPublishing,
                    textStyle = typography.bodyMedium.copy(
                        color = colors.textSecondary
                    ),
                    cursorBrush = SolidColor(colors.composeAccent),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (uiState.tags.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.article_tags_hint),
                                    style = typography.bodyMedium,
                                    color = colors.textSecondary.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colors.divider)
                Spacer(modifier = Modifier.height(8.dp))

                if (!uiState.isEditMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.isLoadingPreview) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = colors.composeAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        ComposePreviewToggle(
                            label = stringResource(R.string.compose_edit),
                            selected = !uiState.isPreview,
                            enabled = !uiState.isLoadingPreview,
                            onClick = { if (uiState.isPreview) viewModel.togglePreview() }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ComposePreviewToggle(
                            label = stringResource(R.string.compose_preview),
                            selected = uiState.isPreview,
                            enabled = !uiState.isLoadingPreview,
                            onClick = { if (!uiState.isPreview) viewModel.togglePreview() }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Content field / preview
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.divider)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .then(
                                if (!uiState.isPreview) Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    contentFocusRequester.requestFocus()
                                    keyboardController?.show()
                                } else Modifier
                            )
                            .padding(16.dp)
                    ) {
                        if (uiState.isPreview) {
                            if (uiState.previewHtml.isBlank()) {
                                Text(
                                    text = stringResource(R.string.compose_preview_empty),
                                    style = typography.bodyLarge,
                                    color = colors.textSecondary
                                )
                            } else {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    HtmlContent(
                                        html = uiState.previewHtml,
                                        modifier = Modifier.fillMaxWidth(),
                                        contentStyle = HtmlContentStyle.Prose
                                    )
                                }
                            }
                        } else {
                            BasicTextField(
                                value = uiState.content,
                                onValueChange = { viewModel.updateContent(it) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .focusRequester(contentFocusRequester),
                                enabled = !uiState.isSaving && !uiState.isPublishing,
                                textStyle = typography.bodyLarge.copy(
                                    color = colors.textBody
                                ),
                                cursorBrush = SolidColor(colors.composeAccent),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (uiState.content.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.article_content_hint),
                                                style = typography.bodyLarge,
                                                color = colors.textSecondary
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Publish fields (slug, language, allow LLM translation)
            AnimatedVisibility(
                visible = uiState.showPublishFields,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    HorizontalDivider(color = colors.divider)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.publish_confirm_title),
                        style = typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.slug,
                        onValueChange = { viewModel.updateSlug(it) },
                        label = { Text(stringResource(R.string.article_slug_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !uiState.isPublishing,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.composeAccent,
                            focusedLabelColor = colors.composeAccent,
                            cursorColor = colors.composeAccent,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            unfocusedBorderColor = colors.divider,
                            unfocusedLabelColor = colors.textSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.language,
                        onValueChange = { viewModel.updateLanguage(it) },
                        label = { Text(stringResource(R.string.article_language_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !uiState.isPublishing,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.composeAccent,
                            focusedLabelColor = colors.composeAccent,
                            cursorColor = colors.composeAccent,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            unfocusedBorderColor = colors.divider,
                            unfocusedLabelColor = colors.textSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.updateAllowLlmTranslation(!uiState.allowLlmTranslation)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.allowLlmTranslation,
                            onCheckedChange = { viewModel.updateAllowLlmTranslation(it) },
                            enabled = !uiState.isPublishing,
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.composeAccent,
                                uncheckedColor = colors.textSecondary
                            )
                        )
                        Text(
                            text = stringResource(R.string.allow_llm_translation),
                            style = typography.bodyMedium,
                            color = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.hidePublishFields() },
                            enabled = !uiState.isPublishing,
                            shape = RoundedCornerShape(AppShapes.pillRadius)
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                color = colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.publishDraft() },
                            enabled = uiState.slug.isNotBlank() && !uiState.isPublishing,
                            shape = RoundedCornerShape(AppShapes.pillRadius),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.composeAccent,
                                contentColor = colors.composeOnAccent,
                                disabledContainerColor = colors.composeAccent,
                                disabledContentColor = colors.composeOnAccent,
                            ),
                            modifier = Modifier.alpha(
                                if (uiState.slug.isNotBlank() && !uiState.isPublishing) 1f else 0.4f
                            )
                        ) {
                            Text(
                                text = if (uiState.isPublishing) {
                                    stringResource(R.string.publishing_article)
                                } else {
                                    stringResource(R.string.publish_article)
                                },
                                color = colors.composeOnAccent
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Bottom bar — Update button in edit mode, Save Draft + Publish otherwise
            if (uiState.isEditMode) {
                HorizontalDivider(color = colors.divider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { viewModel.updateArticle() },
                        enabled = updateEnabled,
                        shape = RoundedCornerShape(AppShapes.pillRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.composeAccent,
                            contentColor = colors.composeOnAccent,
                            disabledContainerColor = colors.composeAccent,
                            disabledContentColor = colors.composeOnAccent,
                        ),
                        modifier = Modifier.alpha(if (updateEnabled) 1f else 0.4f)
                    ) {
                        Text(
                            text = if (uiState.isPublishing) {
                                stringResource(R.string.updating_article)
                            } else {
                                stringResource(R.string.update_article)
                            },
                            color = colors.composeOnAccent
                        )
                    }
                }
            } else if (!uiState.showPublishFields) {
                HorizontalDivider(color = colors.divider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { viewModel.saveDraft() },
                        enabled = saveEnabled,
                        shape = RoundedCornerShape(AppShapes.pillRadius),
                        border = BorderStroke(
                            1.dp,
                            if (saveEnabled) colors.composeAccent else colors.composeAccent.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.alpha(if (saveEnabled) 1f else 0.4f)
                    ) {
                        Text(
                            text = if (uiState.isSaving) {
                                stringResource(R.string.saving_draft)
                            } else {
                                stringResource(R.string.save_draft)
                            },
                            color = colors.composeAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.showPublishFields() },
                        enabled = publishEnabled,
                        shape = RoundedCornerShape(AppShapes.pillRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.composeAccent,
                            contentColor = colors.composeOnAccent,
                            disabledContainerColor = colors.composeAccent,
                            disabledContentColor = colors.composeOnAccent,
                        ),
                        modifier = Modifier.alpha(if (publishEnabled) 1f else 0.4f)
                    ) {
                        Text(
                            text = stringResource(R.string.publish_article),
                            color = colors.composeOnAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposePreviewToggle(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    Text(
        text = label,
        style = typography.labelMedium,
        color = if (selected) colors.composeAccent else colors.textSecondary,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
