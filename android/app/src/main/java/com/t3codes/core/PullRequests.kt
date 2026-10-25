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

@Serializable
data class PullRequestCapabilities(
    val diff: Boolean = false,
    val comment: Boolean = false,
    val actions: List<String> = emptyList(),
    val mergeMethods: List<String> = emptyList(),
    val deleteBranch: Boolean? = null,
    val updateMethods: List<String>? = null,
    val search: Boolean = false,
    val review: JsonElement? = null,
    val reviewers: JsonElement? = null,
    val edit: JsonElement? = null,
    val stacks: Boolean? = null,
    val stackActions: Boolean? = null,
    val labels: Boolean? = null,
)

@Serializable
data class PullRequestViewerPermissions(
    val actions: List<String> = emptyList(),
    val comment: Boolean = false,
    val resolve: Boolean = false,
    val verdicts: List<String> = emptyList(),
    val requestReviewers: Boolean = false,
    val updateMethods: List<String>? = null,
    val labels: Boolean? = null,
    val editChangeRequest: Boolean? = null,
)

@Serializable
data class PullRequestMergeCapabilities(
    val merge: Boolean = false,
    val squash: Boolean = false,
    val rebase: Boolean = false,
)

@Serializable
data class PullRequestCheck(
    val name: String = "",
    val status: String = "",
    val description: String? = null,
    val url: String? = null,
    val required: Boolean? = null,
)

@Serializable
data class PullRequestDetail(
    val provider: String = "",
    val capabilities: PullRequestCapabilities = PullRequestCapabilities(),
    val viewerPermissions: PullRequestViewerPermissions = PullRequestViewerPermissions(),
    val projectId: String = "",
    val projectTitle: String = "",
    val workspaceRoot: String = "",
    val repository: String = "",
    val number: Int = 0,
    val title: String = "",
    val body: String = "",
    val url: String = "",
    val author: PullRequestActor? = null,
    val state: String = "",
    val isDraft: Boolean = false,
    val mergeability: String = "",
    val additions: Int = 0,
    val deletions: Int = 0,
    val changedFiles: Int = 0,
    val headBranch: String = "",
    val headRepositoryNameWithOwner: String? = null,
    val headSha: String? = null,
    val baseBranch: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val mergedAt: String? = null,
    val closedAt: String? = null,
    val reviewers: List<PullRequestActor> = emptyList(),
    val labels: List<PullRequestLabel> = emptyList(),
    val checks: List<PullRequestCheck> = emptyList(),
    val mergeCapabilities: PullRequestMergeCapabilities = PullRequestMergeCapabilities(),
    val viewer: String? = null,
    val baseComparison: String? = null,
    val behindBy: Int? = null,
    val autoMergeEnabled: Boolean? = null,
    val autoMergeMethod: String? = null,
    val workflowApprovalsRequired: Int? = null,
)

@Serializable
data class PullRequestOmittedFileStat(
    val path: String = "",
    val additions: Int = 0,
    val deletions: Int = 0,
)

@Serializable
data class PullRequestDiffResult(
    val patch: String = "",
    val truncated: Boolean = false,
    val nextCursor: String? = null,
    val omittedFileStats: List<PullRequestOmittedFileStat> = emptyList(),
)
