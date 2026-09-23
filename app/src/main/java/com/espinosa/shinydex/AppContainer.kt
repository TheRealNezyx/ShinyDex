package com.espinosa.shinydex

import android.content.Context
import com.espinosa.shinydex.data.local.FaceOffSession
import com.espinosa.shinydex.data.local.SettingsStore
import com.espinosa.shinydex.data.local.ShinyDexDatabase
import com.espinosa.shinydex.data.remote.ApiClient
import com.espinosa.shinydex.data.repo.FaceOffRepository
import com.espinosa.shinydex.data.repo.HuntRepository
import com.espinosa.shinydex.data.repo.PokedexRepository

/**
 * Hand-rolled dependency container.
 *
 * A dependency-injection framework would be overkill for four screens, and keeping the
 * graph explicit makes it obvious to a reviewer exactly what talks to the network.
 */
class AppContainer(context: Context) {

    private val database = ShinyDexDatabase.get(context)

    val settings = SettingsStore(context)

    val huntRepository = HuntRepository(database.huntDao())

    val pokedexRepository = PokedexRepository(database.pokedexDao(), ApiClient.service)

    val faceOffSession = FaceOffSession(context)

    val faceOffRepository = FaceOffRepository()
}
