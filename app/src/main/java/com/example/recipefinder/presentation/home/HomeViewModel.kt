package com.example.recipefinder.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipefinder.domain.connectivity.ConnectivityObserver
import com.example.recipefinder.domain.model.Category
import com.example.recipefinder.domain.model.RefreshResult
import com.example.recipefinder.domain.model.SyncStatus
import com.example.recipefinder.domain.repository.RecipeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    /**
     * The text in the search field. It is Compose state (not a StateFlow) so the TextField is
     * updated synchronously while typing; the debounced copy in [queryInput] drives the search.
     */
    var searchQuery by mutableStateOf(savedStateHandle.get<String>(KEY_QUERY).orEmpty())
        private set

    private val queryInput = MutableStateFlow(searchQuery)
    private val selectedCategory = savedStateHandle.getStateFlow<String?>(KEY_CATEGORY, null)
    private val retryCount = MutableStateFlow(0)
    private val categoriesStatus = MutableStateFlow<SyncStatus>(SyncStatus.Syncing)
    private var categoriesJob: Job? = null

    private val isOnline: StateFlow<Boolean> =
        connectivityObserver.isOnline.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** null until Room has answered the first time, so "not loaded" ≠ "loaded but empty". */
    private val categories: StateFlow<List<Category>?> =
        recipeRepository.observeCategories().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Search vs. category browsing, derived from user input:
     *  - `debounce` waits until typing pauses before searching (clearing the field is immediate);
     *  - `distinctUntilChanged` skips repeated identical queries (e.g. a trailing space).
     */
    private val listingRequest: Flow<ListingRequest> = combine(
        queryInput
            .map { it.trim() }
            .debounce { query -> if (query.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }
            .distinctUntilChanged(),
        selectedCategory,
    ) { query, category ->
        when {
            query.isNotEmpty() -> ListingRequest.Search(query)
            category != null -> ListingRequest.Browse(category)
            else -> ListingRequest.None
        }
    }.distinctUntilChanged()

    /**
     * `flatMapLatest` cancels the previous request's Flow — including its in-flight network call —
     * as soon as a new query/category arrives, so results from an older search can never
     * overwrite newer ones. Bumping [retryCount] re-runs the current request.
     */
    private val listing: Flow<Pair<ListingRequest, HomeContent>> =
        combine(listingRequest, retryCount) { request, _ -> request }
            .flatMapLatest { request ->
                contentFor(request).map { content -> request to content }
            }

    // Eagerly: keep the list subscribed while the user is on another tab or the detail screen,
    // so coming back doesn't trigger a new network refresh every time.
    val uiState: StateFlow<HomeUiState> = combine(
        categories,
        selectedCategory,
        isOnline,
        listing,
        categoriesStatus,
    ) { categories, selected, online, (request, content), categoryStatus ->
        HomeUiState(
            categories = categories.orEmpty(),
            categoriesLoading = categoryStatus is SyncStatus.Syncing,
            selectedCategory = selected,
            isOffline = !online,
            activeRequest = request,
            content = resolveContent(request, content, categories, categoryStatus),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    init {
        refreshCategories()
        selectDefaultCategoryWhenAvailable()
        retryWhenConnectionReturns()
    }

    fun onQueryChange(query: String) {
        searchQuery = query
        savedStateHandle[KEY_QUERY] = query
        queryInput.value = query
    }

    fun onClearQuery() = onQueryChange("")

    /** Picking a category leaves search mode and shows that category's recipes. */
    fun onCategorySelected(category: String) {
        if (searchQuery.isNotEmpty()) onQueryChange("")
        savedStateHandle[KEY_CATEGORY] = category
    }

    /** Re-runs the current request (pull-to-refresh and "Try again" buttons). */
    fun retry() {
        if (categories.value.isNullOrEmpty() || categoriesStatus.value is SyncStatus.Failed) {
            refreshCategories()
        }
        retryCount.update { it + 1 }
    }

    private fun contentFor(request: ListingRequest): Flow<HomeContent> = when (request) {
        is ListingRequest.Search -> recipeRepository.searchRecipes(request.query)
            .map { it.toHomeContent(request) }
        is ListingRequest.Browse -> recipeRepository.recipesInCategory(request.category)
            .map { it.toHomeContent(request) }
        ListingRequest.None -> flowOf(HomeContent.Loading)
    }

    private fun resolveContent(
        request: ListingRequest,
        content: HomeContent,
        categories: List<Category>?,
        categoryStatus: SyncStatus,
    ): HomeContent {
        // Without a category there is nothing to list; explain why if loading them failed.
        if (request == ListingRequest.None && categories?.isEmpty() == true && categoryStatus is SyncStatus.Failed) {
            return HomeContent.NoCategories(categoryStatus.error)
        }
        return content
    }

    private fun refreshCategories() {
        if (categoriesJob?.isActive == true) return
        categoriesJob = viewModelScope.launch {
            categoriesStatus.value = SyncStatus.Syncing
            categoriesStatus.value = when (val result = recipeRepository.refreshCategories()) {
                RefreshResult.Success -> SyncStatus.Synced
                is RefreshResult.Failure -> SyncStatus.Failed(result.error)
            }
        }
    }

    /** First launch: once categories exist, show "Chicken" (or the first category). */
    private fun selectDefaultCategoryWhenAvailable() {
        viewModelScope.launch {
            val available = categories.filterNotNull().first { it.isNotEmpty() }
            if (selectedCategory.value == null) {
                savedStateHandle[KEY_CATEGORY] = defaultCategory(available)
            }
        }
    }

    private fun retryWhenConnectionReturns() {
        viewModelScope.launch {
            var wasOnline = isOnline.value
            isOnline.collect { online ->
                if (online && !wasOnline && uiState.value.content.wouldBenefitFromRetry) retry()
                wasOnline = online
            }
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 450L
        const val DEFAULT_CATEGORY = "Chicken"
        private const val KEY_QUERY = "query"
        private const val KEY_CATEGORY = "category"

        fun defaultCategory(categories: List<Category>): String =
            categories.firstOrNull { it.name.equals(DEFAULT_CATEGORY, ignoreCase = true) }?.name
                ?: categories.first().name
    }
}
