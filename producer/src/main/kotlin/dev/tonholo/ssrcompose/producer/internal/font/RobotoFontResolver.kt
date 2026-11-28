package dev.tonholo.ssrcompose.producer.internal.font

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver

private val resolverWithNoFontBundle: FontFamily.Resolver by lazy { loadedFontResolver(faces = bundledRobotoFaces()) }

/**
 * The resolver of sessions with no font bundle (ADR 0030), which `ScreenScene` gives every screen, since no screen has
 * a bundle yet. It has loaded the four bundled Roboto faces, and screens share one typeface per face.
 *
 * Compose may draw a glyph that no system font has from any font a resolver has loaded. A text's width stays the same
 * in any screen order only while no screen loads another font. A screen with another font loads it here, even when the
 * producer then refuses to send that screen.
 *
 * Call it and use it on the scene thread only: Compose's font cache has no lock (ADR 0021).
 */
internal fun robotoFontResolver(): FontFamily.Resolver = resolverWithNoFontBundle

/** A new font resolver that has loaded every one of [faces], in their order. */
internal fun loadedFontResolver(faces: List<Font>): FontFamily.Resolver {
    val resolver = createFontFamilyResolver()
    faces.forEach { face -> resolver.load(face = face) }
    return resolver
}

private fun FontFamily.Resolver.load(face: Font) {
    resolve(fontFamily = FontFamily(fonts = listOf(face)), fontWeight = face.weight, fontStyle = face.style)
}
