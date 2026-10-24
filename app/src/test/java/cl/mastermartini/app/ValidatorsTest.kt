package cl.mastermartini.app

import cl.mastermartini.app.domain.QuizData
import cl.mastermartini.app.domain.Validators
import cl.mastermartini.app.domain.Video
import org.junit.Assert.*
import org.junit.Test

class ValidatorsTest {
    @Test fun rut() {
        assertTrue(Validators.isValidRut("12.345.678-5"))
        assertTrue(Validators.isValidRut("123456785"))
        assertFalse(Validators.isValidRut("12.345.678-9"))
        assertFalse(Validators.isValidRut("abc"))
    }
    @Test fun phoneAndEmail() {
        assertTrue(Validators.isValidPhone("+56 9 1234 5678"))
        assertFalse(Validators.isValidPhone("12345"))
        assertTrue(Validators.isValidEmail("ana@correo.cl"))
        assertFalse(Validators.isValidEmail("ana@correo"))
    }
    @Test fun password() {
        assertNotNull(Validators.passwordError("abc"))
        assertNull(Validators.passwordError("Cliente123"))
    }
    @Test fun youtubeIds() {
        assertEquals("aqz-KE-bpKQ", Video.id("https://www.youtube.com/watch?v=aqz-KE-bpKQ"))
        assertEquals("aqz-KE-bpKQ", Video.id("https://youtu.be/aqz-KE-bpKQ"))
        assertNull(Video.id("https://example.com/video.mp4"))
    }
    @Test fun quizRoundTrip() {
        val enc = QuizData.encode(mapOf(0 to setOf(1), 1 to setOf(0, 2), 2 to setOf(1), 3 to setOf(0), 4 to setOf(0)))
        val dec = QuizData.decode(enc)
        assertEquals(listOf("Chocolatería", "Heladería"), dec[1])
        assertEquals(listOf("Videos técnicos"), dec[0])
    }
}
