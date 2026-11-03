package com.clairjour.app.shield

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/** What Android reports about the shield accessibility service. */
enum class ShieldServiceState {
    /** Switched on in the accessibility settings and bound by Android. */
    RUNNING,

    /** Switched on in the settings but not bound: the system (often MIUI/HyperOS) killed it. */
    STOPPED_BY_SYSTEM,

    /** Switched off in the accessibility settings. */
    OFF
}

object ShieldServiceStatus {

    private const val ACCESSIBILITY_DETAILS_ACTION = "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"
    private val xiaomiManufacturers = setOf("xiaomi", "redmi", "poco")

    fun state(context: Context): ShieldServiceState {
        val expected = component(context)
        val running = context.getSystemService(AccessibilityManager::class.java)
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .orEmpty()
            .any { ComponentName(it.resolveInfo.serviceInfo.packageName, it.resolveInfo.serviceInfo.name) == expected }
        return when {
            running -> ShieldServiceState.RUNNING
            isSwitchedOnInSettings(context) -> ShieldServiceState.STOPPED_BY_SYSTEM
            else -> ShieldServiceState.OFF
        }
    }

    private fun isSwitchedOnInSettings(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any { ComponentName.unflattenFromString(it) == component(context) }
    }

    /** Opens the shield's own accessibility page when Android allows it, else the general list. */
    fun openAccessibilitySettings(context: Context) {
        val details = Intent(ACCESSIBILITY_DETAILS_ACTION)
            .putExtra(Intent.EXTRA_COMPONENT_NAME, component(context).flattenToString())
        val opened = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && start(context, details)
        if (!opened) start(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private val deviceBrand: String
        get() = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()

    /** MIUI/HyperOS disable accessibility services of apps that are not allowed to autostart. */
    val isXiaomi: Boolean
        get() = xiaomiManufacturers.any { it in deviceBrand }

    /**
     * Known "autostart" screens of brands that stop background services. Without this permission
     * they do not restart an accessibility service whose process was killed.
     */
    private val autostartScreens: List<Pair<Set<String>, ComponentName>> = listOf(
        xiaomiManufacturers to ComponentName(
            "com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"
        ),
        setOf("oppo", "realme", "oneplus") to ComponentName(
            "com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"
        ),
        setOf("oppo", "realme", "oneplus") to ComponentName(
            "com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"
        ),
        setOf("vivo", "iqoo") to ComponentName(
            "com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
        ),
        setOf("huawei", "honor") to ComponentName(
            "com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
        )
    )

    private val deviceAutostartScreens: List<ComponentName>
        get() = autostartScreens.filter { (brands, _) -> brands.any { it in deviceBrand } }.map { it.second }

    /** True on brands that have a dedicated autostart screen. */
    val hasAutostartSettings: Boolean
        get() = deviceAutostartScreens.isNotEmpty()

    fun openAutostartSettings(context: Context) {
        if (deviceAutostartScreens.any { start(context, Intent().setComponent(it)) }) return
        val miuiPermissionEditor = Intent("miui.intent.action.APP_PERM_EDITOR")
            .putExtra("extra_pkgname", context.packageName)
        if (!(isXiaomi && start(context, miuiPermissionEditor))) openAppDetails(context)
    }

    /** Xiaomi "Battery saver: no restrictions" screen, otherwise the app page (Battery > Unrestricted). */
    fun openBatterySettings(context: Context) {
        val miuiBattery = Intent().setComponent(
            ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
        )
            .putExtra("package_name", context.packageName)
            .putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager).toString())
        if (isXiaomi && start(context, miuiBattery)) return
        // Samsung and others: the app page holds the "Battery > Unrestricted" choice.
        openAppDetails(context)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

    private fun openAppDetails(context: Context) {
        start(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        )
    }

    private fun component(context: Context) = ComponentName(context, InstagramShieldService::class.java)

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (error: ActivityNotFoundException) {
        false
    } catch (error: SecurityException) {
        // MIUI security center activities can be non-exported on some versions.
        false
    }
}
