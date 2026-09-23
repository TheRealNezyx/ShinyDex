package com.espinosa.shinydex.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.espinosa.shinydex.ui.components.MilestoneDialog
import com.espinosa.shinydex.ui.components.PokeballLogo
import com.espinosa.shinydex.data.model.FaceOffRoom
import com.espinosa.shinydex.ui.screens.AddHuntDialog
import com.espinosa.shinydex.ui.screens.FaceOffHomeScreen
import com.espinosa.shinydex.ui.screens.FaceOffRoomScreen
import com.espinosa.shinydex.ui.screens.GenerationScreen
import com.espinosa.shinydex.ui.screens.HuntDetailScreen
import com.espinosa.shinydex.ui.screens.HuntsScreen
import com.espinosa.shinydex.ui.screens.PokedexScreen
import com.espinosa.shinydex.util.FaceOffCodes
import com.espinosa.shinydex.ui.theme.Gold
import com.espinosa.shinydex.ui.theme.Ink
import com.espinosa.shinydex.ui.theme.InkSoft
import com.espinosa.shinydex.ui.theme.Muted
import com.espinosa.shinydex.ui.viewmodel.FaceOffViewModel
import com.espinosa.shinydex.ui.viewmodel.GenerationViewModel
import com.espinosa.shinydex.ui.viewmodel.HuntsViewModel
import com.espinosa.shinydex.ui.viewmodel.PokedexViewModel
import com.espinosa.shinydex.ui.viewmodel.ViewModelFactories

private object Routes {
    const val GENERATION = "generation"
    const val HUNTS = "hunts"
    const val POKEDEX = "pokedex"
    const val FACEOFF = "faceoff"
    const val HUNT_DETAIL = "hunt/{huntId}"
    const val HUNT_ID = "huntId"

    fun huntDetail(id: Long): String = "hunt/$id"
}

/**
 * [inviteCode] is a room code from a `shinydex://join/CODE` link the app was opened with;
 * [onInviteConsumed] is called once it has been handed to the face-off screen.
 */
@Composable
fun ShinyDexRoot(inviteCode: String? = null, onInviteConsumed: () -> Unit = {}) {
    val generationViewModel: GenerationViewModel = viewModel(factory = ViewModelFactories.Factory)
    val faceOffViewModel: FaceOffViewModel = viewModel(factory = ViewModelFactories.Factory)
    val faceOffState by faceOffViewModel.state.collectAsStateWithLifecycle()
    val faceOffSpecies by faceOffViewModel.pickerSpecies.collectAsStateWithLifecycle()
    val faceOffMilestone by faceOffViewModel.milestone.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val huntsViewModel: HuntsViewModel = viewModel(factory = ViewModelFactories.Factory)
    val pokedexViewModel: PokedexViewModel = viewModel(factory = ViewModelFactories.Factory)

    val selectedGeneration by generationViewModel.selected.collectAsStateWithLifecycle()
    val hunts by huntsViewModel.allHunts.collectAsStateWithLifecycle()
    val species by huntsViewModel.species.collectAsStateWithLifecycle()
    val milestone by huntsViewModel.milestone.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    var showAddHunt by remember { mutableStateOf(false) }

    val startDestination =
        if (selectedGeneration == null) Routes.GENERATION else Routes.HUNTS

    // An invite link lands on the face-off tab with the code already filled in.
    LaunchedEffect(inviteCode) {
        val code = inviteCode ?: return@LaunchedEffect
        faceOffViewModel.prefillCode(code)
        navController.navigate(Routes.FACEOFF) { launchSingleTop = true }
        onInviteConsumed()
    }

    Scaffold(
        containerColor = Ink,
        topBar = {
            if (route != Routes.GENERATION && route != Routes.HUNT_DETAIL) {
                ShinyDexTopBar(
                    subtitle = selectedGeneration?.let { "Gen ${it.romanNumeral} · ${it.region}" }
                        ?: "",
                    onChangeGeneration = { navController.navigate(Routes.GENERATION) },
                )
            }
        },
        bottomBar = {
            if (route == Routes.HUNTS || route == Routes.POKEDEX || route == Routes.FACEOFF) {
                ShinyDexBottomBar(
                    currentRoute = route,
                    onNavigate = { destination ->
                        navController.navigate(destination) {
                            popUpTo(Routes.HUNTS) { inclusive = destination == Routes.HUNTS }
                            launchSingleTop = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.GENERATION) {
                GenerationScreen(
                    selected = selectedGeneration,
                    onSelect = { generation ->
                        generationViewModel.select(generation)
                        navController.navigate(Routes.HUNTS) {
                            popUpTo(Routes.GENERATION) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.HUNTS) {
                HuntsScreen(
                    hunts = hunts,
                    onOpenHunt = { id -> navController.navigate(Routes.huntDetail(id)) },
                    onAdjust = huntsViewModel::adjust,
                    onAddHunt = { showAddHunt = true },
                )
            }

            composable(Routes.FACEOFF) {
                val room = faceOffState.room
                if (room == null) {
                    FaceOffHomeScreen(
                        state = faceOffState,
                        generation = selectedGeneration,
                        pickerSpecies = faceOffSpecies,
                        onNameChange = faceOffViewModel::onNameChange,
                        onServerChange = faceOffViewModel::onServerChange,
                        onCodeChange = faceOffViewModel::onCodeChange,
                        onPreparePicker = faceOffViewModel::preparePicker,
                        onCreate = faceOffViewModel::create,
                        onJoin = faceOffViewModel::lookUpRoom,
                        onConfirmLoungeJoin = faceOffViewModel::confirmLoungeJoin,
                        onCancelLoungeJoin = faceOffViewModel::cancelLoungeJoin,
                        onDismissError = faceOffViewModel::dismissError,
                    )
                } else {
                    FaceOffRoomScreen(
                        room = room,
                        myPlayerId = faceOffState.myPlayerId,
                        connectionLost = faceOffState.connectionLost,
                        error = faceOffState.error,
                        onAdjust = faceOffViewModel::adjust,
                        onFound = faceOffViewModel::markFound,
                        onLeave = faceOffViewModel::leave,
                        onShare = { shareInvite(context, room) },
                        onDismissError = faceOffViewModel::dismissError,
                    )
                }
            }

            composable(Routes.POKEDEX) {
                val entries by pokedexViewModel.entries.collectAsStateWithLifecycle()
                val query by pokedexViewModel.query.collectAsStateWithLifecycle()
                val showShiny by pokedexViewModel.showShiny.collectAsStateWithLifecycle()
                val isRefreshing by pokedexViewModel.isRefreshing.collectAsStateWithLifecycle()
                val error by pokedexViewModel.error.collectAsStateWithLifecycle()
                val selected by pokedexViewModel.selected.collectAsStateWithLifecycle()
                val detail by pokedexViewModel.detail.collectAsStateWithLifecycle()

                PokedexScreen(
                    entries = entries,
                    query = query,
                    showShiny = showShiny,
                    isRefreshing = isRefreshing,
                    error = error,
                    selected = selected,
                    detail = detail,
                    onQueryChange = pokedexViewModel::onQueryChange,
                    onToggleShiny = pokedexViewModel::toggleShiny,
                    onRefresh = pokedexViewModel::refresh,
                    onSelect = pokedexViewModel::select,
                    onDismissDetail = pokedexViewModel::clearSelection,
                )
            }

            composable(
                route = Routes.HUNT_DETAIL,
                arguments = listOf(navArgument(Routes.HUNT_ID) { type = NavType.LongType }),
            ) { entry ->
                val huntId = entry.arguments?.getLong(Routes.HUNT_ID) ?: 0L
                val hunt = hunts.firstOrNull { it.id == huntId }
                if (hunt == null) {
                    // The hunt was deleted while its detail screen was open.
                    LaunchedEffect(huntId) { navController.popBackStack() }
                } else {
                    HuntDetailScreen(
                        hunt = hunt,
                        onBack = { navController.popBackStack() },
                        onAdjust = { delta -> huntsViewModel.adjust(hunt.id, delta) },
                        onToggleFound = { huntsViewModel.setFound(hunt.id, !hunt.isFound) },
                        onReset = { huntsViewModel.resetCounter(hunt.id) },
                        onDelete = {
                            huntsViewModel.delete(hunt.id)
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
    }

    val generationForNewHunt = selectedGeneration
    if (showAddHunt && generationForNewHunt != null) {
        AddHuntDialog(
            generation = generationForNewHunt,
            species = species,
            onDismiss = { showAddHunt = false },
            onConfirm = { pokemon, game, method, startingCount ->
                huntsViewModel.addHunt(pokemon, game, method, startingCount)
                showAddHunt = false
            },
        )
    }

    milestone?.let { event ->
        MilestoneDialog(
            count = event.count,
            message = event.message,
            onDismiss = huntsViewModel::dismissMilestone,
        )
    }

    faceOffMilestone?.let { event ->
        MilestoneDialog(
            count = event.count,
            message = event.message,
            onDismiss = faceOffViewModel::dismissMilestone,
        )
    }
}

private fun shareInvite(context: Context, room: FaceOffRoom) {
    val text = FaceOffCodes.inviteText(room.code, room.mode, room.target?.displayName)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Invite to room " + room.code))
}

@Composable
private fun ShinyDexTopBar(subtitle: String, onChangeGeneration: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PokeballLogo(modifier = Modifier.size(34.dp))
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text("SHINYDEX", style = MaterialTheme.typography.titleMedium, color = Gold)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Muted)
            }
        }
        TextButton(onClick = onChangeGeneration) {
            Text("CHANGE", style = MaterialTheme.typography.labelSmall, color = Muted)
        }
    }
}

@Composable
private fun ShinyDexBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = InkSoft) {
        NavigationBarItem(
            selected = currentRoute == Routes.HUNTS,
            onClick = { onNavigate(Routes.HUNTS) },
            icon = { Icon(Icons.Filled.CatchingPokemon, contentDescription = null) },
            label = { Text("Hunts") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Ink,
                selectedTextColor = Gold,
                indicatorColor = Gold,
                unselectedIconColor = Muted,
                unselectedTextColor = Muted,
            ),
        )
        NavigationBarItem(
            selected = currentRoute == Routes.FACEOFF,
            onClick = { onNavigate(Routes.FACEOFF) },
            icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null) },
            label = { Text("Face-off") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Ink,
                selectedTextColor = Gold,
                indicatorColor = Gold,
                unselectedIconColor = Muted,
                unselectedTextColor = Muted,
            ),
        )
        NavigationBarItem(
            selected = currentRoute == Routes.POKEDEX,
            onClick = { onNavigate(Routes.POKEDEX) },
            icon = { Icon(Icons.Filled.MenuBook, contentDescription = null) },
            label = { Text("Pokedex") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Ink,
                selectedTextColor = Gold,
                indicatorColor = Gold,
                unselectedIconColor = Muted,
                unselectedTextColor = Muted,
            ),
        )
    }
}
