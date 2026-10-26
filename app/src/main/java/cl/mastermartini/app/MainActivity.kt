@file:OptIn(ExperimentalMaterial3Api::class)

package cl.mastermartini.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import cl.mastermartini.app.data.UserEntity
import cl.mastermartini.app.ui.components.LocalHaptics
import cl.mastermartini.app.ui.screens.*
import cl.mastermartini.app.ui.theme.MartiniTheme
import cl.mastermartini.app.viewmodel.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings: SettingsViewModel = viewModel()
            val prefs by settings.prefs.collectAsState()
            MartiniTheme(prefs) {
                CompositionLocalProvider(LocalHaptics provides prefs.haptics) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { AppRoot(settings) }
                }
            }
        }
    }
}

@Composable
private fun AppRoot(settings: SettingsViewModel) {
    val auth: AuthViewModel = viewModel()
    val session by auth.session.collectAsState()
    when (val s = session) {
        Session.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        Session.Out -> AuthNav(auth)
        is Session.In -> MainNav(s.user, auth, settings)
    }
}

@Composable
private fun AuthNav(auth: AuthViewModel) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "login", enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
        composable("login") { LoginScreen(auth) { auth.clearError(); nav.navigate("register") } }
        composable("register") { RegisterScreen(auth) { auth.clearError(); nav.popBackStack() } }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("feed", "Inicio", Icons.Filled.Home),
    Tab("recipes", "Recetas", Icons.Filled.RestaurantMenu),
    Tab("profile", "Perfil", Icons.Filled.Person),
    Tab("accessibility", "Accesibilidad", Icons.Filled.Accessibility)
)

private fun titleFor(route: String?) = when {
    route == null -> ""
    route.startsWith("recipe/") -> "Receta"
    route.startsWith("recipe_edit/") -> "Editar receta"
    route.startsWith("post_edit/") -> "Editar publicación"
    else -> when (route) {
        "feed" -> "Master Martini"; "recipes" -> "Recetas"; "profile" -> "Mi perfil"
        "accessibility" -> "Accesibilidad"; "recipe_form" -> "Nueva receta"; "post_form" -> "Nueva publicación"
        "quiz" -> "Encuesta de intereses"; "contact" -> "Contacto"; "dashboard" -> "Panel de segmentación"
        else -> ""
    }
}
fun openWhatsApp(
    context: Context,
    phone: String,
    message: String = "Hola Master Martini, tengo una consulta..."
) {
    val number = phone.filter { it.isDigit() }
    val text = Uri.encode(message)
    val urls = listOf(
        "https://api.whatsapp.com/send?phone=$number&text=$text",
        "https://wa.me/$number?text=$text"
    )

    for (url in urls) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            return
        } catch (_: ActivityNotFoundException) {
        }
    }

    Toast.makeText(context, "No se pudo abrir WhatsApp en este dispositivo", Toast.LENGTH_SHORT).show()
}


@Composable
private fun MainNav(user: UserEntity, auth: AuthViewModel, settings: SettingsViewModel) {
    val nav = rememberNavController()
    val content: ContentViewModel = viewModel()
    val profile: ProfileViewModel = viewModel()
    val prefs by settings.prefs.collectAsState()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val isTab = tabs.any { it.route == route }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleFor(route)) },
                navigationIcon = {
                    if (!isTab) IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
                actions = {
                    if (route != "contact") IconButton(onClick = { nav.navigate("contact") }) { Icon(Icons.Filled.SupportAgent, "Contacto de la marca") }
                }
            )
        },
        bottomBar = {
            if (isTab) NavigationBar {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = { nav.navigate(t.route) { popUpTo("feed") { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(t.icon, null) }, label = { Text(t.label) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "feed", modifier = Modifier.padding(pad),
            enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
            composable("feed") {
                FeedScreen(user, content,
                    onNewPost = { content.postForm.reset(); nav.navigate("post_form") },   // limpia restos de una edición
                    onEditPost = { id -> nav.navigate("post_edit/$id") },
                    onQuiz = { nav.navigate("quiz") })
            }
            composable("post_edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments!!.getLong("id")
                val posts by content.posts.collectAsState()
                posts.firstOrNull { it.id == id }?.let { PostFormScreen(user, content, editing = it) { nav.popBackStack() } }
            }
            composable("recipes") { RecipesScreen(user, content, onOpen = { nav.navigate("recipe/$it") }, onNew = { content.recipeForm.reset(); nav.navigate("recipe_form") }) }
            composable("recipe/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments!!.getLong("id")
                RecipeDetailScreen(id, user, content, onEdit = { nav.navigate("recipe_edit/$id") }) { nav.popBackStack() }
            }
            composable("recipe_edit/{id}", arguments = listOf(navArgument("id") {type = NavType.LongType})) { entry ->
                val id = entry.arguments!!.getLong("id")
                val recipe by remember(id) { content.recipe(id) }.collectAsState(null)
                recipe?.let { RecipeFormScreen(user, content, editing = it) { nav.popBackStack() } }

            }
            composable("recipe_form") { RecipeFormScreen(user, content) { nav.popBackStack() } }
            composable("post_form") { PostFormScreen(user, content) { nav.popBackStack() } }
            composable("profile") {
                ProfileScreen(user, profile, onQuiz = { nav.navigate("quiz") }, onContact = { nav.navigate("contact") },
                    onDashboard = { nav.navigate("dashboard") }, onLogout = { auth.logout() })
            }
            composable("quiz") { QuizScreen(user, profile) { nav.popBackStack() } }
            composable("accessibility") { AccessibilityScreen(prefs, settings) }
         //   composable("contact") { ContactScreen(user, content,) }
            composable("dashboard") { DashboardScreen(content) }
            composable("contact") { ContactScreen(user, content, ::openWhatsApp) }
        }
    }
}
