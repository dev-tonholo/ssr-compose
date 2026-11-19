package fixtures.consumer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

enum class BadgeTone {
    calm,
    urgent,
}

typealias TitleText = String
typealias NullableTitleText = String?
typealias ItemCount = Int
typealias TotalCount = Long
typealias ScoreValue = Float
typealias PriceValue = Double
typealias EnabledFlag = Boolean
typealias ToneChoice = BadgeTone
typealias NullableToneChoice = BadgeTone?
typealias LabelList = List<TitleText>
typealias NullableToneList = List<ToneChoice>?
typealias NestedTitleText = TitleText

@ConsumerComposable
@Composable
fun TypealiasValueKindsCard(
    title: NestedTitleText,
    subtitle: NullableTitleText = null,
    count: ItemCount = 0,
    total: TotalCount,
    score: ScoreValue,
    price: PriceValue,
    enabled: EnabledFlag,
    tone: ToneChoice,
    secondaryTone: NullableToneChoice,
    labels: LabelList,
    tones: NullableToneList = null,
) {
}
