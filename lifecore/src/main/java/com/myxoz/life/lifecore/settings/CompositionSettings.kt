package com.myxoz.life.lifecore.settings

import android.app.Activity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds

class CompositionSettings(
    val settingsChecker: Permission.SettingsChecker,
    activity: Activity,
    serviceController: ServiceController
) {
    val localSettings = settingsChecker.store
    private val features = mutableMapOf<Feature, MutableStateFlow<Boolean>>()
    private val changeContext = Permission.SettingsChanger(
        settingsChecker.store,
        settingsChecker.context,
        settingsChecker.specificPermissionChecker,
        activity,
        serviceController
    )
    private val permissions = mutableMapOf<Permission, MutableStateFlow<Boolean>>()
    private val dependencies = mutableMapOf<Permission, MutableStateFlow<Set<Feature>>>()
    fun has(feature: Feature): MutableStateFlow<Boolean> {
        val cachedFeature = features[feature]
        if(cachedFeature != null) return cachedFeature
        val stored = settingsChecker.store.get(feature.storedSetting.key, false)
        updateDependency(feature, stored)
        return features.getOrPut(feature) { MutableStateFlow(stored) }
    }

    fun has(permission: Permission): MutableStateFlow<Boolean> {
        val cachedPermission = permissions[permission]
        if(cachedPermission != null) return cachedPermission
        return permissions.getOrPut(permission) {
            MutableStateFlow(permission.check(settingsChecker))
        }
    }

    fun hasAssured(feature: Feature) =
        has(feature).value && feature.enabledAssured(settingsChecker)

    fun hasAssured(permission: Permission) = permission.check(settingsChecker)

    fun set(feature: Feature, newVal: Boolean) {
        has(feature).update { newVal }
        updateDependency(feature, newVal)
        settingsChecker.store.put(feature.storedSetting.key, newVal)
        if(newVal) feature.onEnable?.invoke(changeContext) else feature.onDisable?.invoke(changeContext)
    }

    suspend fun set(permission: Permission, newVal: Boolean) {
        if(newVal) {
            permission.onEnable(changeContext)
        } else {
            permission.onDisable(changeContext)
            Feature.entries.forEach {
                if(permission in it.reliesOn) set(it, false)
            }
        }
        awaitForValue(permission, newVal){
            has(permission).value = newVal
            permission.onEnabled?.invoke(changeContext)
        }
    }

    private fun getDependencies(permission: Permission) =
        dependencies.getOrPut(permission) { MutableStateFlow(setOf()) }

    private fun updateDependency(feature: Feature, active: Boolean) {
        feature.reliesOn.forEach {
            if(active) getDependencies(it).value += feature else getDependencies(it).value -= feature
        }
    }

    /* Cache this value */
    fun isUseless(permission: Permission) = getDependencies(permission).map { it.isEmpty() }
    private suspend fun awaitForValue(permission: Permission, expect: Boolean, onSuccess: ()-> Unit){
        while (true) {
            if (permission.check(settingsChecker) == expect){
                onSuccess()
                break
            }
            delay(200.milliseconds)
        }
    }
}
