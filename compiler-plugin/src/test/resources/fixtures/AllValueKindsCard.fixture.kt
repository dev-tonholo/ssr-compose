package fixtures.consumer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

enum class Style {
    bodyMedium,
    titleLarge,
}

enum class Tag {
    featured,
    onSale,
}

@ConsumerComposable
@Composable
fun AllValueKindsCard(
    title: String,
    count: Int,
    total: Long,
    rating: Float,
    price: Double,
    enabled: Boolean,
    style: Style,
    labels: List<String>,
    counts: List<Int>,
    totals: List<Long>,
    ratings: List<Float>,
    prices: List<Double>,
    flags: List<Boolean>,
    tags: List<Tag>,
) {
}
