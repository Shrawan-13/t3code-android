package com.t3codes.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** One row from `pullRequests.list`. Unknown keys are ignored by the shared codec. */
@Serializable
data class PullRequestActor(
    val login: String = "",
    val name: String? = null,
    val avatarUrl: String? = null,
    val isBot: Boolean = false,
)

@Serializable
data class PullRequestLabel(
    val name: String = "",
    val color: String? = null,
)

@Serializable
data class PullRequestEntry(
    val provider: String = "",
    val host: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val repository: String = "",
    val number: Int = 0,
    val title: String = "",
    val url: String = "",
    val author: PullRequestActor? = null,
    val headBranch: String = "",
    val baseBranch: String = "",
    val state: String = "",
    val isDraft: Boolean = false,
    val mergeability: String = "",
    val additions: Int = 0,
    val deletions: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
    val viewerReviewRequested: Boolean = false,
    val labels: List<PullRequestLabel> = emptyList(),
    val reviewDecision: String? = null,
    val checksState: String? = null,
    val stack: JsonElement? = null,
)

@Serializable
data class PullRequestProjectError(
    val projectId: String = "",
    val projectTitle: String = "",
    val message: String = "",
)

@Serializable
data class PullRequestProviderSummary(
    val host: String = "",
    val kind: String = "",
    val configured: Boolean = false,
    val projectCount: Int = 0,
    val detail: String? = null,
)

@Serializable
data class PullRequestListResult(
    val entries: List<PullRequestEntry> = emptyList(),
    val errors: List<PullRequestProjectError> = emptyList(),
    val providers: List<PullRequestProviderSummary> = emptyList(),
    val viewers: Map<String, String> = emptyMap(),
    val truncated: Boolean = false,
    val nextCursors: Map<String, String> = emptyMap(),
)
