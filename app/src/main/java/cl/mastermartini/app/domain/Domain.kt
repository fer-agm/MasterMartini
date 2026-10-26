package cl.mastermartini.app.domain

enum class UserRole(val label: String) {
    CLIENT("Cliente"), CHEF("Chef"), MARKETING("Marketing")
}

object Categories {
    val recipe = listOf(
        "Chocolatería", "Pastelería de vitrina", "Heladería", "Bollería",
        "Masas de hoja", "Galletas", "Empanadas", "Productos de horneo"
    )
    val businessTypes = listOf("Panadería", "Pastelería", "Chocolatería", "Heladería", "Casino / Restaurante", "Otro")
    val businessSizes = listOf("Micro", "Pequeño", "Mediano", "Grande")
}

object AccessCodes {
    const val CHEF = "CHEF2026"
    const val MARKETING = "MKT2026"
}

data class QuizQuestion(val text: String, val options: List<String>, val multi: Boolean = false)

object QuizData {
    val questions = listOf(
        QuizQuestion("¿Qué tipo de contenido prefieres?",
            listOf("Recetas simples", "Videos técnicos", "Cursos de especialidad")),
        QuizQuestion("¿Qué áreas te interesan? (Marca todas las que apliquen)", Categories.recipe, multi = true),
        QuizQuestion("¿Cuál es tu nivel de experiencia?",
            listOf("Estoy comenzando", "Intermedio", "Profesional")),
        QuizQuestion("¿Cómo prefieres que te contactemos?",
            listOf("WhatsApp", "Correo electrónico", "Llamada telefónica"))
    )

    fun encode(selected: Map<Int, Set<Int>>): String = questions.indices.joinToString("|") { i ->
        "$i:" + (selected[i] ?: emptySet()).sorted().joinToString(";") { questions[i].options[it] }
    }

    fun decode(s: String): Map<Int, List<String>> =
        if (s.isBlank()) emptyMap()
        else s.split("|").associate {
            val parts = it.split(":", limit = 2)
            parts[0].toInt() to parts.getOrElse(1) { "" }.split(";").filter { x -> x.isNotBlank() }
        }
}

object Video {
    private val rx = Regex("(?:youtu\\.be/|youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/))([A-Za-z0-9_-]{11})")
    fun id(url: String): String? = rx.find(url)?.groupValues?.get(1)
    fun thumbnail(url: String): String? = id(url)?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
}
