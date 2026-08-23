package com.klortek.velora.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.klortek.velora.R
import com.klortek.velora.jellyfin.JellyfinItem

@Composable
fun ContinueWatchingActionsDialog(
    item: JellyfinItem,
    onRemove: () -> Unit,
    onMarkWatched: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(text = item.Name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(R.string.continue_watching_actions),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.continue_watching_remove_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                )

                ActionButton(
                    text = stringResource(R.string.continue_watching_remove),
                    onClick = onRemove,
                    filled = true
                )

                ActionButton(
                    text = stringResource(R.string.continue_watching_mark_watched),
                    onClick = onMarkWatched,
                    filled = false
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ActionButton(
                        text = stringResource(R.string.cancel),
                        onClick = onCancel,
                        filled = false,
                        compact = true
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    filled: Boolean,
    compact: Boolean = false
) {
    Box(
        modifier = Modifier
            .then(if (compact) Modifier.widthIn(min = 112.dp) else Modifier.fillMaxWidth())
            .background(
                if (filled) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else Color.Transparent,
                RoundedCornerShape(28.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (filled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
