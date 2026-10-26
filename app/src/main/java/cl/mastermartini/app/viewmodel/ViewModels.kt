package cl.mastermartini.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.mastermartini.app.container
import cl.mastermartini.app.data.*
import cl.mastermartini.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

sealed interface Session {
    data object Loading : Session
    data object Out : Session
    data class In(val user: UserEntity) : Session
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.container.repo
    private val settings = app.container.settings

    val loginForm = Forms.login()
    val registerForm = Forms.register()
    var error by mutableStateOf<String?>(null); private set
    var loading by mutableStateOf(false); private set

    @OptIn(ExperimentalCoroutinesApi::class)
    val session: StateFlow<Session> = settings.prefs.map { it.userId }.distinctUntilChanged()
        .flatMapLatest { id ->
            if (id < 0) flowOf<Session>(Session.Out)
            else repo.observeUser(id).map<UserEntity?, Session> { u -> if (u == null) Session.Out else Session.In(u) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Session.Loading)

    fun clearError() { error = null }

    fun login(onInvalid: () -> Unit) {
        if (!loginForm.validateAll()) { onInvalid(); return }
        viewModelScope.launch {
            loading = true
            val u = repo.login(loginForm.value("email"), loginForm.value("password"))
            if (u == null) { error = "Correo o contraseña incorrectos"; onInvalid() }
            else { error = null; settings.setUser(u.id); loginForm.reset() }
            loading = false
        }
    }

    fun register(onInvalid: () -> Unit) {
        val f = registerForm
        if (!f.validateAll()) { onInvalid(); return }
        viewModelScope.launch {
            loading = true
            if (repo.emailExists(f.value("email"))) {
                error = "Ese correo ya está registrado"; onInvalid()
            } else {
                val role = f.value("role")
                val client = role == UserRole.CLIENT.name
                val u = repo.register(
                    UserEntity(
                        name = f.value("name").trim(), email = f.value("email"), passwordHash = "",
                        role = role, phone = f.value("phone"),
                        rut = if (client) f.value("rut") else "",
                        businessType = if (client) f.value("businessType") else "",
                        businessSize = if (client) f.value("businessSize") else ""
                    ), f.value("password")
                )
                error = null
                settings.setUser(u.id)
                registerForm.reset()
            }
            loading = false
        }
    }

    fun logout() { viewModelScope.launch { settings.setUser(-1) } }
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val s = app.container.settings
    val prefs: StateFlow<Prefs> = s.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, Prefs())
    fun setScale(v: Float) { viewModelScope.launch { s.setScale(v) } }
    fun setContrast(v: Boolean) { viewModelScope.launch { s.setContrast(v) } }
    fun setHaptics(v: Boolean) { viewModelScope.launch { s.setHaptics(v) } }
    fun resetAll() { viewModelScope.launch { s.setScale(1f); s.setContrast(false); s.setHaptics(true) } }
}

class ContentViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.container.repo
    private fun <T> Flow<T>.state(init: T) = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), init)

    val recipes = repo.observeRecipes().state(emptyList())
    val posts = repo.observePosts().state(emptyList())
    val clients = repo.observeClients().state(emptyList())

    val recipeForm = Forms.recipe()
    val postForm = Forms.post()
    val contactForm = Forms.contact()

    fun recipe(id: Long) = repo.observeRecipe(id)
    fun favorites(userId: Long) = repo.observeFavorites(userId)
    fun toggleFavorite(userId: Long, recipeId: Long, isFav: Boolean) {
        viewModelScope.launch { repo.toggleFavorite(userId, recipeId, isFav) }
    }

    fun saveRecipe(user: UserEntity, onInvalid: () -> Unit, onDone: () -> Unit) {
        val f = recipeForm
        if (!f.validateAll()) { onInvalid(); return }
        viewModelScope.launch {
            repo.addRecipe(RecipeEntity(
                title = f.value("title").trim(), category = f.value("category"),
                description = f.value("description").trim(), ingredients = f.value("ingredients").trim(),
                steps = f.value("steps").trim(), videoUrl = f.value("videoUrl").trim(),
                authorId = user.id, authorName = user.name))
            f.reset(); onDone()
        }
    }

    fun prepareEdit(r: RecipeEntity) {
        val f = recipeForm
        f.reset()
        f.set("title", r.title)
        f.set("category", r.category)
        f.set("description", r.description)
        f.set("ingredients", r.ingredients)
        f.set("steps", r.steps)
        f.set("videoUrl", r.videoUrl)
    }

    fun updateRecipe(original: RecipeEntity, onInvalid: () -> Unit, onDone: () -> Unit) {
        val f = recipeForm
        if (!f.validateAll()) { onInvalid(); return }
        viewModelScope.launch {
            // copy() conserva id, autor y fecha de creación
            repo.updateRecipe(original.copy(
                title = f.value("title").trim(), category = f.value("category"),
                description = f.value("description").trim(), ingredients = f.value("ingredients").trim(),
                steps = f.value("steps").trim(), videoUrl = f.value("videoUrl").trim()))
            f.reset(); onDone()
        }
    }

    fun savePost(user: UserEntity, onInvalid: () -> Unit, onDone: () -> Unit) {
        val f = postForm
        if (!f.validateAll()) { onInvalid(); return }
        viewModelScope.launch {
            repo.addPost(PostEntity(title = f.value("title").trim(), body = f.value("body").trim(),
                videoUrl = f.value("videoUrl").trim(), authorName = user.name))
            f.reset(); onDone()
        }
    }

    fun prepareEditPost(p: PostEntity) {
        val f = postForm
        f.reset()
        f.set("title", p.title)
        f.set("body", p.body)
        f.set("videoUrl", p.videoUrl)
    }

    fun updatePost(original: PostEntity, onInvalid: () -> Unit, onDone: () -> Unit) {
        val f = postForm
        if (!f.validateAll()) { onInvalid(); return }
        viewModelScope.launch {
            // copy() conserva id, autor y fecha original
            repo.updatePost(original.copy(
                title = f.value("title").trim(), body = f.value("body").trim(),
                videoUrl = f.value("videoUrl").trim()))
            f.reset(); onDone()
        }
    }

    fun deleteRecipe(id: Long) { viewModelScope.launch { repo.deleteRecipe(id) } }
    fun deletePost(id: Long) { viewModelScope.launch { repo.deletePost(id) } }
}

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = app.container.repo

    var step by mutableIntStateOf(0); private set
    val selected = mutableStateMapOf<Int, Set<Int>>()
    val canContinue: Boolean get() = selected[step].orEmpty().isNotEmpty()

    fun startQuiz() { step = 0; selected.clear() }
    fun toggle(opt: Int) {
        val cur = selected[step].orEmpty()
        selected[step] = if (QuizData.questions[step].multi) { if (opt in cur) cur - opt else cur + opt } else setOf(opt)
    }
    fun back() { if (step > 0) step-- }
    fun next(userId: Long, onDone: () -> Unit) {
        if (step < QuizData.questions.lastIndex) step++
        else viewModelScope.launch { repo.saveQuiz(userId, QuizData.encode(selected)); onDone() }
    }

    fun savePhoto(user: UserEntity, bmp: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(getApplication<Application>().filesDir, "profile_${user.id}_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            user.photoPath?.let { File(it).delete() }
            repo.savePhoto(user.id, file.absolutePath)
        }
    }
}
