package cl.mastermartini.app.data

import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class Repository(private val db: AppDatabase) {

    private fun hash(email: String, pass: String): String =
        MessageDigest.getInstance("SHA-256").digest("mm|${email.lowercase()}|$pass".toByteArray())
            .joinToString("") { "%02x".format(it) }

    suspend fun login(email: String, pass: String): UserEntity? {
        val u = db.users().byEmail(email.trim().lowercase()) ?: return null
        return if (u.passwordHash == hash(u.email, pass)) u else null
    }

    suspend fun emailExists(email: String) = db.users().byEmail(email.trim().lowercase()) != null

    suspend fun register(u: UserEntity, plainPassword: String): UserEntity {
        val email = u.email.trim().lowercase()
        val e = u.copy(email = email, passwordHash = hash(email, plainPassword))
        return e.copy(id = db.users().insert(e))
    }

    fun observeUser(id: Long) = db.users().observe(id)
    fun observeClients() = db.users().observeClients()
    suspend fun saveQuiz(id: Long, answers: String) = db.users().saveQuiz(id, answers)
    suspend fun savePhoto(id: Long, path: String?) = db.users().savePhoto(id, path)

    fun observeRecipes() = db.recipes().observeAll()
    fun observeRecipe(id: Long) = db.recipes().observe(id)
    suspend fun addRecipe(r: RecipeEntity) = db.recipes().insert(r)
    suspend fun deleteRecipe(id: Long) { db.favorites().deleteForRecipe(id); db.recipes().delete(id) }

    fun observePosts() = db.posts().observeAll()
    suspend fun addPost(p: PostEntity) = db.posts().insert(p)
    suspend fun deletePost(id: Long) = db.posts().delete(id)

    fun observeFavorites(userId: Long): Flow<Set<Long>> = db.favorites().observe(userId).map { it.toSet() }
    suspend fun toggleFavorite(userId: Long, recipeId: Long, isFav: Boolean) {
        if (isFav) db.favorites().delete(userId, recipeId) else db.favorites().insert(FavoriteEntity(userId, recipeId))
    }

    suspend fun seedIfEmpty() {
        if (db.users().count() > 0) return
        fun u(name: String, email: String, pass: String, role: String, phone: String,
              rut: String = "", bt: String = "", bs: String = "", quiz: String = "") =
            UserEntity(name = name, email = email, passwordHash = hash(email, pass), role = role,
                phone = phone, rut = rut, businessType = bt, businessSize = bs, quizAnswers = quiz)

        val chefId = db.users().insert(u("Chef Demo", "chef@demo.cl", "Chef1234", "CHEF", "+56 9 1111 1111"))
        db.users().insert(u("Marketing Demo", "mkt@demo.cl", "Mkt12345", "MARKETING", "+56 9 2222 2222"))
        db.users().insert(u("Cliente Demo", "cliente@demo.cl", "Cliente123", "CLIENT", "+56 9 3333 3333",
            "12.345.678-5", "Panadería", "Pequeño"))
        db.users().insert(u("Ana Pastelera", "ana@demo.cl", "Cliente123", "CLIENT", "+56 9 4444 4444",
            "12.345.678-5", "Pastelería", "Mediano",
            "0:Videos técnicos|1:Chocolatería;Pastelería de vitrina|2:Intermedio|3:Cada semana|4:WhatsApp"))
        db.users().insert(u("Luis Heladero", "luis@demo.cl", "Cliente123", "CLIENT", "+56 9 5555 5555",
            "12.345.678-5", "Heladería", "Micro",
            "0:Recetas simples|1:Heladería|2:Estoy comenzando|3:Una vez al mes|4:Correo electrónico"))
        db.users().insert(u("Rosa Panadera", "rosa@demo.cl", "Cliente123", "CLIENT", "+56 9 6666 6666",
            "12.345.678-5", "Panadería", "Micro",
            "0:Cursos de especialidad|1:Bollería;Masas de hoja;Empanadas|2:Profesional|3:Cada 15 días|4:Llamada telefónica"))

        val coffeeBreakVideo = "https://www.youtube.com/watch?v=u-PrzOYszH4"
        val temptVideo = "https://www.youtube.com/watch?v=Wjja1iEHAks"
        val gallinaVideo = "https://www.youtube.com/watch?v=1pTyQObs0DE"
        db.recipes().insert(RecipeEntity(title = "Postre individual con sabor a chocolate, café y avellanas", category = "Chocolatería",
            description = "Master Martini y la chef Rita Busalacchi presentan Coffee Break elaborada con la crema 100% vegetal sin lactosa y chocolate con auténtico sabor a chocolate con leche.",
            ingredients = "100gr Agua\n34gr Chocolate Master Martini 22/24 MG\n25gr Azúcar Blanco\n2gr Pectina\n22gr Café Soluble\n110gr Jarabe de glucosa 42DE\n34gr Master Martini Plant Based Dual de avellanas",
            steps = "Poner todos los ingredientes a derretir a baño maría hasta llegar a ebulalción batiendo ligeramente. Hirviendo nos aseguramos que todos los olores de nuestra preparación se desarrollen y se mezclen, así como activamos la pectina, que nos dará la consistencia final y permitirá una estructura no líquida. Si quieres que quede líquida, puedes no agregarla\nUna vez que hierva, introduce la mezcla a una jarra y vierte una parte en un molde.\nDespués hay que hacer un merengue con aqua faba ???",
            videoUrl = coffeeBreakVideo, authorId = chefId, authorName = "Chef Demo"))


        db.recipes().insert(RecipeEntity(title = "Masa Quebrada de Avellanas", category = "Masas de hoja",
            description = "Master Martini y el chef Valerio Barralis presentan Tentación de Invierno elaborado con Caravella Gran Mora.",
            ingredients = "135gr Margarina Hoja Master Martini\nRalladura de una maranja\n56gr huevos\n38gr Polvo de avellanas.\n68gr Harina floja\n198gr harina floja",
            steps = "Son 17 minutos de vídeo",
            videoUrl = temptVideo, authorId = chefId, authorName = "Chef Demo"))


        db.posts().insert(PostEntity(title = "¡Bienvenidos a Master Martini!",
            body = "Aquí encontrarás recetas, videos técnicos y cursos para tu negocio gastronómico.",
            authorName = "Marketing Demo"))
        db.posts().insert(PostEntity(title = "Gallina de Chocolate con el chef Maurizio Frau",
            body = "Observa como el chef Maurizio Frau crea una gallina de chocolate a mano paso a paso.",
            videoUrl = gallinaVideo, authorName = "Marketing Demo"))
    }
}
