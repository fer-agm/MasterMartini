package cl.mastermartini.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: String,            // CLIENT | CHEF | MARKETING
    val phone: String,
    val rut: String = "",
    val businessType: String = "",
    val businessSize: String = "",
    val photoPath: String? = null,
    val quizAnswers: String = "" // ver QuizData.encode / decode
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val description: String,
    val ingredients: String,
    val steps: String,
    val videoUrl: String = "",   // solo el ENLACE; el video nunca se guarda en la app
    val authorId: Long,
    val authorName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val videoUrl: String = "",
    val authorName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites", primaryKeys = ["userId", "recipeId"])
data class FavoriteEntity(val userId: Long, val recipeId: Long)
