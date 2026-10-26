package com.t3codes.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.t3codes.core.FilesystemBrowseEntry
import com.t3codes.core.FilesystemBrowseResult
import com.t3codes.core.Project
import com.t3codes.core.ServerConfig
import com.t3codes.core.SourceControlDiscoveryResult
import com.t3codes.core.SourceControlProviderDiscovery
import com.t3codes.core.SourceControlRepositoryInfo
import com.t3codes.core.VcsStatus
import com.t3codes.core.optionString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import java.util.Locale
import java.util.UUID

private const val STEP_MENU = "menu"
private const val STEP_NEW = "new"
private const val STEP_LOCAL = "local"
private const val STEP_CLONE_SOURCE = "clone-source"
private const val STEP_CLONE_DESTINATION = "clone-destination"

@Composable
fun AddProjectScreen(state: UiState, viewModel: MainViewModel) {
    var step by rememberSaveable { mutableStateOf(STEP_MENU) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var discovery by remember { mutableStateOf<SourceControlDiscoveryResult?>(null) }
    var discoveryLoading by remember { mutableStateOf(true) }
    var discoveryError by remember { mutableStateOf<String?>(null) }

    var newName by rememberSaveable { mutableStateOf("") }
    var publishRepository by rememberSaveable { mutableStateOf(true) }
    var publishProvider by rememberSaveable { mutableStateOf("") }

    var localPath by rememberSaveable { mutableStateOf(directoryPath(addProjectBaseDirectory(state.config))) }
    var localRepository by remember { mutableStateOf<VcsStatus?>(null) }
    var localRepositoryLoading by remember { mutableStateOf(false) }

    var cloneSource by rememberSaveable { mutableStateOf("url") }
    var cloneRemoteUrl by rememberSaveable { mutableStateOf("") }
    var cloneRepositoryInput by rememberSaveable { mutableStateOf("") }
    var cloneRepositoryInfo by remember { mutableStateOf<SourceControlRepositoryInfo?>(null) }
    var cloneTitle by rememberSaveable { mutableStateOf("") }
    var clonePath by rememberSaveable { mutableStateOf("") }
    var cloneLookupBusy by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val providers = discovery?.sourceControlProviders.orEmpty()
    val readyProviders = providers.filter { it.isReady }
    val publishProviders = readyProviders.filter(::supportsNewRepository)
    val newProjectsRoot = state.config?.newProjectsRoot.orEmpty()
    val baseDirectory = addProjectBaseDirectory(state.config)
    val folderName = newProjectFolderName(newName)

    fun moveTo(next: String) {
        error = null
        step = next
    }

    fun goBack() {
        when (step) {
            STEP_MENU -> viewModel.closeAddProject()
            STEP_CLONE_DESTINATION -> moveTo(STEP_CLONE_SOURCE)
            else -> moveTo(STEP_MENU)
        }
    }

    LaunchedEffect(Unit) {
        discoveryLoading = true
        discoveryError = null
        runCatching { viewModel.discoverSourceControl() }
            .onSuccess { discovery = it }
            .onFailure { discoveryError = messageFor(it) }
        discoveryLoading = false
    }

    LaunchedEffect(publishProviders) {
        if (publishProvider.isBlank() || publishProviders.none { it.kind == publishProvider }) {
            publishProvider = publishProviders.firstOrNull()?.kind.orEmpty()
        }
    }

    LaunchedEffect(localPath) {
        val path = localPath.trim()
        if (path.isBlank() || !stepStartsWithLocal(step)) {
            localRepository = null
            return@LaunchedEffect
        }
        localRepositoryLoading = true
        delay(350)
        localRepository = runCatching {
            viewModel.inspectRepository(path.removeSuffix("/").ifBlank { "/" })
        }.onFailure {
            localRepository = null
        }.getOrNull()
        localRepositoryLoading = false
    }

    BackHandler { goBack() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = when (step) {
                    STEP_MENU -> "Add project"
                    STEP_NEW -> "New project"
                    STEP_LOCAL -> "Local folder"
                    STEP_CLONE_SOURCE -> "Clone repository"
                    else -> "Clone destination"
                },
                subtitle = when (step) {
                    STEP_MENU -> environmentLabel(state)
                    STEP_NEW -> "Create a Git repository on ${environmentLabel(state)}"
                    STEP_LOCAL -> "Use a folder or Git checkout on ${environmentLabel(state)}"
                    STEP_CLONE_SOURCE -> "Choose a Git remote to clone"
                    else -> "Where should this repository live?"
                },
                leading = { BackButton(onClick = { goBack() }) },
                actions = {
                    if (step == STEP_MENU) {
                        IconButtonRefresh {
                            error = null
                            discoveryLoading = true
                            scope.launch {
                                runCatching { viewModel.discoverSourceControl() }
                                    .onSuccess { discovery = it; discoveryError = null }
                                    .onFailure { discoveryError = messageFor(it) }
                                discoveryLoading = false
                            }
                        }
                    }
                },
            )
        }

        if (error != null) {
            item {
                ErrorBanner(
                    message = error.orEmpty(),
                    onDismiss = { error = null },
                )
            }
        }

        when (step) {
            STEP_MENU -> {
                item {
                    SectionLabel("Create")
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column {
                            if (newProjectsRoot.isNotBlank()) {
                                AddProjectOption(
                                    icon = Icons.Rounded.CreateNewFolder,
                                    title = "New project",
                                    subtitle = "Start a new Git repository under $newProjectsRoot",
                                    enabled = !busy,
                                    onClick = { moveTo(STEP_NEW) },
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                            AddProjectOption(
                                icon = Icons.Rounded.FolderOpen,
                                title = "Local folder",
                                subtitle = "Select an existing folder or Git repository",
                                enabled = !busy,
                                onClick = { moveTo(STEP_LOCAL) },
                            )
                        }
                    }
                }

                item {
                    SectionLabel("Clone")
                }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column {
                            AddProjectOption(
                                icon = Icons.Rounded.Link,
                                title = "Git URL",
                                subtitle = "Clone from any HTTPS or SSH remote",
                                enabled = !busy,
                                onClick = {
                                    cloneSource = "url"
                                    cloneRepositoryInfo = null
                                    cloneTitle = ""
                                    clonePath = ""
                                    moveTo(STEP_CLONE_SOURCE)
                                },
                            )
                            providers.forEach { provider ->
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                                AddProjectOption(
                                    icon = providerIcon(provider.kind),
                                    title = provider.label.ifBlank { provider.kind },
                                    subtitle = providerSubtitle(provider),
                                    enabled = !busy && provider.isReady,
                                    onClick = {
                                        cloneSource = provider.kind
                                        cloneRepositoryInfo = null
                                        cloneRepositoryInput = ""
                                        cloneTitle = ""
                                        clonePath = ""
                                        moveTo(STEP_CLONE_SOURCE)
                                    },
                                )
                            }
                        }
                    }
                }

                if (discoveryLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = "Checking source control on the server…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (!discoveryError.isNullOrBlank()) {
                    item {
                        NoteCard(
                            text = "Could not load GitHub/GitLab integrations. ${discoveryError} " +
                                "You can still add a project or clone from a Git URL.",
                        )
                    }
                } else if (!discoveryLoading && providers.isEmpty()) {
                    item {
                        NoteCard(
                            text = "No source control integrations were reported by this server. " +
                                "Use Local folder or Git URL instead.",
                        )
                    }
                }

                item {
                    Text(
                        text = "Projects are created on ${environmentLabel(state)}. " +
                            "Your phone connects to the same filesystem.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            STEP_NEW -> {
                item {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Project name") },
                        placeholder = { Text("my-project") },
                        supportingText = {
                            Text(
                                if (folderName.isNotBlank()) {
                                    "Creates ${joinPath(newProjectsRoot, folderName)}"
                                } else {
                                    "A folder name is generated from the project name."
                                },
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                        ),
                    )
                }

                if (publishProviders.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Publish to a remote",
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            text = "Create a private repository and push the first commit.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Switch(
                                        checked = publishRepository,
                                        onCheckedChange = { publishRepository = it },
                                        enabled = !busy,
                                    )
                                }
                                if (publishRepository && publishProviders.size > 1) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        publishProviders.forEach { provider ->
                                            val selected = publishProvider == provider.kind
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                                },
                                                contentColor = if (selected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                onClick = { publishProvider = provider.kind },
                                            ) {
                                                Text(
                                                    text = provider.label.ifBlank {
                                                        provider.kind.replaceFirstChar { it.uppercase() }
                                                    },
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                    style = MaterialTheme.typography.labelMedium,
                                                )
                                            }
                                        }
                                    }
                                }
                                if (publishRepository) {
                                    val target = publishProviders.firstOrNull {
                                        it.kind == publishProvider
                                    } ?: publishProviders.firstOrNull()
                                    if (target != null) {
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            text = "Repository: ${publishRepositoryName(target, folderName)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (newName.isBlank()) {
                                error = "Enter a project name."
                                return@Button
                            }
                            busy = true
                            error = null
                            scope.launch {
                                try {
                                    val created = viewModel.createNamedProject(newName.trim())
                                    val project = viewModel.awaitProject(created.projectId)
                                        ?: Project(
                                            id = created.projectId,
                                            title = newName.trim(),
                                            workspaceRoot = created.workspaceRoot,
                                        )
                                    var publishMessage = created.commitError
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { "Created without a first commit. $it" }
                                    val target = publishProviders.firstOrNull {
                                        it.kind == publishProvider
                                    } ?: publishProviders.firstOrNull()
                                    val createdFolder = created.workspaceRoot
                                        .trimEnd('/')
                                        .substringAfterLast('/')
                                        .ifBlank { folderName }
                                    if (publishRepository && target != null) {
                                        runCatching {
                                            viewModel.publishRepository(
                                                cwd = project.workspaceRoot,
                                                provider = target.kind,
                                                repository = publishRepositoryName(target, createdFolder),
                                            )
                                        }.onFailure {
                                            val failure = "Project created, but publishing failed. ${messageFor(it)}"
                                            publishMessage = listOfNotNull(publishMessage, failure).joinToString(" ")
                                        }
                                    }
                                    viewModel.finishAddProject(project)
                                    if (publishMessage != null) {
                                        viewModel.showMessage(publishMessage.orEmpty())
                                    }
                                } catch (failure: Throwable) {
                                    error = messageFor(failure)
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy && newName.isNotBlank(),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(9.dp))
                        }
                        Text(if (busy) "Creating…" else "Create project")
                    }
                }
            }

            STEP_LOCAL -> {
                item {
                    OutlinedTextField(
                        value = localPath,
                        onValueChange = {
                            localPath = it
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Folder path") },
                        placeholder = { Text("~/code/project") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                        supportingText = {
                            when {
                                localRepositoryLoading -> Text("Checking for a Git repository…")
                                localRepository?.isRepo == true -> Text("Git repository detected")
                                localPath.isNotBlank() -> Text("The folder will be registered as a project.")
                            }
                        },
                        trailingIcon = {
                            if (localRepository?.isRepo == true) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Git repository",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                    )
                }

                item {
                    FolderBrowser(
                        path = localPath,
                        pinnedName = "",
                        viewModel = viewModel,
                        onSelect = { selected -> localPath = directoryPath(selected) },
                    )
                }

                item {
                    Button(
                        onClick = {
                            if (localPath.isBlank()) {
                                error = "Choose a folder first."
                                return@Button
                            }
                            busy = true
                            error = null
                            scope.launch {
                                try {
                                    val existing = state.projects.firstOrNull {
                                        sameWorkspacePath(it.workspaceRoot, localPath)
                                    }
                                    if (existing != null) {
                                        viewModel.finishAddProject(existing)
                                        return@launch
                                    }
                                    val project = viewModel.addExistingProject(localPath.trim())
                                    viewModel.finishAddProject(project)
                                } catch (failure: Throwable) {
                                    error = messageFor(failure)
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy && localPath.isNotBlank(),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(9.dp))
                        }
                        Text(if (busy) "Adding…" else "Add project")
                    }
                }
            }

            STEP_CLONE_SOURCE -> {
                val provider = providers.firstOrNull { it.kind == cloneSource }
                if (cloneSource == "url") {
                    item {
                        OutlinedTextField(
                            value = cloneRemoteUrl,
                            onValueChange = {
                                cloneRemoteUrl = it
                                error = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Repository URL") },
                            placeholder = { Text("https://github.com/owner/repo.git") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done,
                            ),
                        )
                    }
                    item {
                        NoteCard(
                            text = "Use an HTTPS or SSH URL. Authentication is handled by Git on the server, " +
                                "or choose GitHub/GitLab above to use your saved source control login.",
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                val url = cloneRemoteUrl.trim()
                                if (url.isBlank()) {
                                    error = "Enter a repository URL."
                                    return@Button
                                }
                                cloneRemoteUrl = url
                                cloneRepositoryInfo = null
                                cloneTitle = cloneTitleFromUrl(url)
                                clonePath = joinPath(baseDirectory, cloneTitle)
                                moveTo(STEP_CLONE_DESTINATION)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                        ) {
                            Text("Continue")
                        }
                    }
                } else {
                    item {
                        OutlinedTextField(
                            value = cloneRepositoryInput,
                            onValueChange = {
                                cloneRepositoryInput = it
                                error = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("${provider?.label ?: cloneSource.replaceFirstChar { it.uppercase() }} repository")
                            },
                            placeholder = { Text("owner/repository") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            supportingText = {
                                if (provider != null && !provider.isReady) {
                                    Text(providerSubtitle(provider))
                                }
                            },
                        )
                    }
                    item {
                        Button(
                            onClick = {
                                val repository = cloneRepositoryInput.trim()
                                if (repository.isBlank()) {
                                    error = "Enter a repository, for example owner/repository."
                                    return@Button
                                }
                                cloneLookupBusy = true
                                busy = true
                                error = null
                                scope.launch {
                                    try {
                                        val info = viewModel.lookupRepository(cloneSource, repository)
                                        cloneRepositoryInfo = info
                                        cloneRepositoryInput = info.nameWithOwner.ifBlank { repository }
                                        cloneRemoteUrl = info.url
                                        cloneTitle = info.nameWithOwner.substringAfterLast('/')
                                            .ifBlank { cloneTitleFromUrl(info.url) }
                                        clonePath = joinPath(baseDirectory, cloneTitle)
                                        moveTo(STEP_CLONE_DESTINATION)
                                    } catch (failure: Throwable) {
                                        error = messageFor(failure)
                                    } finally {
                                        cloneLookupBusy = false
                                        busy = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy && cloneRepositoryInput.isNotBlank() &&
                                provider?.isReady != false,
                        ) {
                            if (cloneLookupBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(Modifier.width(9.dp))
                            }
                            Text(if (cloneLookupBusy) "Looking up…" else "Look up repository")
                        }
                    }
                    if (provider != null && !provider.isReady) {
                        item {
                            NoteCard(
                                text = providerSubtitle(provider) +
                                    " Fix the source control login or CLI installation on the server to use this option.",
                            )
                        }
                    }
                }
            }

            STEP_CLONE_DESTINATION -> {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = providerIcon(cloneSource),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cloneRepositoryInfo?.nameWithOwner
                                        ?: cloneTitle.ifBlank { "Git repository" },
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = cloneRepositoryInfo?.url ?: cloneRemoteUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = clonePath,
                        onValueChange = {
                            clonePath = it
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Destination path") },
                        placeholder = { Text("~/repo-name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                        supportingText = {
                            Text("The project folder is created here. Relative paths start from ~.")
                        },
                    )
                }
                item {
                    FolderBrowser(
                        path = clonePath,
                        pinnedName = cloneTitle,
                        viewModel = viewModel,
                        onSelect = { selected ->
                            clonePath = joinPath(selected, cloneTitle)
                        },
                    )
                }
                item {
                    Button(
                        onClick = {
                            if (clonePath.isBlank()) {
                                error = "Choose a destination folder."
                                return@Button
                            }
                            busy = true
                            error = null
                            scope.launch {
                                try {
                                    val existing = state.projects.firstOrNull {
                                        sameWorkspacePath(it.workspaceRoot, clonePath)
                                    }
                                    if (existing != null) {
                                        viewModel.finishAddProject(existing, openThread = false)
                                        viewModel.showMessage("That project is already added.")
                                        return@launch
                                    }
                                    val provider = if (cloneSource == "url") "" else cloneSource
                                    val repository = if (cloneSource == "url") {
                                        ""
                                    } else {
                                        cloneRepositoryInfo?.nameWithOwner ?: cloneRepositoryInput.trim()
                                    }
                                    val remoteUrl = if (cloneSource == "url") {
                                        cloneRemoteUrl.trim()
                                    } else {
                                        cloneRepositoryInfo?.url ?: cloneRemoteUrl.trim()
                                    }
                                    val title = cloneTitle.ifBlank {
                                        cloneRepositoryInfo?.nameWithOwner?.substringAfterLast('/')
                                            .orEmpty()
                                            .ifBlank { cloneTitleFromUrl(remoteUrl) }
                                    }
                                    if (state.config?.environment?.capabilities?.projectCloneTracking == true) {
                                        val projectId = UUID.randomUUID().toString()
                                        val started = viewModel.startProjectClone(
                                            projectId = projectId,
                                            title = title.ifBlank { "Cloned project" },
                                            remoteUrl = remoteUrl,
                                            destinationPath = clonePath.trim(),
                                            provider = provider,
                                            repository = repository,
                                        )
                                        val project = viewModel.awaitProject(started.projectId)
                                            ?: Project(
                                                id = started.projectId,
                                                title = title.ifBlank { "Cloned project" },
                                                workspaceRoot = started.cwd,
                                            )
                                        viewModel.finishAddProject(project, openThread = false)
                                        viewModel.showMessage(
                                            "Cloning ${title.ifBlank { "repository" }}. " +
                                                "It will appear on Projects as it downloads.",
                                        )
                                    } else {
                                        val cloned = viewModel.cloneRepository(
                                            remoteUrl = remoteUrl,
                                            destinationPath = clonePath.trim(),
                                            provider = provider,
                                            repository = repository,
                                        )
                                        val project = viewModel.addExistingProject(
                                            workspaceRoot = cloned.cwd,
                                            title = title,
                                        )
                                        viewModel.finishAddProject(project)
                                    }
                                } catch (failure: Throwable) {
                                    error = messageFor(failure)
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy && clonePath.isNotBlank(),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(9.dp))
                        }
                        Text(if (busy) "Cloning…" else "Clone project")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddProjectOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (enabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            contentColor = if (enabled) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(9.dp).size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FolderBrowser(
    path: String,
    pinnedName: String,
    viewModel: MainViewModel,
    onSelect: (String) -> Unit,
) {
    val target = browseDirectoryFor(path, pinnedName)
    var result by remember { mutableStateOf<FilesystemBrowseResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var browseError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(target) {
        loading = true
        browseError = null
        delay(250)
        runCatching { viewModel.browseProjectDirectory(target) }
            .onSuccess { result = it }
            .onFailure {
                result = null
                browseError = messageFor(it)
            }
        loading = false
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Folders in ${result?.parentPath ?: target}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(
                    onClick = {
                        val current = result?.parentPath ?: target
                        val parent = parentDirectory(current)
                        if (parent != current) onSelect(parent)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("Up", style = MaterialTheme.typography.labelSmall)
                }
            }

            when {
                loading -> Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Loading folders…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                browseError != null -> Text(
                    text = browseError.orEmpty(),
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                result?.entries.isNullOrEmpty() -> Text(
                    text = "No folders here.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> result?.entries.orEmpty().forEach { entry ->
                    BrowserRow(
                        entry = entry,
                        onClick = {
                            onSelect(
                                if (pinnedName.isBlank()) {
                                    directoryPath(entry.fullPath)
                                } else {
                                    joinPath(entry.fullPath, pinnedName)
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserRow(entry: FilesystemBrowseEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Folder,
            contentDescription = null,
            modifier = Modifier.size(19.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = entry.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun IconButtonRefresh(onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick) {
        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh source control")
    }
}

@Composable
private fun NoteCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Code,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun stepStartsWithLocal(step: String): Boolean = step == STEP_LOCAL

private fun environmentLabel(state: UiState): String =
    state.config?.environment?.label?.takeIf { it.isNotBlank() }
        ?: state.baseUrl
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')
            .ifBlank { "this machine" }

private fun addProjectBaseDirectory(config: ServerConfig?): String {
    val raw = (config?.settings as? JsonObject)
        ?.get("addProjectBaseDirectory")
        ?.optionString()
        ?.trim()
        .orEmpty()
    if (raw.isNotBlank()) return raw
    return "~"
}

private fun providerIcon(kind: String): ImageVector = when (kind.lowercase(Locale.ROOT)) {
    "github", "gitlab", "forgejo", "gitcafe", "bitbucket", "azure-devops" -> Icons.Rounded.Code
    else -> Icons.Rounded.AddLink
}

private fun providerSubtitle(provider: SourceControlProviderDiscovery): String {
    if (provider.status != "available") {
        return provider.installHint.ifBlank { "The ${provider.label} CLI is not installed on the server." }
    }
    if (provider.auth.status == "unauthenticated") {
        return provider.readinessHint.ifBlank {
            "${provider.label} is not signed in. Open Source Control settings on the server."
        }
    }
    val account = provider.auth.accountName
    if (!account.isNullOrBlank()) {
        return "Signed in as $account"
    }
    return provider.readinessHint.ifBlank { "Clone a repository using ${provider.label}." }
}

private fun supportsNewRepository(provider: SourceControlProviderDiscovery): Boolean =
    when (provider.kind) {
        "github", "gitlab" -> true
        "forgejo", "gitcafe" -> !provider.auth.accountName.isNullOrBlank()
        else -> false
    }

private fun publishRepositoryName(
    provider: SourceControlProviderDiscovery,
    folderName: String,
): String {
    val owner = provider.auth.accountName?.trim().orEmpty()
    return if (owner.isBlank()) folderName else "$owner/$folderName"
}

private fun newProjectFolderName(name: String): String {
    val slug = name.trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(64)
        .trim('-')
    if (slug.isBlank()) return "project"
    val reserved = setOf("con", "prn", "aux", "nul") +
        (1..9).map { "com$it" } + (1..9).map { "lpt$it" }
    return if (slug in reserved) "$slug-project" else slug
}

private fun cloneTitleFromUrl(value: String): String {
    val withoutQuery = value.substringBefore('?').substringBefore('#').trim()
    val path = withoutQuery
        .replace(Regex("^[a-z][a-z0-9+.-]*://", RegexOption.IGNORE_CASE), "")
        .substringAfter(':', withoutQuery)
    val segments = path.split('/', '\\').filter { it.isNotBlank() }
    val candidate = segments.lastOrNull().orEmpty().removeSuffix(".git")
    return candidate.ifBlank { "repository" }
}

private fun sameWorkspacePath(first: String, second: String): Boolean {
    fun normalize(value: String): String = value.trim()
        .trimEnd('/', '\\')
        .replace('\\', '/')
    return normalize(first).equals(normalize(second), ignoreCase = true)
}

private fun joinPath(base: String, name: String): String {
    val root = base.trim().ifBlank { "~" }.trimEnd('/')
    val leaf = name.trim().trim('/')
    return when {
        root.isBlank() -> "/$leaf"
        leaf.isBlank() -> "$root/"
        else -> "$root/$leaf"
    }
}

private fun directoryPath(path: String): String {
    val value = path.trim().ifBlank { "~" }
    return if (value.endsWith("/")) value else "$value/"
}

private fun browseDirectoryFor(path: String, pinnedName: String): String {
    var value = path.trim().ifBlank { "~/" }
    if (pinnedName.isNotBlank() && value.endsWith("/$pinnedName")) {
        value = value.removeSuffix("/$pinnedName")
    }
    return directoryPath(value.ifBlank { "~/" })
}

private fun parentDirectory(path: String): String {
    val value = path.trim().trimEnd('/')
    if (value.isEmpty()) return "/"
    val index = value.lastIndexOf('/')
    return when {
        index < 0 -> "~/"
        index == 0 -> "/"
        else -> value.substring(0, index) + "/"
    }
}

private fun messageFor(error: Throwable): String =
    error.message?.trim()?.takeIf { it.isNotBlank() } ?: error::class.java.simpleName
