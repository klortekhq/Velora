package com.klortek.velora.screens

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
    modifier: Modifier = Modifier,
    label: String = "Reproducir",
    icon: ImageVector = Icons.Default.PlayArrow,
    size: androidx.compose.ui.unit.Dp = 85.dp, // Match current button size (100 * 0.85)
    iconSize: androidx.compose.ui.unit.Dp = 20.4.dp, // Match current icon size
    showLabel: Boolean = true,
    labelTextStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.labelLarge.copy(
        fontSize = MaterialTheme.typography.labelLarge.fontSize * 0.7f
    ),
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimary,
    glowAnimationResId: Int? = null // R.raw.button_glow - pass when Lottie file is added
) {
    var focused by remember { mutableStateOf(false) }
    
    // Lottie is optional. Do not manufacture RawRes(0): it is not a valid
    // Android resource and can make an otherwise usable button fail during
    // composition. When no animation is configured, keep the composition
    // absent and render the normal focus treatment below.
    val glowComposition = if (glowAnimationResId != null && glowAnimationResId != 0) {
        val composition by rememberLottieComposition(
            spec = LottieCompositionSpec.RawRes(glowAnimationResId)
        )
        composition
    } else {
        null
    }
    
    // Animate glow based on focus (only if composition is loaded)
    val glowProgress by animateLottieCompositionAsState(
        composition = glowComposition,
        iterations = if (glowComposition != null) LottieConstants.IterateForever else 1,
        isPlaying = focused && glowComposition != null,
        restartOnPlay = true
    )
    
    val focusColor = androidx.compose.ui.graphics.Color.White
    val unfocusColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f)
    val focusedContent = androidx.compose.ui.graphics.Color.Black
    val unfocusedContent = androidx.compose.ui.graphics.Color.White

    val currentContainerColor = if (focused) focusColor else unfocusColor
    val currentContentColor = if (focused) focusedContent else unfocusedContent

    // Button container that extends horizontally when focused
    Box(
        modifier = modifier
            .then(
                if (focused) {
                    Modifier
                        .wrapContentWidth()
                        .height(28.dp)
                } else {
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
