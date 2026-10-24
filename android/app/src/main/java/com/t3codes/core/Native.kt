package com.t3codes.core

/**
 * Low-level JNI surface. Every method returns a JSON string; errors are encoded
 * as {"__error": "...", "__tag": "..."}.
 *
 * Keep this class and its method names in sync with core/mobile/jni.go: JNI
 * resolves them as Java_com_t3codes_core_Native_<method>.
 */
internal object Native {
    init {
        System.loadLibrary("t3core")
    }

    external fun nativeConnect(base: String, token: String, label: String): String
    external fun nativeClose(handle: Long): String
    external fun nativeConfig(handle: Long): String

    external fun nativeCall(handle: Long, method: String, payload: String): String

    external fun nativeShell(handle: Long): String
    external fun nativeShellWatch(handle: Long): String
    external fun nativeShellNext(handle: Long, timeoutMs: Int): String
    external fun nativeShellState(handle: Long): String
    external fun nativeShellClose(handle: Long): String

    external fun nativeThread(handle: Long, threadId: String): String
    external fun nativeThreadWatch(
        handle: Long,
        threadId: String,
        afterSequence: Long,
        acceptBounded: Boolean,
    ): String
    external fun nativeThreadNext(handle: Long, timeoutMs: Int): String
    external fun nativeThreadState(handle: Long): String
    external fun nativeThreadHistory(handle: Long, threadId: String, cursor: String): String
    external fun nativeThreadClose(handle: Long): String

    external fun nativeStreamOpen(handle: Long, method: String, payload: String): String
    external fun nativeStreamNext(handle: Long, timeoutMs: Int): String
    external fun nativeStreamClose(handle: Long): String

    external fun nativeSendMessage(
        handle: Long,
        threadId: String,
        text: String,
        attachmentsJson: String,
        mode: String,
        targetRunId: String,
    ): String

    external fun nativeLaunchThread(
        handle: Long,
        projectId: String,
        title: String,
        modelJson: String,
        workspaceJson: String,
        initialText: String,
        runtimeMode: String,
        interactionMode: String,
    ): String

    external fun nativeStopThread(handle: Long, threadId: String): String

    external fun nativeRespond(
        handle: Long,
        threadId: String,
        requestId: String,
        decision: String,
        feedback: String,
        answersJson: String,
    ): String

    external fun nativeThreadAction(
        handle: Long,
        threadId: String,
        action: String,
        value: String,
    ): String

    external fun nativeTerminalOpen(
        handle: Long,
        threadId: String,
        terminalId: String,
        cwd: String,
        cols: Int,
        rows: Int,
    ): String

    external fun nativeTerminalAttach(
        handle: Long,
        threadId: String,
        terminalId: String,
        cwd: String,
        cols: Int,
        rows: Int,
    ): String

    external fun nativeTerminalNext(handle: Long, timeoutMs: Int): String
    external fun nativeTerminalWrite(
        handle: Long,
        threadId: String,
        terminalId: String,
        data: String,
    ): String

    external fun nativeTerminalResize(
        handle: Long,
        threadId: String,
        terminalId: String,
        cols: Int,
        rows: Int,
    ): String

    external fun nativeTerminalClose(handle: Long, threadId: String, terminalId: String): String
    external fun nativeTerminalCloseHandle(handle: Long): String

    external fun nativeAttachmentUpload(
        handle: Long,
        type: String,
        name: String,
        mimeType: String,
        data: ByteArray,
    ): String

    external fun nativeUsage(
        handle: Long,
        sinceDay: String,
        untilDay: String,
        timeZone: String,
    ): String

    external fun nativeVcsStatus(handle: Long, cwd: String): String
    external fun nativeDiffPreview(handle: Long, cwd: String): String
    external fun nativeListEntries(handle: Long, cwd: String, dir: String): String
    external fun nativeReadFile(handle: Long, cwd: String, path: String): String

    external fun nativePair(base: String, code: String, label: String): String
    external fun nativeProbe(base: String): String
}
