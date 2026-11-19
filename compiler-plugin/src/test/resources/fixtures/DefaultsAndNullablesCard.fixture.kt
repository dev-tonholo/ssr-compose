package fixtures.consumer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

enum class Priority {
    low,
    high,
}

@ConsumerComposable
@Composable
fun DefaultsAndNullablesCard(
    requiredNullableText: String?,
    optionalText: String = "Welcome",
    optionalNullableText: String? = null,
    optionalInt: Int = 1,
    nullableInt: Int?,
    optionalLong: Long = 2L,
    nullableLong: Long?,
    optionalFloat: Float = 1.5f,
    nullableFloat: Float?,
    optionalDouble: Double = 2.5,
    nullableDouble: Double?,
    optionalFlag: Boolean = false,
    nullableFlag: Boolean?,
    optionalChoice: Priority = Priority.low,
    nullableChoice: Priority?,
    optionalList: List<String> = emptyList(),
    nullableList: List<String>?,
    optionalNullableList: List<Priority>? = null,
) {
}
