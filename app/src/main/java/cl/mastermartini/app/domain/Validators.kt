package cl.mastermartini.app.domain

object Validators {
    private val emailRx = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val phoneRx = Regex("^(\\+?56)?9\\d{8}$")

    fun isValidEmail(s: String) = emailRx.matches(s.trim())

    fun isValidPhone(s: String) = s.filter { it != ' ' && it != '-' }.matches(phoneRx)

    fun isValidUrl(s: String) = Regex("^https?://[^\\s/$.?#][^\\s]*$").matches(s.trim())

    fun isValidRut(raw: String): Boolean {
        val c = raw.replace(".", "").replace("-", "").uppercase().trim()
        if (c.length !in 8..9) return false
        val body = c.dropLast(1)
        if (!body.all { it.isDigit() }) return false
        var sum = 0
        var f = 2
        for (d in body.reversed()) { sum += (d - '0') * f; f = if (f == 7) 2 else f + 1 }
        val r = 11 - sum % 11
        val expected = when (r) { 11 -> '0'; 10 -> 'K'; else -> '0' + r }
        return c.last() == expected
    }

    fun passwordError(p: String): String? = when {
        p.isEmpty() -> "La contraseña es obligatoria"
        p.length < 8 -> "Mínimo 8 caracteres"
        p.none { it.isUpperCase() } -> "Incluye al menos una mayúscula"
        p.none { it.isLowerCase() } -> "Incluye al menos una minúscula"
        p.none { it.isDigit() } -> "Incluye al menos un número"
        else -> null
    }
}
