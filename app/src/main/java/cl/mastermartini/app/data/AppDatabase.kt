package cl.mastermartini.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert suspend fun insert(u: UserEntity): Long
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1") suspend fun byEmail(email: String): UserEntity?
    @Query("SELECT * FROM users WHERE id = :id") fun observe(id: Long): Flow<UserEntity?>
    @Query("SELECT * FROM users WHERE role = 'CLIENT' ORDER BY name") fun observeClients(): Flow<List<UserEntity>>
    @Query("UPDATE users SET quizAnswers = :a WHERE id = :id") suspend fun saveQuiz(id: Long, a: String)
    @Query("UPDATE users SET photoPath = :p WHERE id = :id") suspend fun savePhoto(id: Long, p: String?)
    @Query("SELECT COUNT(*) FROM users") suspend fun count(): Int
}

@Dao
interface RecipeDao {
    @Insert suspend fun insert(r: RecipeEntity): Long
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC") fun observeAll(): Flow<List<RecipeEntity>>
    @Query("SELECT * FROM recipes WHERE id = :id") fun observe(id: Long): Flow<RecipeEntity?>
    @Query("DELETE FROM recipes WHERE id = :id") suspend fun delete(id: Long)
    @Update suspend fun update(r: RecipeEntity)
}

@Dao
interface PostDao {
    @Insert suspend fun insert(p: PostEntity): Long
    @Query("SELECT * FROM posts ORDER BY createdAt DESC") fun observeAll(): Flow<List<PostEntity>>
    @Query("DELETE FROM posts WHERE id = :id") suspend fun delete(id: Long)
    @Update suspend fun update(p: PostEntity)
}

@Dao
interface FavoriteDao {
    @Query("SELECT recipeId FROM favorites WHERE userId = :u") fun observe(u: Long): Flow<List<Long>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(f: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE userId = :u AND recipeId = :r") suspend fun delete(u: Long, r: Long)
    @Query("DELETE FROM favorites WHERE recipeId = :r") suspend fun deleteForRecipe(r: Long)
}

@Database(
    entities = [UserEntity::class, RecipeEntity::class, PostEntity::class, FavoriteEntity::class],
    version = 1, exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun users(): UserDao
    abstract fun recipes(): RecipeDao
    abstract fun posts(): PostDao
    abstract fun favorites(): FavoriteDao

    companion object {
        fun build(ctx: Context): AppDatabase =
            Room.databaseBuilder(ctx, AppDatabase::class.java, "martini.db").build()
    }
}
