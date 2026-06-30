package com.myxoz.life.android

import android.app.Application
import com.myxoz.life.repositories.AppRepos
import com.myxoz.life.storage.interfaces.DatabaseInterface

class MainApplication: Application() {
    val dbInterface by lazy { DatabaseInterface.by(applicationContext) }
    val appRepos by lazy { AppRepos(dbInterface) }
}