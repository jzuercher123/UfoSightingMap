package com.ufomap.ufosightingmap.data

/**
 * Data class representing the current filter and search state
 */
data class FilterState(
    val shape: String? = null,
    val city: String? = null,
    val country: String? = null,
    val state: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val searchText: String? = null,
    val isFilterApplied: Boolean = false
) {
    /**
     * Returns true if any filter is actively applied
     */
    fun hasActiveFilters(): Boolean {
        return shape != null || city != null || country != null ||
                state != null || startDate != null || endDate != null ||
                searchText != null
    }
}