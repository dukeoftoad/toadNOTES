package com.nestnote.app

/**
 * Coordinates password verifier and data-key rewrap updates across separate preference stores.
 * The operation is not atomic; rollback failure must force a locked, recovery-required state.
 */
internal object PrivateVaultPasswordChange {
    internal data class Result(
        val accepted: Boolean,
        val recoveryRequired: Boolean = false,
        val failure: Failure? = null
    )

    internal enum class Failure { KEY_REWRAP, VERIFIER_UPDATE, ROLLBACK }

    fun execute(
        current: String,
        replacement: String,
        rewrapKey: (from: String, to: String) -> Boolean,
        updateVerifier: () -> Boolean,
        markRecoveryRequired: () -> Unit
    ): Result {
        if (!rewrapKey(current, replacement)) return Result(false, failure = Failure.KEY_REWRAP)
        if (updateVerifier()) return Result(true)
        if (rewrapKey(replacement, current)) return Result(false, failure = Failure.VERIFIER_UPDATE)
        markRecoveryRequired()
        return Result(false, recoveryRequired = true, failure = Failure.ROLLBACK)
    }
}
