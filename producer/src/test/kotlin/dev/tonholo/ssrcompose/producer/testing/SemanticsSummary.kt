package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull

private val READER_MODIFIER_NAMES = setOf("background", "border", "clickable", "scroll")

internal data class SemanticsSummary(
    val text: String?,
    val modifiers: List<String>,
    val hasClickAction: Boolean,
    val children: List<SemanticsSummary>,
)

internal fun ImageComposeScene.rootChildNodes(): List<SemanticsNode> =
    semanticsOwners.single().unmergedRootSemanticsNode.children

internal fun ImageComposeScene.rootChildrenSummary(): List<SemanticsSummary> =
    semanticsOwners.single().unmergedRootSemanticsNode.children.map { it.toSummary() }

private fun SemanticsNode.toSummary(): SemanticsSummary = SemanticsSummary(
    text = config.getOrNull(SemanticsProperties.Text)?.joinToString(separator = "") { it.text },
    modifiers = layoutInfo.getModifierInfo()
        .mapNotNull { (it.modifier as? InspectableValue)?.nameFallback }
        .filter { it in READER_MODIFIER_NAMES },
    hasClickAction = config.getOrNull(SemanticsActions.OnClick) != null,
    children = children.map { it.toSummary() },
)
