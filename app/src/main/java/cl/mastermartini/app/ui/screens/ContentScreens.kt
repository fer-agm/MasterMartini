@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package cl.mastermartini.app.ui.screens

import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cl.mastermartini.app.data.PostEntity
import cl.mastermartini.app.data.RecipeEntity
import cl.mastermartini.app.data.UserEntity
import cl.mastermartini.app.domain.Categories
import cl.mastermartini.app.domain.UserRole
import cl.mastermartini.app.domain.Video
import cl.mastermartini.app.nativeaccess.buzz
import cl.mastermartini.app.nativeaccess.openUrl
import cl.mastermartini.app.ui.components.*
import cl.mastermartini.app.viewmodel.ContentViewModel
import coil.compose.AsyncImage
import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat



@Composable
fun FeedScreen(user: UserEntity, vm: ContentViewModel, onNewPost: () -> Unit, onEditPost: (Long) -> Unit, onQuiz: () -> Unit) {
    val posts by vm.posts.collectAsState()
    val role = UserRole.valueOf(user.role)
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Hola, ${user.name.substringBefore(' ')}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            if (role == UserRole.CLIENT) item {
                AnimatedVisibility(user.quizAnswers.isBlank()) {
                    Card(onClick = onQuiz, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Quiz, null)
                            Column {
                                Text("Cuéntanos tus intereses", fontWeight = FontWeight.Bold)
                                Text("Responde una breve encuesta para recibir contenido a tu medida.")
                            }
                        }
                    }
                }
            }
            if (posts.isEmpty()) item { Text("Aún no hay publicaciones.") }
            items(posts, key = { it.id }) { p ->
                PostCard(p, canManage = role == UserRole.MARKETING,
                    onEdit = { vm.prepareEditPost(p); onEditPost(p.id) },
                    onDelete = { vm.deletePost(p.id) }, modifier = Modifier.animateItem())
            }
        }
        if (role == UserRole.MARKETING) {
            ExtendedFloatingActionButton(
                onClick = onNewPost, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Nueva publicación") }
            )
        }
    }
}

@Composable
private fun PostCard(p: PostEntity, canManage: Boolean, onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (canManage) {
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Editar publicación") }
                    IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Eliminar publicación") }
                }
            }
            if (p.videoUrl.isNotBlank()) VideoPreview(p.videoUrl)
            Text(p.body, style = MaterialTheme.typography.bodyLarge)
            Text("${p.authorName} · ${formatDate(p.createdAt)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable
fun PostFormScreen(user: UserEntity, vm: ContentViewModel, editing: PostEntity? = null, onDone: () -> Unit) {
    val f = vm.postForm
    val ctx = LocalContext.current
    val haptics = LocalHaptics.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ValidatedField("Título", f.value("title"), { f.set("title", it) }, f.error("title"), f.isValid("title"), Icons.Filled.Title)
        ValidatedField("Mensaje", f.value("body"), { f.set("body", it) }, f.error("body"), f.isValid("body"),
            Icons.Filled.Description, singleLine = false, minLines = 4)
        ValidatedField("Enlace de video (opcional)", f.value("videoUrl"), { f.set("videoUrl", it) }, f.error("videoUrl"), f.isValid("videoUrl"),
            Icons.Filled.Link, imeAction = ImeAction.Done)
        VideoHint(f.value("videoUrl"))
        Button(onClick = {
            if (editing == null) vm.savePost(user, { buzz(ctx, haptics) }, onDone)
            else vm.updatePost(editing, { buzz(ctx, haptics) }, onDone)
        }, Modifier.fillMaxWidth().height(56.dp)) {
            Text(if (editing == null) "Publicar" else "Guardar cambios")
        }
    }
}

@Composable
private fun VideoHint(url: String) {
    AnimatedVisibility(Video.id(url) != null) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Vista previa (solo se guarda el enlace):", style = MaterialTheme.typography.bodySmall)
            VideoPreview(url)
        }
    }
}

// ───────────────────────── RECETAS ─────────────────────────

@Composable
fun RecipesScreen(user: UserEntity, vm: ContentViewModel, onOpen: (Long) -> Unit, onNew: () -> Unit) {
    val recipes by vm.recipes.collectAsState()
    val favs by remember(user.id) { vm.favorites(user.id) }.collectAsState(emptySet())
    var query by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val text = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (res.resultCode == Activity.RESULT_OK && text != null) query = text
    }
    fun startVoice(){
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CL")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di una receta")
        }
        try { voice.launch(i) }
        catch (e:ActivityNotFoundException){
            Toast.makeText(ctx, "Este dispositivo no tiene reconocimiento de voz", Toast.LENGTH_SHORT).show()
        }
    }
    val micPermission = rememberLauncherForActivityResult( ActivityResultContracts.RequestPermission()) {granted ->
        if (granted) startVoice()
        else Toast.makeText(ctx, "Debes habilitar el permiso para usar el micrófono", Toast.LENGTH_SHORT).show()
    }

    var filter by remember { mutableStateOf("Todas") }
    val canCreate = UserRole.valueOf(user.role) != UserRole.CLIENT

    val shown = recipes.filter { r ->
        val byFilter = when (filter) { "Todas" -> true; "Favoritas" -> r.id in favs; else -> r.category == filter }
        byFilter && (query.isBlank() || r.title.contains(query, true) || r.description.contains(query, true))
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                label = { Text("Buscar receta") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 0.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startVoice()
                        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }) { Icon(Icons.Filled.Mic, contentDescription = "Buscar por voz") }
                },
            )
            LazyRow(contentPadding = PaddingValues(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Todas", "Favoritas") + Categories.recipe) { c ->
                    FilterChip(selected = filter == c, onClick = { filter = c }, label = { Text(c) })
                }
            }
            LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (shown.isEmpty()) item { Text("No hay recetas para este filtro.") }
                items(shown, key = { it.id }) { r ->
                    RecipeCard(r, isFav = r.id in favs, onFav = { vm.toggleFavorite(user.id, r.id, r.id in favs) },
                        onClick = { onOpen(r.id) }, modifier = Modifier.animateItem())
                }
            }
        }
        if (canCreate) ExtendedFloatingActionButton(
            onClick = onNew, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Nueva receta") }
        )
    }
}

@Composable
private fun RecipeCard(r: RecipeEntity, isFav: Boolean, onFav: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(if (isFav) 1.25f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "favScale")
    val tint by animateColorAsState(if (isFav) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant, label = "favColor")
    val thumb = Video.thumbnail(r.videoUrl)
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                if (thumb != null) AsyncImage(thumb, "Miniatura del video de ${r.title}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Filled.Restaurant, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(r.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(r.category, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(r.description, maxLines = 2, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onFav) {
                Icon(if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    if (isFav) "Quitar de favoritas" else "Marcar como favorita",
                    tint = tint, modifier = Modifier.scale(scale))
            }
        }
    }
}

@Composable
fun RecipeDetailScreen(id: Long, user: UserEntity, vm: ContentViewModel, onEdit:() -> Unit, onBack: () -> Unit) {
    val recipe by remember(id) { vm.recipe(id) }.collectAsState(null)
    val ctx = LocalContext.current
    val r = recipe ?: return
    val canDelete = UserRole.valueOf(user.role) == UserRole.MARKETING || r.authorId == user.id
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (r.videoUrl.isNotBlank()) {
            VideoPreview(r.videoUrl)
            OutlinedButton(onClick = { openUrl(ctx, r.videoUrl) }, Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Filled.PlayCircle, null); Spacer(Modifier.width(8.dp)); Text("Ver video completo")
            }
        }
        Text(r.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("${r.category} · por ${r.authorName} · ${formatDate(r.createdAt)}", style = MaterialTheme.typography.bodySmall)
        Text(r.description, style = MaterialTheme.typography.bodyLarge)
        SectionTitle("Ingredientes")
        r.ingredients.lines().filter { it.isNotBlank() }.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
        SectionTitle("Preparación")
        r.steps.lines().filter { it.isNotBlank() }.forEachIndexed { i, s -> Text("${i + 1}. $s", style = MaterialTheme.typography.bodyLarge) }
        if (canDelete) Button(onClick = { vm.prepareEdit(r); onEdit() }, Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(8.dp)); Text("Editar receta")
        }
        if (canDelete) OutlinedButton(onClick = { vm.deleteRecipe(r.id); onBack() }, Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Delete, null); Spacer(Modifier.width(8.dp)); Text("Eliminar receta")
        }
    }
}

@Composable
fun RecipeFormScreen(user: UserEntity, vm: ContentViewModel,editing: RecipeEntity? = null, onDone: () -> Unit) {
    val f = vm.recipeForm
    val ctx = LocalContext.current
    val haptics = LocalHaptics.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ValidatedField("Título", f.value("title"), { f.set("title", it) }, f.error("title"), f.isValid("title"), Icons.Filled.Title)
        DropdownField("Categoría", f.value("category"), Categories.recipe, Icons.Filled.Category) { f.set("category", it) }
        ValidatedField("Descripción", f.value("description"), { f.set("description", it) }, f.error("description"), f.isValid("description"),
            Icons.Filled.Description, singleLine = false, minLines = 2)
        ValidatedField("Ingredientes (uno por línea)", f.value("ingredients"), { f.set("ingredients", it) }, f.error("ingredients"), f.isValid("ingredients"),
            Icons.Filled.Restaurant, singleLine = false, minLines = 3)
        ValidatedField("Pasos (uno por línea)", f.value("steps"), { f.set("steps", it) }, f.error("steps"), f.isValid("steps"),
            Icons.Filled.FormatListNumbered, singleLine = false, minLines = 3)
        ValidatedField("Enlace de video (YouTube u otro, opcional)", f.value("videoUrl"), { f.set("videoUrl", it) }, f.error("videoUrl"), f.isValid("videoUrl"),
            Icons.Filled.Link, imeAction = ImeAction.Done)
        VideoHint(f.value("videoUrl"))
        Button(onClick = {
            if (editing == null) vm.saveRecipe(user, { buzz(ctx, haptics) }, onDone)
            else vm.updateRecipe(editing, { buzz(ctx, haptics) }, onDone)
        }, Modifier.fillMaxWidth().height(56.dp)) {
            Text(if (editing == null) "Guardar receta" else "Guardar cambios")
        }    }
}
