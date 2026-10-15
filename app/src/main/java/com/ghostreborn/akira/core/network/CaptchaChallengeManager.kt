package com.ghostreborn.akira.core.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/** A token obtained through the app's visible, user-completed CAPTCHA UI. */
data class CaptchaSolution(
    val token: String,
    val provider: String
)

data class CaptchaChallengeState(
    val id: Long,
    val message: String = "Complete the security check to continue."
)

/**
 * Bridges API requests that need CAPTCHA to the Compose UI. Requests suspend until the user
 * completes verification; this class does not attempt to solve or bypass CAPTCHA automatically.
 */
object CaptchaChallengeManager {
    private const val CHALLENGE_TIMEOUT_MS = 180_000L

    private data class PendingChallenge(
        val id: Long,
        val result: CompletableDeferred<CaptchaSolution>
    )

    private val mutex = Mutex()
    private var nextId = 1L
    @Volatile private var pending: PendingChallenge? = null

    private val _challenge = MutableStateFlow<CaptchaChallengeState?>(null)
    val challenge: StateFlow<CaptchaChallengeState?> = _challenge.asStateFlow()

    suspend fun awaitSolution(): CaptchaSolution = mutex.withLock {
        val current = PendingChallenge(nextId++, CompletableDeferred())
        pending = current
        _challenge.value = CaptchaChallengeState(current.id)
        try {
            withTimeout(CHALLENGE_TIMEOUT_MS) { current.result.await() }
        } catch (_: TimeoutCancellationException) {
            throw IllegalStateException("Security verification timed out. Please retry the request.")
        } finally {
            if (pending?.id == current.id) {
                pending = null
                _challenge.value = null
            }
        }
    }

    fun submitSolution(token: String, provider: String) {
        val safeToken = token.trim()
        val safeProvider = provider.trim()
        if (safeToken.isEmpty() || safeProvider.isEmpty()) return
        pending?.result?.complete(CaptchaSolution(safeToken, safeProvider))
    }

    fun cancelChallenge() {
        val current = pending ?: run {
            _challenge.value = null
            return
        }
        current.result.completeExceptionally(IllegalStateException("Security verification was cancelled."))
    }
}
