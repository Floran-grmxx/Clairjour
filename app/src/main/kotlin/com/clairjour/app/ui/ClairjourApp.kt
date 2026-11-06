package com.clairjour.app.ui

import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.clairjour.app.R
import com.clairjour.app.data.AppContainer
import com.clairjour.app.shield.ShieldKeepAlive
import com.clairjour.app.shield.ShieldServiceState
import com.clairjour.app.shield.ShieldServiceStatus
import com.clairjour.app.ui.navigation.ClairjourNavHost
import com.clairjour.app.ui.navigation.Destinations

private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomTabs = listOf(
    BottomTab(Destinations.HOME, R.string.nav_home, Icons.Outlined.Home),
    BottomTab(Destinations.JOURNAL, R.string.nav_journal, Icons.Outlined.Book),
    BottomTab(Destinations.STATS, R.string.nav_stats, Icons.Outlined.BarChart),
    BottomTab(Destinations.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings)
)

@Composable
fun ClairjourApp(container: AppContainer) {
    val navController = rememberNavController()
    val onboardingDone by container.settingsRepository.onboardingDoneFlow
        .collectAsState(initial = null)

    // Badge on the Settings tab when the shield is wanted but the phone stopped it.
    val context = LocalContext.current
    val shieldSettings by container.settingsRepository.shieldSettingsFlow.collectAsState(initial = null)
    var shieldServiceState by remember { mutableStateOf(ShieldServiceState.OFF) }
    LifecycleResumeEffect(Unit) {
        shieldServiceState = ShieldServiceStatus.state(context)
        val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
        val listener = AccessibilityManager.AccessibilityStateChangeListener {
            shieldServiceState = ShieldServiceStatus.state(context)
        }
        accessibilityManager?.addAccessibilityStateChangeListener(listener)
        onPauseOrDispose { accessibilityManager?.removeAccessibilityStateChangeListener(listener) }
    }
    val shieldNeedsAttention = ShieldKeepAlive.needsAttention(
        serviceState = shieldServiceState,
        shieldWanted = shieldSettings?.enabled == true
    )

    val startDestination = when (onboardingDone) {
        null -> null
        false -> Destinations.ONBOARDING
        true -> Destinations.HOME
    }

    if (startDestination == null) return

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val showBottomBar = currentRoute in bottomTabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = currentBackStack?.destination?.hierarchy
                            ?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (tab.route == Destinations.SETTINGS && shieldNeedsAttention) {
                                    val badgeDescription = stringResource(R.string.cd_shield_stopped_badge)
                                    BadgedBox(
                                        badge = {
                                            Badge(Modifier.semantics { contentDescription = badgeDescription })
                                        }
                                    ) { Icon(tab.icon, contentDescription = null) }
                                } else {
                                    Icon(tab.icon, contentDescription = null)
                                }
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        ClairjourNavHost(
            navController = navController,
            container = container,
            startDestination = startDestination,
            contentPadding = padding
        )
    }
}
