package com.t3codes.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Platform(val os: String = "", val arch: String = "")

@Serializable
data class Capabilities(
    val repositoryIdentity: Boolean = false,
    val connectionProbe: Boolean = false,
    val attachmentUploads: Boolean = false,
    val questionAttachments: Boolean = false,
    val pullRequests: Boolean = false,
    val pullRequestChecks: Boolean = false,
    val pullRequestStackActions: Boolean = false,
    val inlineMessageContext: Boolean = false,
    val requiredWorktreeBootstrap: Boolean = false,
    val threadSettlement: Boolean = false,
    val threadAutoSettlement: Boolean = false,
    val threadSnooze: Boolean = false,
    val threadPinning: Boolean = false,
    val threadPinReorder: Boolean = false,
    val threadActiveReorder: Boolean = false,
    val threadAutoSettleOptOut: Boolean = false,
    val threadTitleRegeneration: Boolean = false,
    val threadVisitedTracking: Boolean = false,
    val threadPullRequestLinking: Boolean = false,
    val threadPullRequests: Boolean = false,
    val threadPullRequestWatch: Boolean = false,
    val threadRestartContinuation: Boolean = false,
    val storageCleanup: Boolean = false,
    val storageCleanupRun: Boolean = false,
    val projectWorktreeCleanup: Boolean = false,
    val settledWorktreeCleanup: Boolean = false,
    val worktreesDirectory: Boolean = false,
    val environmentThemes: Boolean = false,
    val environmentIcon: Boolean = false,
    val usageLimitSources: Boolean = false,
    val usagePriceOverrides: Boolean = false,
    val usageModelAliases: Boolean = false,
    val ignoredFileListing: Boolean = false,
    val commandOutputStreaming: Boolean = false,
    val projectCloneTracking: Boolean = false,
    val projectSettingsOverrides: Boolean = false,
    @SerialName("projectProviderInstanceOverrides")
    val projectProviderOverrides: Boolean = false,
    val serverBrowser: Boolean = false,
    val serverSelfUpdateProgress: Boolean = false,
    val desktopAppUpdate: Boolean = false,
    val agentActivityPublishing: Boolean = false,
)

@Serializable
data class EnvironmentDescriptor(
    val environmentId: String = "",
    val label: String = "",
    val platform: Platform = Platform(),
    val serverVersion: String = "",
    val orchestrationProtocolVersion: Int = 0,
    val capabilities: Capabilities = Capabilities(),
)

@Serializable
data class ModelOption(val id: String = "", val value: JsonElement? = null)

@Serializable
data class ModelSelection(
    val instanceId: String = "",
    val model: String = "",
    val options: List<ModelOption> = emptyList(),
)

@Serializable
data class Model(
    val id: String = "",
    val slug: String = "",
    val name: String = "",
    val displayName: String = "",
    val description: String = "",
    val contextWindow: Int? = null,
    val isCustom: Boolean = false,
    val capabilities: JsonElement? = null,
) {
    val key: String
        get() = when {
            slug.isNotEmpty() -> slug
            id.isNotEmpty() -> id
            else -> name
        }

    val label: String
        get() = when {
            displayName.isNotEmpty() -> displayName
            name.isNotEmpty() -> name
            else -> key
        }
}

@Serializable
data class Provider(
    val instanceId: String = "",
    val driver: String = "",
    val displayName: String = "",
    val enabled: Boolean = false,
    val installed: Boolean = false,
    val version: String? = null,
    val status: String = "",
    val message: String? = null,
    val auth: JsonElement? = null,
    val models: List<Model> = emptyList(),
)

@Serializable
data class ServerConfig(
    val environment: EnvironmentDescriptor = EnvironmentDescriptor(),
    val cwd: String = "",
    val providers: List<Provider> = emptyList(),
    val scratchWorkspaceRoot: String = "",
    val newProjectsRoot: String = "",
    val settings: JsonElement? = null,
    val keybindings: List<JsonElement> = emptyList(),
)

@Serializable
data class Project(
    val id: String = "",
    val title: String = "",
    val workspaceRoot: String = "",
    val repositoryIdentity: JsonElement? = null,
    val defaultModelSelection: ModelSelection? = null,
    val defaultThreadEnvMode: String? = null,
    val autoPull: Boolean? = null,
    val faviconPath: String? = null,
    val projectIcon: JsonElement? = null,
    val scripts: List<JsonElement> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class ThreadLineage(
    val parentThreadId: String? = null,
    val relationshipToParent: String? = null,
    val rootThreadId: String = "",
)

@Serializable
data class PendingRuntimeRequestSummary(
    val id: String = "",
    val kind: String = "",
    val status: String = "",
)

@Serializable
data class LatestVisibleMessageSummary(
    val id: String = "",
    val role: String = "",
    val text: String = "",
    val createdAt: String = "",
)

@Serializable
data class ThreadShell(
    val createdBy: String = "",
    val creationSource: String = "",
    val id: String = "",
    val projectId: String = "",
    val title: String = "",
    val providerInstanceId: String = "",
    val modelSelection: ModelSelection = ModelSelection(),
    val runtimeMode: String = "",
    val interactionMode: String = "",
    val branch: String? = null,
    val worktreePath: String? = null,
    val pullRequests: List<JsonElement> = emptyList(),
    val linkedPullRequest: JsonElement? = null,
    val lineage: ThreadLineage = ThreadLineage(),
    val activeProviderThreadId: String? = null,
    val latestRunId: String? = null,
    val latestRunRequestedAt: String? = null,
    val latestRunStartedAt: String? = null,
    val latestRunCompletedAt: String? = null,
    val activeRunId: String? = null,
    val activityRunStartedAt: String? = null,
    val activityRunStatus: String? = null,
    val blockedOnThreadWait: Boolean? = null,
    val status: String = "",
    val lastError: String? = null,
    val lastErrorClass: String? = null,
    val lastErrorCode: String? = null,
    val usageLimitResetAt: String? = null,
    val pendingRuntimeRequest: PendingRuntimeRequestSummary? = null,
    val latestVisibleMessage: LatestVisibleMessageSummary? = null,
    val latestUserMessageAt: String? = null,
    val latestUserAuthoredMessageAt: String? = null,
    val hasActionableProposedPlan: Boolean = false,
    val pendingBackgroundTasks: List<JsonElement> = emptyList(),
    val providerInstanceHistory: List<String> = emptyList(),
    val itemCount: Int = 0,
    val visibleItemCount: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
    val archivedAt: String? = null,
    // Present on the V2 projection's thread shape.
    val activeOrderKey: String? = null,
    val pinOrderKey: String? = null,
    val pinnedAt: String? = null,
    val settledAt: String? = null,
    val settledOverride: String? = null,
    val unsettledAt: String? = null,
    val snoozedAt: String? = null,
    val snoozedUntil: String? = null,
    val lastVisitedAt: String? = null,
    val deletedAt: String? = null,
    val forkedFrom: JsonElement? = null,
    val titleRegeneration: JsonElement? = null,
) {
    val isArchived: Boolean get() = archivedAt != null
    val isPinned: Boolean get() = pinnedAt != null
    val isRunning: Boolean get() = activeRunId != null
}

@Serializable
data class ShellSnapshot(
    val schemaVersion: Int = 0,
    val snapshotSequence: Long = 0,
    val threads: List<ThreadShell> = emptyList(),
    val archivedThreads: List<ThreadShell> = emptyList(),
    val projects: List<Project> = emptyList(),
)

@Serializable
data class ShellEvent(
    val kind: String = "",
    val sequence: Long = 0,
    val snapshot: ShellSnapshot? = null,
    val resolvedRepositoryIdentityRoots: List<String> = emptyList(),
    val project: Project? = null,
    val projectId: String = "",
    val location: String = "",
    val thread: ThreadShell? = null,
    val threadId: String = "",
)

@Serializable
data class Message(
    val id: String = "",
    val threadId: String = "",
    val runId: String = "",
    val nodeId: String = "",
    val role: String = "",
    val text: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val createdBy: String = "",
    val creationSource: String = "",
    val attachments: List<JsonElement> = emptyList(),
    val context: JsonElement? = null,
)

@Serializable
data class TurnItem(
    val id: String = "",
    val threadId: String = "",
    val runId: String = "",
    val nodeId: String = "",
    val providerThreadId: String? = null,
    val providerTurnId: String? = null,
    val parentItemId: String? = null,
    val ordinal: Int = 0,
    val status: String = "",
    val title: String = "",
    val type: String = "",
    val text: String = "",
    val input: JsonElement? = null,
    val output: JsonElement? = null,
    val outputOmitted: Boolean? = null,
    val streaming: Boolean? = null,
    val exitCode: Int? = null,
    val toolName: String = "",
    val fileName: String = "",
    val message: String = "",
    val checkpointId: String = "",
    val scopeId: String = "",
    val files: List<JsonElement> = emptyList(),
    val attachments: List<JsonElement> = emptyList(),
    val context: JsonElement? = null,
    val inputIntent: String? = null,
    val messageId: String = "",
    val nativeItemRef: JsonElement? = null,
    val requestId: String = "",
    val questions: List<UserInputQuestion> = emptyList(),
    val questionAnswer: JsonElement? = null,
    val responseMode: String? = null,
    val startedAt: String = "",
    val completedAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class UserInputQuestion(
    val id: String = "",
    val header: String = "",
    val question: String = "",
    val options: List<UserInputOption> = emptyList(),
    val multiSelect: Boolean? = null,
    val allowCustomAnswer: Boolean? = null,
    val initialAnswer: String? = null,
    val required: Boolean? = null,
)

@Serializable
data class UserInputOption(
    val label: String = "",
    val description: String = "",
    val value: String? = null,
    val preview: String? = null,
)

@Serializable
data class VisibleTurnItem(
    val item: TurnItem = TurnItem(),
    val position: Int = 0,
    val sourceItemId: String = "",
    val sourceThreadId: String = "",
    val visibility: String = "",
)

@Serializable
data class RuntimeRequest(
    val id: String = "",
    val nodeId: String = "",
    val providerTurnId: String? = null,
    val kind: String = "",
    val status: String = "",
    val responseCapability: JsonElement? = null,
    val createdAt: String = "",
    val resolvedAt: String? = null,
    val decision: String? = null,
    val feedback: String? = null,
    val answers: JsonElement? = null,
)

@Serializable
data class ThreadProjection(
    val thread: ThreadShell = ThreadShell(),
    val runs: List<JsonElement> = emptyList(),
    val attempts: List<JsonElement> = emptyList(),
    val nodes: List<JsonElement> = emptyList(),
    val subagents: List<JsonElement> = emptyList(),
    val providerSessions: List<JsonElement> = emptyList(),
    val providerThreads: List<JsonElement> = emptyList(),
    val providerTurns: List<JsonElement> = emptyList(),
    val runtimeRequests: List<RuntimeRequest> = emptyList(),
    val messages: List<Message> = emptyList(),
    val plans: List<JsonElement> = emptyList(),
    val turnItems: List<TurnItem> = emptyList(),
    val checkpointScopes: List<JsonElement> = emptyList(),
    val checkpoints: List<JsonElement> = emptyList(),
    val contextHandoffs: List<JsonElement> = emptyList(),
    val contextTransfers: List<JsonElement> = emptyList(),
    val visibleTurnItems: List<VisibleTurnItem> = emptyList(),
    val updatedAt: String = "",
) {
    val pendingRequests: List<RuntimeRequest>
        get() = runtimeRequests.filter { it.status == "pending" }
}

@Serializable
data class ThreadState(
    @SerialName("Projection") val projection: ThreadProjection = ThreadProjection(),
    @SerialName("Sequence") val sequence: Long = 0,
    @SerialName("HistoryCursor") val historyCursor: String? = null,
    @SerialName("HasMoreHistory") val hasMoreHistory: Boolean = false,
    @SerialName("Partial") val partial: Boolean = false,
)

@Serializable
data class ThreadEvent(
    val kind: String = "",
    val sequence: Long = 0,
    val snapshot: ThreadState? = null,
    val event: JsonElement? = null,
    val eventType: String = "",
    val error: String = "",
)

@Serializable
data class ThreadDetailSnapshot(
    val snapshotSequence: Long = 0,
    val projection: ThreadProjection = ThreadProjection(),
    val historyCursor: String? = null,
    val hasMoreHistory: Boolean? = null,
    val latestLocalTurnOrdinal: Int? = null,
)

@Serializable
data class ThreadHistoryPage(
    val snapshotSequence: Long = 0,
    val items: List<VisibleTurnItem> = emptyList(),
    val nextCursor: String? = null,
    val hasMoreHistory: Boolean = false,
)

@Serializable
data class ThreadLaunchResult(
    val threadId: String = "",
    val projection: ThreadProjection = ThreadProjection(),
    val resumed: Boolean = false,
)

@Serializable
data class Attachment(
    val type: String = "file",
    val id: String = "",
    val name: String = "",
    val mimeType: String = "",
    val sizeBytes: Int = 0,
)

@Serializable
data class TerminalSession(
    val threadId: String = "",
    val terminalId: String = "",
    val cwd: String = "",
    val worktreePath: String? = null,
    val status: String = "",
    val pid: Int? = null,
    val history: String = "",
    val exitCode: Int? = null,
    val exitSignal: Int? = null,
    val label: String = "",
    val updatedAt: String = "",
    val sequence: Long? = null,
    val hasRunningSubprocess: Boolean? = null,
)

@Serializable
data class TerminalEvent(
    val type: String = "",
    val threadId: String = "",
    val terminalId: String = "",
    val sequence: Long? = null,
    val data: String = "",
    val snapshot: TerminalSession? = null,
    val message: String = "",
    val exitCode: Int? = null,
    val exitSignal: Int? = null,
    val hasRunningSubprocess: Boolean? = null,
    val label: String = "",
)

@Serializable
data class TokenTotals(
    val uncachedInputTokens: Long = 0,
    val cachedInputTokens: Long = 0,
    val cacheCreationTokens: Long = 0,
    val outputTokens: Long = 0,
    val reasoningTokens: Long = 0,
)

@Serializable
data class UsageBucket(
    val day: String = "",
    val hourStart: String? = null,
    val provider: String = "",
    val model: String = "",
    val sourcePath: String? = null,
    val totals: TokenTotals = TokenTotals(),
    val costUsd: Double = 0.0,
    val cacheSavingsUsd: Double = 0.0,
    val costSource: String = "",
    val records: Long = 0,
    val unpricedRecords: Long = 0,
    val sessions: Long = 0,
)

@Serializable
data class UsageSummary(
    val contractVersion: Int = 0,
    val readAt: String = "",
    val timeZone: String = "",
    val sinceDay: String = "",
    val untilDay: String = "",
    val buckets: List<UsageBucket> = emptyList(),
    val sources: JsonElement? = null,
    val pricing: JsonElement? = null,
    val scanDurationMs: Long = 0,
)

@Serializable
data class VcsStatus(
    val isRepo: Boolean = false,
    val sourceControlProvider: JsonElement? = null,
    val hasPrimaryRemote: Boolean = false,
    val hasUpstream: Boolean = false,
    val isDefaultRef: Boolean = false,
    val refName: String? = null,
    val hasWorkingTreeChanges: Boolean = false,
    val workingTree: JsonElement? = null,
    val aheadCount: Int? = null,
    val aheadOfDefaultCount: Int? = null,
    val behindCount: Int? = null,
    val pr: JsonElement? = null,
    val remote: JsonElement? = null,
)

@Serializable
data class GitRef(
    val name: String = "",
    val isRemote: Boolean = false,
    val current: Boolean = false,
    val isDefault: Boolean = false,
    val worktreePath: String? = null,
)

@Serializable
data class ListRefsResult(
    val refs: List<GitRef> = emptyList(),
    val isRepo: Boolean = false,
    val hasPrimaryRemote: Boolean = false,
    val nextCursor: Int? = null,
    val totalCount: Int = 0,
)

@Serializable
data class DiffFileStat(
    val path: String = "",
    val previousPath: String? = null,
    val additions: Int = 0,
    val deletions: Int = 0,
)

@Serializable
data class DiffPreviewSource(
    val id: String = "",
    val kind: String = "",
    val title: String = "",
    val baseRef: String? = null,
    val headRef: String? = null,
    val diff: String = "",
    val diffHash: String = "",
    val truncated: Boolean = false,
    val files: List<DiffFileStat> = emptyList(),
)

@Serializable
data class DiffPreview(val sources: List<DiffPreviewSource> = emptyList())

@Serializable
data class ProjectEntry(
    val path: String = "",
    val kind: String = "",
    val ignored: Boolean = false,
)

@Serializable
data class ListEntriesResult(
    val entries: List<ProjectEntry> = emptyList(),
    val truncated: Boolean = false,
)

@Serializable
data class ReadFileResult(
    val relativePath: String = "",
    val contents: String = "",
    val byteLength: Int = 0,
    val truncated: Boolean = false,
    val exists: Boolean? = null,
)

@Serializable
data class AccessToken(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("token_type") val tokenType: String = "",
    @SerialName("expires_in") val expiresIn: Int = 0,
    @SerialName("scope") val scope: String = "",
)

@Serializable
data class StreamFrame(
    val values: List<JsonElement> = emptyList(),
    val done: Boolean = false,
    val timeout: Boolean = false,
)

@Serializable
data class WorkspaceStrategy(
    val type: String = "root",
    val branch: String? = null,
    val worktreePath: String? = null,
    val baseRef: String? = null,
    val startFromOrigin: Boolean? = null,
)

@Serializable
data class ProjectSnapshot(
    val projects: List<Project> = emptyList(),
    val updatedAt: String = "",
)
