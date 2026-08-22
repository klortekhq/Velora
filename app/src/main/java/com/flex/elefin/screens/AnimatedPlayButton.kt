Warning: truncated output (original token count: 1568)
Total output lines: 154

package com.flex.elefin.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.animateLottieCompositionAsState

/**
 * Animated play button with Lottie glow effect and icon morphing.
 * Can be used for both Play and Resume buttons.
 */
@Composable
fun AnimatedPlayButton(
    onClick: () -> Unit,
  …568 tokens truncated…             } else {
                    Modifier.size(28.dp) // Circular when unfocused
                }
            )
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
            .background(currentContainerColor, androidx.compose.foundation.shape.CircleShape)
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .onFocusChanged { focusState ->
                focused = focusState.isFocused
            }
    ) {
        // Lottie glow animation (optional - only if composition loaded)
        if (glowComposition != null) {
            LottieAnimation(
                composition = glowComposition,
                progress = { glowProgress },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Icon and label content
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(iconSize),
                tint = currentContentColor
            )
            
            // Show label when focused
            if (focused && showLabel) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = labelTextStyle,
                    color = currentContentColor,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

