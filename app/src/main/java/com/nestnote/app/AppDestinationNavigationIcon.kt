package com.nestnote.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

/** Navigation affordance for private-vault locking and nested More destinations. */
@Composable
internal fun AppDestinationNavigationIcon(
    destination: String,
    isMoreSubDestination: Boolean,
    onLockPrivateVault: () -> Unit,
    onBackToMore: () -> Unit
) {
    when {
        destination == "Private" -> IconButton(onClick = onLockPrivateVault) {
            Icon(Icons.Default.LockOpen, contentDescription = "Lock Private Vault")
        }
        isMoreSubDestination -> IconButton(onClick = onBackToMore) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to More")
        }
    }
}
