@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package cl.mastermartini.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cl.mastermartini.app.R
import cl.mastermartini.app.data.Prefs
import cl.mastermartini.app.data.UserEntity
import cl.mastermartini.app.domain.QuizData
import cl.mastermartini.app.domain.UserRole
import cl.mastermartini.app.nativeaccess.*
import cl.mastermartini.app.ui.components.*
import cl.mastermartini.app.viewmodel.ContentViewModel
import cl.mastermartini.app.viewmodel.ProfileViewModel
import cl.mastermartini.app.viewmodel.SettingsViewModel
import coil.compose.AsyncImage
import java.io.File
import kotlin.math.abs

// ───────────────────────── PERFIL ─────────────────────────

@Composable
fun ProfileScreen(
    user: UserEntity, vm: ProfileViewModel,
    onQuiz: () -> Unit, onContact: () -> Unit, onDashboard: () -> Unit, onLogout: () -> Unit
) {
    val ctx = LocalContext.current
    val role = UserRole.valueOf(user.role)

    // Recurso nativo: CÁMARA, con solicitud de permiso en tiempo de ejecución y manejo de rechazo.
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) vm.savePhoto(user, bmp)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) camera.launch(null)
        else Toast.makeText(ctx, "Sin permiso de cámara no se puede tomar la foto de perfil", Toast.LENGTH_LONG).show()
    }
    fun takePhoto() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera.launch(null)
        else permission.launch(Manifest.permission.CAMERA)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            if (user.photoPath != null) AsyncImage(model = File(user.photoPath), contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Icon(Icons.Filled.Person, "Sin foto de perfil", Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        OutlinedButton(onClick = { takePhoto() }) {
            Icon(Icons.Filled.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Tomar foto de perfil")
        }
        Text(user.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        AssistChip(onClick = {}, label = { Text("Perfil: ${role.label}") })

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle("Mis datos")
                Text("Correo: ${user.email}"); Text("Teléfono: ${user.phone}")
                if (role == UserRole.CLIENT) {
                    Text("RUT empresa: ${user.rut}"); Text("Negocio: ${user.businessType} (${user.businessSize})")
                }
            }
        }

        if (role == UserRole.CLIENT) Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Mis intereses")
                val areas = QuizData.decode(user.quizAnswers)[1].orEmpty()
                if (areas.isEmpty()) Text("Aún no respondes la encuesta.")
                else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { areas.forEach { AssistChip(onClick = {}, label = { Text(it) }) } }
                Button(onClick = onQuiz, Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Filled.Quiz, null); Spacer(Modifier.width(8.dp))
                    Text(if (user.quizAnswers.isBlank()) "Responder encuesta" else "Rehacer encuesta")
                }
            }
        }

        if (role == UserRole.MARKETING) Button(onClick = onDashboard, Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.Dashboard, null); Spacer(Modifier.width(8.dp)); Text("Panel de segmentación de clientes")
        }
        OutlinedButton(onClick = onContact, Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.SupportAgent, null); Spacer(Modifier.width(8.dp)); Text("Contacto de la marca")
        }
        TextButton(onClick = onLogout) {
            Icon(Icons.AutoMirrored.Filled.Logout, null); Spacer(Modifier.width(8.dp)); Text("Cerrar sesión")
        }
    }
}

// ───────────────────────── ENCUESTA ─────────────────────────

@Composable
fun QuizScreen(user: UserEntity, vm: ProfileViewModel, onDone: () -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.startQuiz() }
    val total = QuizData.questions.size
    val progress by animateFloatAsState((vm.step + 1f) / total, label = "quizProgress")

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pregunta ${vm.step + 1} de $total", style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))

        AnimatedContent(
            targetState = vm.step, modifier = Modifier.weight(1f),
            transitionSpec = {
                if (targetState > initialState) (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                else (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            }, label = "quizStep"
        ) { step ->
            val q = QuizData.questions[step]
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(q.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                q.options.forEachIndexed { i, opt ->
                    OptionRow(opt, selected = i in vm.selected[step].orEmpty(), multi = q.multi) { vm.toggle(i) }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { vm.back() }, enabled = vm.step > 0, modifier = Modifier.weight(1f).height(52.dp)) { Text("Atrás") }
            Button(
                onClick = {
                    vm.next(user.id) {
                        Toast.makeText(ctx, "¡Gracias! Guardamos tus intereses", Toast.LENGTH_SHORT).show(); onDone()
                    }
                },
                enabled = vm.canContinue, modifier = Modifier.weight(1f).height(52.dp)
            ) { Text(if (vm.step == total - 1) "Finalizar" else "Siguiente") }
        }
    }
}

@Composable
private fun OptionRow(text: String, selected: Boolean, multi: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, label = "optBorder")
    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, label = "optBg")
    Surface(
        shape = MaterialTheme.shapes.medium, color = bg,
        border = BorderStroke(if (selected) 2.dp else 1.dp, border),
        modifier = Modifier.fillMaxWidth().selectable(selected, onClick = onClick, role = if (multi) Role.Checkbox else Role.RadioButton)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (multi) Checkbox(selected, onCheckedChange = null) else RadioButton(selected, onClick = null)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ───────────────────────── ACCESIBILIDAD ─────────────────────────

@Composable
fun AccessibilityScreen(prefs: Prefs, vm: SettingsViewModel) {
    val scales = listOf(1f, 1.15f, 1.3f, 1.45f, 1.6f)
    val idx = scales.indexOfFirst { abs(it - prefs.textScale) < 0.01f }.coerceAtLeast(0)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Hazla cómoda para ti", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        SectionTitle("Tamaño del texto")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { vm.setScale(scales[idx - 1]) }, enabled = idx > 0, modifier = Modifier.weight(1f).height(56.dp)) { Text("A −  Más pequeño") }
            Text("${(scales[idx] * 100).toInt()}%", fontWeight = FontWeight.Bold)
            Button(onClick = { vm.setScale(scales[idx + 1]) }, enabled = idx < scales.lastIndex, modifier = Modifier.weight(1f).height(56.dp)) { Text("A +  Más grande") }
        }
        Card(Modifier.fillMaxWidth()) {
            Text("Así se verá el texto en toda la aplicación.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
        }
        SwitchRow("Alto contraste", "Colores más marcados para leer mejor", Icons.Filled.Contrast, prefs.highContrast) { vm.setContrast(it) }
        SwitchRow("Vibración ante errores", "El teléfono vibra si un formulario tiene errores", Icons.Filled.Vibration, prefs.haptics) { vm.setHaptics(it) }
        OutlinedButton(onClick = { vm.resetAll() }, Modifier.fillMaxWidth().height(52.dp)) { Text("Restablecer valores") }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.toggleable(checked, onValueChange = onChange, role = Role.Switch).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null)
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall) }
            Switch(checked, onCheckedChange = null)
        }
    }
}

// ───────────────────────── CONTACTO DE LA MARCA ─────────────────────────

private const val BRAND_PHONE = "+56 2 26611700"
private const val BRAND_EMAIL = "clientes@mastermartini.cl"
private const val BRAND_ADDRESS = "Avenida Américo Vespucio 1385.\n" +
        "Centro SpacioFlex, Módulo 3.\n" +
        "Quilicura, Santiago."
private const val BRAND_WEB = "https://mastermartini.cl/retail/"
private const val BRAND_WHATSAPP = "+56 9 4457 0770"

@Composable
fun ContactScreen(user: UserEntity, vm: ContentViewModel, openWhatsApp: (Context, String, String) -> Unit) {
    val f = vm.contactForm
    val ctx = LocalContext.current
    val haptics = LocalHaptics.current
    LaunchedEffect(Unit) { if (f.value("name").isBlank()) { f.set("name", user.name); f.set("email", user.email) } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Master Martini Chile SPA", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Insumos para alimentos · Escríbenos o llámanos", style = MaterialTheme.typography.bodyLarge)
        Card(Modifier.fillMaxWidth()) {
            Column {
                ContactRow(Icons.Filled.Phone, "Llamar", BRAND_PHONE) { dial(ctx, BRAND_PHONE) }
                ContactRow(Icons.Filled.Email, "Correo", BRAND_EMAIL) { sendEmail(ctx, BRAND_EMAIL, "Consulta desde la app") }
                ContactRow(Icons.Filled.Place, "Cómo llegar", BRAND_ADDRESS) { openMap(ctx, BRAND_ADDRESS) }
                ContactRow(Icons.Filled.Language, "Sitio web", BRAND_WEB) { openUrl(ctx, BRAND_WEB) }

                ContactRow(painterResource(R.drawable.logo_wsp), "Whatsapp", BRAND_WHATSAPP) {
                    openWhatsApp(ctx, BRAND_WHATSAPP, "Hola Master Martini, tengo una consulta...")
                }
            }
        }

        SectionTitle("Envíanos un mensaje")
        ValidatedField("Nombre", f.value("name"), { f.set("name", it) }, f.error("name"), f.isValid("name"), Icons.Filled.Person)
        ValidatedField("Correo", f.value("email"), { f.set("email", it) }, f.error("email"), f.isValid("email"), Icons.Filled.Email,
            keyboard = androidx.compose.ui.text.input.KeyboardType.Email)
        ValidatedField("Mensaje", f.value("message"), { f.set("message", it) }, f.error("message"), f.isValid("message"),
            Icons.Filled.Description, singleLine = false, minLines = 4, imeAction = ImeAction.Done)
        Button(
            onClick = {
                if (f.validateAll()) {
                    sendEmail(ctx, BRAND_EMAIL, "Consulta de ${f.value("name")}", f.value("message") + "\n\nContacto: " + f.value("email"))
                    f.set("message", ""); 
                } else buzz(ctx, haptics)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Icon(Icons.Filled.Send, null); Spacer(Modifier.width(8.dp)); Text("Enviar mensaje") }
    }
}

@Composable
private fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(value) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.clickable(onClickLabel = title, onClick = onClick)
    )
}
@Composable
private fun ContactRow(painter: androidx.compose.ui.graphics.painter.Painter, title: String, value: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(value) },
        leadingContent = { Icon(painter, null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.clickable(onClickLabel = title, onClick = onClick)
    )
}

// ───────────────────────── PANEL MARKETING ─────────────────────────

@Composable
fun DashboardScreen(vm: ContentViewModel) {
    val clients by vm.clients.collectAsState()
    var area by remember { mutableStateOf<String?>(null) }
    val answered = clients.count { it.quizAnswers.isNotBlank() }
    val byType = clients.groupingBy { it.businessType.ifBlank { "Sin dato" } }.eachCount()
    val byArea = clients.flatMap { QuizData.decode(it.quizAnswers)[1].orEmpty() }.groupingBy { it }.eachCount()
    val filtered = if (area == null) clients else clients.filter { area in QuizData.decode(it.quizAnswers)[1].orEmpty() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Clientes registrados: ${clients.size} · Encuesta respondida: $answered", fontWeight = FontWeight.Bold)
        SectionTitle("Por tipo de negocio")
        byType.entries.sortedByDescending { it.value }.forEach { (k, v) -> BarRow(k, v, clients.size.coerceAtLeast(1)) }
        SectionTitle("Por área de interés (toca para filtrar)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            byArea.entries.sortedByDescending { it.value }.forEach { (k, v) ->
                FilterChip(selected = area == k, onClick = { area = if (area == k) null else k }, label = { Text("$k ($v)") })
            }
        }
        SectionTitle(if (area == null) "Todos los clientes" else "Clientes interesados en $area")
        filtered.forEach { c ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(c.name, fontWeight = FontWeight.Bold)
                    Text("${c.businessType} · ${c.businessSize}", style = MaterialTheme.typography.bodySmall)
                    Text("${c.email} · ${c.phone}", style = MaterialTheme.typography.bodySmall)
                    Text("Prefiere: ${QuizData.decode(c.quizAnswers)[4].orEmpty().firstOrNull() ?: "sin respuesta"}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun BarRow(label: String, count: Int, max: Int) {
    val p by animateFloatAsState(count.toFloat() / max, label = "bar")
    Column {
        Text("$label: $count")
        LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape))
    }
}
