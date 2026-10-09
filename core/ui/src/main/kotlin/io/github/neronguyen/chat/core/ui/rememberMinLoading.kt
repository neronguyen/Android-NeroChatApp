package io.github.neronguyen.chat.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberMinLoading(
    isLoading: Boolean,
    minDurationMillis: Long = 500L
): State<Boolean> {
    var loadingStartTime by remember { mutableLongStateOf(0L) }

    return produceState(initialValue = isLoading, key1 = isLoading) {
        if (isLoading) {
            loadingStartTime = System.currentTimeMillis()
            value = true
        } else {
            val elapsedTime = System.currentTimeMillis() - loadingStartTime
            val remainingTime = minDurationMillis - elapsedTime
            if (remainingTime > 0) {
                delay(remainingTime.milliseconds)
            }
            value = false
        }
    }
}
