package pub.hackers.android.ui.screens.compose

import android.content.Context
import android.os.Build
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextLanguage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pub.hackers.android.data.repository.HackersPubRepository
import javax.inject.Inject

data class ComposeArticleUiState(
    val title: String = "",
    val content: String = "",
    val tags: String = "",
    val draftId: String? = null,
    val articleId: String? = null,
    val isEditMode: Boolean = false,
    val isLoadingArticle: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val slug: String = "",
    val language: String = java.util.Locale.getDefault().language,
    val allowLlmTranslation: Boolean = true,
    val showPublishFields: Boolean = false,
    val isPublishing: Boolean = false,
    val isPublished: Boolean = false,
    val publishedArticleId: String? = null,
    val publishedArticleUrl: String? = null,
    val isPreview: Boolean = false,
    val previewHtml: String = "",
    val previewContent: String? = null,
    val isLoadingPreview: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ComposeArticleViewModel @Inject constructor(
    private val repository: HackersPubRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComposeArticleUiState())
    val uiState: StateFlow<ComposeArticleUiState> = _uiState.asStateFlow()

    fun loadDraft(draftId: String) {
        viewModelScope.launch {
            repository.getArticleDraft(draftId)
                .onSuccess { draft ->
                    _uiState.update {
                        it.copy(
                            title = draft.title,
                            content = draft.content,
                            tags = draft.tags.joinToString(", "),
                            draftId = draft.id,
                            slug = generateSlug(draft.title),
                            previewHtml = draft.contentHtml,
                            previewContent = draft.content
                        )
                    }
                    detectLanguage(draft.content)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun loadArticleForEdit(articleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingArticle = true, isEditMode = true, articleId = articleId) }
            repository.getEditableArticle(articleId)
                .onSuccess { article ->
                    _uiState.update {
                        it.copy(
                            title = article.title,
                            content = article.content,
                            tags = article.tags.joinToString(", "),
                            language = article.language.ifBlank { it.language },
                            allowLlmTranslation = article.allowLlmTranslation,
                            isLoadingArticle = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message, isLoadingArticle = false)
                    }
                }
        }
    }

    fun updateArticle() {
        val state = _uiState.value
        val articleId = state.articleId ?: return
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Title is required") }
            return
        }
        if (state.isPublishing) return

        val tagsList = state.tags
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        viewModelScope.launch {
            _uiState.update { it.copy(isPublishing = true, error = null) }

            repository.updateArticle(
                articleId = articleId,
                title = state.title,
                content = state.content,
                tags = tagsList,
                language = state.language,
                allowLlmTranslation = state.allowLlmTranslation
            )
                .onSuccess { article ->
                    _uiState.update {
                        it.copy(
                            isPublishing = false,
                            isPublished = true,
                            publishedArticleId = article.id,
                            publishedArticleUrl = article.url
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message, isPublishing = false)
                    }
                }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update {
            it.copy(
                title = title,
                slug = generateSlug(title)
            )
        }
    }

    fun updateContent(content: String) {
        _uiState.update { it.copy(content = content) }
        detectLanguage(content)
    }

    fun updateTags(tags: String) {
        _uiState.update { it.copy(tags = tags) }
    }

    fun updateSlug(slug: String) {
        _uiState.update { it.copy(slug = slug) }
    }

    fun updateLanguage(language: String) {
        _uiState.update { it.copy(language = language) }
    }

    fun updateAllowLlmTranslation(allow: Boolean) {
        _uiState.update { it.copy(allowLlmTranslation = allow) }
    }

    fun togglePreview() {
        val state = _uiState.value
        if (state.isPreview) {
            _uiState.update { it.copy(isPreview = false) }
            return
        }
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Title is required") }
            return
        }
        if (state.content == state.previewContent && state.previewHtml.isNotEmpty()) {
            _uiState.update { it.copy(isPreview = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPreview = true, error = null) }
            val tagsList = state.tags
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            repository.saveArticleDraft(
                title = state.title,
                content = state.content,
                tags = tagsList,
                id = state.draftId
            )
                .onSuccess { draft ->
                    _uiState.update {
                        it.copy(
                            draftId = draft.id,
                            previewHtml = draft.contentHtml,
                            previewContent = draft.content,
                            isPreview = true,
                            isLoadingPreview = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(error = error.message, isLoadingPreview = false)
                    }
                }
        }
    }

    fun saveDraft() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Title is required") }
            return
        }
        if (state.isSaving) return

        val tagsList = state.tags
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, isSaved = false) }

            repository.saveArticleDraft(
                title = state.title,
                content = state.content,
                tags = tagsList,
                id = state.draftId
            )
                .onSuccess { draft ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isSaved = true,
                            draftId = draft.id,
                            previewHtml = draft.contentHtml,
                            previewContent = draft.content
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message,
                            isSaving = false
                        )
                    }
                }
        }
    }

    fun showPublishFields() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Title is required") }
            return
        }
        _uiState.update { it.copy(showPublishFields = true) }
    }

    fun hidePublishFields() {
        _uiState.update { it.copy(showPublishFields = false) }
    }

    fun publishDraft() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Title is required") }
            return
        }
        if (state.slug.isBlank()) {
            _uiState.update { it.copy(error = "Slug is required") }
            return
        }
        if (state.isPublishing) return

        viewModelScope.launch {
            // Save draft first if not yet saved
            if (state.draftId == null) {
                _uiState.update { it.copy(isSaving = true, error = null) }

                val tagsList = state.tags
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                val saveResult = repository.saveArticleDraft(
                    title = state.title,
                    content = state.content,
                    tags = tagsList
                )

                saveResult.onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isSaving = false) }
                    return@launch
                }

                saveResult.onSuccess { draft ->
                    _uiState.update { it.copy(draftId = draft.id, isSaving = false) }
                }
            }

            val draftId = _uiState.value.draftId ?: return@launch

            _uiState.update { it.copy(isPublishing = true, error = null) }

            repository.publishArticleDraft(
                id = draftId,
                slug = state.slug,
                language = state.language,
                allowLlmTranslation = state.allowLlmTranslation
            )
                .onSuccess { article ->
                    _uiState.update {
                        it.copy(
                            isPublishing = false,
                            isPublished = true,
                            publishedArticleId = article.id,
                            publishedArticleUrl = article.url
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message,
                            isPublishing = false
                        )
                    }
                }
        }
    }

    private fun detectLanguage(text: String) {
        if (text.isBlank() || text.length < 20) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val tcm = context.getSystemService(Context.TEXT_CLASSIFICATION_SERVICE) as? TextClassificationManager
                val classifier = tcm?.textClassifier ?: return
                val request = TextLanguage.Request.Builder(text).build()
                val result = classifier.detectLanguage(request)
                if (result.localeHypothesisCount > 0) {
                    val topLocale = result.getLocale(0)
                    val lang = topLocale.language
                    if (lang.isNotEmpty()) {
                        _uiState.update { it.copy(language = lang) }
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback: keep current language
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSavedFlag() {
        _uiState.update { it.copy(isSaved = false) }
    }

    companion object {
        fun generateSlug(title: String): String {
            return title
                .lowercase()
                .replace(Regex("[^\\p{L}\\p{N}\\s-]"), "")
                .replace(Regex("\\s+"), "-")
                .replace(Regex("-+"), "-")
                .trim('-')
                .take(128)
        }
    }
}
