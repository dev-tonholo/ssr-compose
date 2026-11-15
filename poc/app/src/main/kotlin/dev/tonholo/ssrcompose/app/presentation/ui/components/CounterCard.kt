package dev.tonholo.ssrcompose.app.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.tonholo.ssrcompose.app.presentation.ui.Dimens
import dev.tonholo.ssrcompose.protocol.ActionId
import dev.tonholo.ssrcompose.protocol.CounterCardProps

@Composable
fun CounterCard(props: CounterCardProps, onAction: (ActionId) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(all = Dimens.CardPadding)) {
            Text(text = props.title, style = MaterialTheme.typography.titleMedium)
            Text(text = "Count: ${props.count}", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { onAction(props.onAddOne) }) { Text(text = "Add one") }
        }
    }
}
