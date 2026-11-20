package com.myxoz.life.lifecore.settings

enum class Service(
    val enable: Permission.SettingsChanger.()->Unit,
    val disable: Permission.SettingsChanger.()->Unit
) {
    StepCounting({
        serviceController.enable(context, StepCounting)
    }, {
        serviceController.disable(context, StepCounting)
    }),
}