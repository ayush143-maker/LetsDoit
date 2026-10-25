package app.letsdoit.focus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PageHeader(title: String, subtitle: String, subtitleBold: Boolean = false) {
    Column(Modifier.padding(top = 22.dp, bottom = 20.dp)) {
        Text(title, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            fontWeight = if (subtitleBold) FontWeight.SemiBold else FontWeight.Normal,
            color = Muted
        )
    }
}
