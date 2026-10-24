package cl.mastermartini.app

import android.app.Application
import android.content.Context
import cl.mastermartini.app.data.AppDatabase
import cl.mastermartini.app.data.Repository
import cl.mastermartini.app.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppContainer(ctx: Context) {
    val db = AppDatabase.build(ctx)
    val repo = Repository(db)
    val settings = SettingsStore(ctx)
}

class MartiniApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CoroutineScope(Dispatchers.IO).launch { container.repo.seedIfEmpty() }
    }
}

val Application.container: AppContainer get() = (this as MartiniApp).container
