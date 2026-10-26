@file:OptIn(ExperimentalMaterial3Api::class)

package cl.mastermartini.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cl.mastermartini.app.domain.Video
import cl.mastermartini.app.nativeaccess.openUrl
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val LocalHaptics = compositionLocalOf { true }

fun formatDate(ms: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CL")).format(Date(ms))

@Composable
fun ValidatedField(
    label: String, value: String, onChange: (String) -> Unit,
    error: String?, valid: Boolean, icon: ImageVector,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false, singleLine: Boolean = true, minLines: Int = 1,
    imeAction: ImeAction = ImeAction.Next
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = {
            when {
                password -> IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña")
                }
                error != null -> Icon(Icons.Filled.Error, "Campo con error", tint = MaterialTheme.colorScheme.error)
                valid -> Icon(Icons.Filled.CheckCircle, "Campo válido", tint = Color(0xFF2E7D32))
                else -> {}
            }
        },
        isError = error != null,
        supportingText = { AnimatedVisibility(visible = error != null) { Text(error.orEmpty()) } },
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard, imeAction = imeAction),
        singleLine = singleLine, minLines = minLines,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    icon: ImageVector,
    onSelect: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = open,
        onExpandedChange = { open = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(icon, null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = open,
            onDismissRequest = { open = false }
        ) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(o) },
                    onClick = {
                        onSelect(o)
                        open = false
                    }
                )
            }
        }
    }
}
@Composable
fun VideoPreview(url: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val thumb = Video.thumbnail(url)
    Box(
        modifier.fillMaxWidth().aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp)).background(Color.Black)
            .clickable(onClickLabel = "Abrir video") { openUrl(ctx, url) },
        contentAlignment = Alignment.Center
    ) {
        if (thumb != null) AsyncImage(model = thumb, contentDescription = "Vista previa del video",
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Icon(Icons.Filled.PlayCircle, "Reproducir video", tint = Color.White, modifier = Modifier.size(64.dp))
    }
}

@Composable
fun SectionTitle(text: String) =
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
