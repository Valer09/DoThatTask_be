package homeaq.dothattask.data.service

import homeaq.dothattask.Model.TaskCategory
import homeaq.dothattask.data.DataResponse
import homeaq.dothattask.data.repository.CategoryRepository

class CategoryService(private val categories: CategoryRepository) {

    /**
     * Same palette pattern groups use, applied here when a brand-new
     * category is created (no override color provided by the client).
     */
    private val palette = listOf(
        "#7E57C2", "#26A69A", "#EF5350", "#42A5F5",
        "#FFA726", "#66BB6A", "#5C6BC0", "#EC407A",
    )

    suspend fun listForGroup(groupId: Int): DataResponse<List<TaskCategory>> =
        DataResponse.success(categories.categoriesForGroup(groupId))

    /**
     * Add a category to a group:
     *   1. If a category with the same (case-insensitive) name already
     *      exists, only the group↔category link is created.
     *   2. Otherwise the category is inserted into the global table first,
     *      then linked to this group.
     *
     * The stored name is normalized: "First letter uppercase, rest lowercase".
     */
    suspend fun addToGroup(groupId: Int, rawName: String, color: String?): DataResponse<TaskCategory> {
        val normalized = TaskCategory.normalizeName(rawName)
        if (normalized.isEmpty()) return DataResponse.validationError("Category name cannot be empty")

        val existing = categories.byNameInsensitive(normalized)
        val category = existing ?: run {
            val pickedColor = color?.takeIf { it.isNotBlank() }
                ?: palette.random()
            categories.createCategory(normalized, pickedColor)
        }

        // Link (idempotent). If already linked we still echo back success so
        // the UI can refresh its list without throwing an error.
        categories.linkToGroup(groupId, category.id)
        return DataResponse.success(category, if (existing != null) "Linked existing category" else "Created and linked")
    }

    /**
     * Remove a category from a group's relationship table. The category
     * itself is preserved in the global table — other groups may still use
     * it. Tasks already assigned to this category in this group are kept;
     * they just won't validate against the group's category list anymore,
     * so the UI is expected to migrate them before unlinking. The check
     * below blocks unlinking while tasks still reference the category.
     */
    suspend fun removeFromGroup(groupId: Int, categoryId: Int): DataResponse<Boolean> {
        val tasksUsingIt = categories.tasksInGroupWithCategory(groupId, categoryId)
        if (tasksUsingIt > 0) {
            return DataResponse.forbidden(
                "Cannot remove: $tasksUsingIt task(s) in this group still use this category"
            )
        }
        val removed = categories.unlinkFromGroup(groupId, categoryId)
        return if (removed) DataResponse.success(true, "Unlinked from group")
        else DataResponse.notFound("Category was not linked to this group")
    }

    /** Hook for GroupService to wire up defaults when a new group is created. */
    suspend fun linkDefaultsToGroup(groupId: Int) {
        categories.defaultCategoryIds().forEach { id ->
            categories.linkToGroup(groupId, id)
        }
    }
}
