package com.t3codes.mobile

import kotlinx.serialization.json.*

/** Keys whose project override may legitimately store an explicit null (unset = inherit). */
private val NULLABLE_PROJECT_OVERRIDE_KEYS = setOf(
    "defaultModelSelection",
    "sourceControlWriterModelSelection",
    "reviewGuideModelSelection",
    "pullRequestMergeMethod",
    "sidebarAutoSettleAfterDays",
)

/** Keys that may be overridden per project (mirrors PROJECT_SCOPED_SERVER_SETTING_KEYS). */
val PROJECT_SCOPED_SETTING_KEYS = setOf(
    "worktreeCleanup",
    "defaultModelSelection",
    "defaultRuntimeMode",
    "defaultThreadEnvMode",
    "newWorktreesStartFromOrigin",
    "worktreeSubmodules",
    "defaultAutoPull",
    "defaultProjectScripts",
    "enableAgentBrowserAccess",
    "enableAgentDeviceAccess",
    "textGenerationModelSelection",
    "threadTitleInstructions",
    "sourceControlWriterModelSelection",
    "reviewGuideModelSelection",
    "reviewGuideInstructions",
    "sourceControlWritingStyle",
    "removeAgentCreditsOnMerge",
    "branchNamingMode",
    "branchNamePrefix",
    "branchNameInstructions",
    "pullRequestMergeMethod",
    "sidebarAutoSettleOnMerge",
    "sidebarAutoSettleAfterDays",
    "sidebarAutoSettleKeepPullRequests",
    "continueThreadsAfterServerUpdate",
    "responseStreamingMode",
)

/**
 * Read and write access to the server's Settings object.
 *
 * The server replaces `projectSettingsOverrides` wholesale, so project-scoped writes always
 * round-trip the full record: read fresh, modify one project's entry, write the whole thing.
 * Device-local or global-scope keys are sent as a flat patch.
 */
class ServerSettingsStore(private val connection: com.t3codes.core.T3Connection) {

    fun get(): JsonObject = connection.getSettings()

    /** Patch a machine-wide setting. Returns the full updated settings object. */
    fun setMachine(key: String, value: JsonElement?): JsonObject {
        val patch = buildJsonObject {
            if (value == null) put(key, JsonNull) else put(key, value)
        }
        return connection.updateSettings(patch)
    }

    /** Patch a nullable machine-wide setting; null stores an explicit null. */
    fun setMachineNullable(key: String, value: JsonElement?): JsonObject = setMachine(key, value)

    /** Set or clear one project override key. Returns the full updated settings object. */
    fun setProjectOverride(
        projectId: String,
        key: String,
        value: JsonElement?,
    ): JsonObject {
        val current = get()
        val overrides = current["projectSettingsOverrides"] as? JsonObject ?: JsonObject(emptyMap())
        val target = overrides[projectId] as? JsonObject ?: JsonObject(emptyMap())
        val next = buildJsonObject {
            target.forEach { (k, v) -> if (k != key) put(k, v) }
            if (value != null) {
                put(key, value)
            } else if (key in NULLABLE_PROJECT_OVERRIDE_KEYS) {
                put(key, JsonNull)
            }
            // Otherwise a null value clears the override (delete the key).
        }
        val nextOverrides = buildJsonObject {
            overrides.forEach { (id, entry) -> if (id != projectId) put(id, entry) }
            // Keep the project entry even if it became empty; harmless and matches the server's
            // whole-record semantics. Drop it only when the user explicitly clears everything.
            put(projectId, next)
        }
        return connection.updateSettings(buildJsonObject { put("projectSettingsOverrides", nextOverrides) })
    }

    /** Remove every override for [projectId] from the given keys. */
    fun clearProjectOverrides(projectId: String, keys: List<String>): JsonObject {
        val current = get()
        val overrides = current["projectSettingsOverrides"] as? JsonObject ?: JsonObject(emptyMap())
        val target = overrides[projectId] as? JsonObject ?: JsonObject(emptyMap())
        val next = buildJsonObject {
            target.forEach { (k, v) -> if (k !in keys) put(k, v) }
        }
        val nextOverrides = buildJsonObject {
            overrides.forEach { (id, entry) -> if (id != projectId) put(id, entry) }
            if (next.isNotEmpty()) put(projectId, next)
        }
        return connection.updateSettings(buildJsonObject { put("projectSettingsOverrides", nextOverrides) })
    }
}

/** Managed-policy helpers over the raw `managedPolicy` key on ServerConfig. */
object ManagedPolicy {
    fun lockedPaths(config: com.t3codes.core.ServerConfig?): List<String> =
        ((config?.managedPolicy as? JsonObject)?.get("lockedSettings") as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.content }
            .orEmpty()

    fun organization(config: com.t3codes.core.ServerConfig?): String? =
        ((config?.managedPolicy as? JsonObject)?.get("organization") as? JsonPrimitive)?.content

    /** True when `path` is locked, sits inside a locked setting, or contains one. */
    fun isLocked(config: com.t3codes.core.ServerConfig?, path: String): Boolean =
        lockedPaths(config).any { it == path || it.startsWith("$path.") || path.startsWith("$it.") }

    /** "Managed by <org>." suffix for rows when locked, null otherwise. */
    fun notice(config: com.t3codes.core.ServerConfig?, path: String): String? =
        organization(config)?.takeIf { isLocked(config, path) }?.let { "Managed by $it." }
}

/** Keys that are not project-scoped: disabled while a project scope is selected. */
val MACHINE_ONLY_KEYS = setOf(
    "enableProviderUpdateChecks",
    "observability",
)

/** Defensive accessors over the raw settings object. Absent and explicit null both read as null. */
class ServerSettingsView(val raw: JsonObject?) {
    private fun element(key: String): JsonElement? = raw?.get(key)?.takeUnless { it is JsonNull }

    fun string(key: String): String? =
        (element(key) as? JsonPrimitive)?.content

    fun bool(key: String): Boolean? =
        (element(key) as? JsonPrimitive)?.booleanOrNull

    fun int(key: String): Int? =
        (element(key) as? JsonPrimitive)?.intOrNull

    /** Per-project override value for [key], or null (absent *or* explicitly null). */
    fun override(projectId: String, key: String): JsonElement? =
        ((raw?.get("projectSettingsOverrides") as? JsonObject)
            ?.get(projectId) as? JsonObject)?.get(key)?.takeUnless { it is JsonNull }

    fun overrideOrNullStored(projectId: String, key: String): Triple<Boolean, JsonElement?, Boolean> {
        // (hasOverride, value, storedExplicitNull)
        val entry = ((raw?.get("projectSettingsOverrides") as? JsonObject)
            ?.get(projectId) as? JsonObject)
        if (entry == null) return Triple(false, null, false)
        val v = entry[key]
        return when {
            v == null -> Triple(false, null, false)
            v is JsonNull -> Triple(true, null, true)
            else -> Triple(true, v, false)
        }
    }

    fun hasOverride(projectId: String, key: String): Boolean = overrideOrNullStored(projectId, key).first

    /** Effective bool for scope: project override when set, else machine value, else default. */
    fun effectiveBool(projectId: String?, key: String, default: Boolean): Boolean {
        if (projectId != null) {
            val (has, value) = overrideOrNullStored(projectId, key)
            if (has) return (value as? JsonPrimitive)?.booleanOrNull ?: default
        }
        return bool(key) ?: default
    }

    /** Effective string for scope: project override when set (explicit null counts as unset), else machine value. */
    fun effectiveString(projectId: String?, key: String, default: String? = null): String? {
        if (projectId != null) {
            val (has, value) = overrideOrNullStored(projectId, key)
            if (has) return (value as? JsonPrimitive)?.content ?: default
        }
        return string(key) ?: default
    }

    fun effectiveIntNullable(projectId: String?, key: String, default: Int? = null): Int? {
        if (projectId != null) {
            val (has, value, explicitNull) = overrideOrNullStored(projectId, key)
            if (has) return (value as? JsonPrimitive)?.intOrNull
            if (explicitNull) return null
        }
        return int(key) ?: default
    }
}
