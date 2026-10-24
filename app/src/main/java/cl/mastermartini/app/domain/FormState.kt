package cl.mastermartini.app.domain

import androidx.compose.runtime.mutableStateMapOf

typealias Rule = (Map<String, String>) -> String?

class FormState(
    private val rules: Map<String, Rule>,
    private val initial: Map<String, String> = emptyMap()
) {
    val values = mutableStateMapOf<String, String>().apply { putAll(initial) }
    private val touched = mutableStateMapOf<String, Boolean>()

    fun value(k: String): String = values[k] ?: ""
    fun set(k: String, v: String) { values[k] = v; touched[k] = true }
    fun error(k: String): String? = if (touched[k] == true) rules[k]?.invoke(values) else null
    fun isValid(k: String): Boolean = touched[k] == true && value(k).isNotBlank() && rules[k]?.invoke(values) == null

    fun validateAll(): Boolean {
        rules.keys.forEach { touched[it] = true }
        return rules.values.all { it(values) == null }
    }

    fun reset() { values.clear(); values.putAll(initial); touched.clear() }
}

object Forms {
    private fun required(key: String, label: String, min: Int = 1): Rule = { m ->
        val v = m[key].orEmpty().trim()
        when {
            v.isEmpty() -> "$label es obligatorio"
            v.length < min -> "$label debe tener al menos $min caracteres"
            else -> null
        }
    }

    private val name: Rule = { m ->
        val v = m["name"].orEmpty().trim()
        when {
            v.isEmpty() -> "El nombre es obligatorio"
            v.length < 3 -> "Mínimo 3 caracteres"
            !v.all { it.isLetter() || it == ' ' } -> "Solo letras y espacios"
            else -> null
        }
    }
    private val email: Rule = { m ->
        val v = m["email"].orEmpty()
        when {
            v.isBlank() -> "El correo es obligatorio"
            !Validators.isValidEmail(v) -> "Formato inválido (ej: nombre@correo.cl)"
            else -> null
        }
    }
    private val phone: Rule = { m ->
        val v = m["phone"].orEmpty()
        when {
            v.isBlank() -> "El teléfono es obligatorio"
            !Validators.isValidPhone(v) -> "Formato: +56 9 1234 5678"
            else -> null
        }
    }
    private val optionalUrl: Rule = { m ->
        val v = m["videoUrl"].orEmpty()
        if (v.isBlank() || Validators.isValidUrl(v)) null else "Enlace inválido (debe comenzar con https://)"
    }

    fun login() = FormState(mapOf(
        "email" to email,
        "password" to { m -> if (m["password"].orEmpty().isEmpty()) "Ingresa tu contraseña" else null }
    ))

    fun register() = FormState(
        rules = mapOf(
            "name" to name, "email" to email, "phone" to phone,
            "rut" to { m ->
                if (m["role"] != "CLIENT") null else {
                    val v = m["rut"].orEmpty()
                    when {
                        v.isBlank() -> "El RUT de empresa es obligatorio"
                        !Validators.isValidRut(v) -> "RUT inválido (ej: 12.345.678-5)"
                        else -> null
                    }
                }
            },
            "code" to { m ->
                val expected = when (m["role"]) { "CHEF" -> AccessCodes.CHEF; "MARKETING" -> AccessCodes.MARKETING; else -> null }
                if (expected == null || m["code"] == expected) null else "Código de acceso incorrecto"
            },
            "password" to { m -> Validators.passwordError(m["password"].orEmpty()) },
            "confirm" to { m -> if (m["confirm"] != m["password"]) "Las contraseñas no coinciden" else null }
        ),
        initial = mapOf("role" to "CLIENT", "businessType" to Categories.businessTypes[0], "businessSize" to Categories.businessSizes[0])
    )

    fun recipe() = FormState(
        rules = mapOf(
            "title" to required("title", "El título", 3),
            "description" to required("description", "La descripción", 10),
            "ingredients" to required("ingredients", "Los ingredientes", 5),
            "steps" to required("steps", "Los pasos", 10),
            "videoUrl" to optionalUrl
        ),
        initial = mapOf("category" to Categories.recipe[0])
    )

    fun post() = FormState(mapOf(
        "title" to required("title", "El título", 3),
        "body" to required("body", "El mensaje", 10),
        "videoUrl" to optionalUrl
    ))

    fun contact() = FormState(mapOf(
        "name" to name, "email" to email,
        "message" to required("message", "El mensaje", 10)
    ))
}
