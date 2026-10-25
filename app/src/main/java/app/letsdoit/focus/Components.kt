package app.letsdoit.focus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Muted) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.2.sp
    )
}

// Uses the app's real launcher logo (adaptive foreground vector on its icon background).
@Composable
fun LogoMark(size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.24f))
            .background(colorResource(R.color.ic_bg))
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_fg),
            contentDescription = "Let's Do It",
            modifier = Modifier
                .fillMaxSize()
                .scale(1.6f)
        )
    }
}

@Composable
fun LCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RCard)
            .background(Sand)
            .border(1.dp, Line, RCard)
            .padding(18.dp),
        content = content
    )
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RSmall)
            .background(if (selected) Ink else Sand)
            .border(1.dp, if (selected) Ink else Line, RSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text,
            color = if (selected) Sand else Ink,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun PButton(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    ink: Color = Ink,
    paper: Color = Cream,
    onClick: () -> Unit
) {
    val base = modifier
        .clip(RBox)
        .background(if (filled) ink else Color.Transparent)
    val bordered = if (filled) base else base.border(1.dp, ink, RBox)
    Box(
        bordered
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (filled) paper else ink,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {}
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RBox)
                    .background(Sand)
                    .border(1.dp, Line, RBox)
                    .padding(horizontal = 16.dp, vertical = 15.dp)
            ) {
                if (value.isEmpty()) {
                    Text(hint, color = Muted, style = MaterialTheme.typography.bodyLarge)
                }
                inner()
            }
        }
    )
}

@Composable
fun RoundBtn(sym: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RSmall)
            .background(Sand)
            .border(1.dp, Line, RSmall)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(sym, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun CheckCircle(checked: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        Modifier
            .size(26.dp)
            .clip(shape)
            .background(if (checked) Accent else Color.Transparent)
            .border(1.5.dp, if (checked) Accent else Muted, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Done",
                tint = Sand,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RBox)
            .background(Sand)
            .border(1.dp, Line, RBox)
            .padding(16.dp)
    ) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Muted)
    }
}
