package org.strigate.ferrot.presentation.transitions

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

object Transitions {
    val searchEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { it / 3 },
        animationSpec = tween(180),
    ) + fadeIn(
        animationSpec = tween(120),
    )
    val searchExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { it / 3 },
        animationSpec = tween(120),
    ) + fadeOut(
        animationSpec = tween(90),
    )

    val titleExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { -it / 3 },
        animationSpec = tween(120),
    ) + fadeOut(
        animationSpec = tween(90),
    )
    val titleEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { -it / 3 },
        animationSpec = tween(180),
    ) + fadeIn(
        animationSpec = tween(120),
    )

    val emptyEnter: EnterTransition = fadeIn(animationSpec = tween(120))
    val emptyExit: ExitTransition = fadeOut(animationSpec = tween(90))

    val listItemEnter: EnterTransition = expandVertically() + fadeIn()
    val listItemExit: ExitTransition = shrinkVertically() + fadeOut()
}
