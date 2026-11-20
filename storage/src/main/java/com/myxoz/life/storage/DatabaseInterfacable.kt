package com.myxoz.life.storage

import com.myxoz.life.storage.interfaces.DatabaseInterface

interface DatabaseInterfacable {
    val dbInterface: DatabaseInterface
    fun suggestResync()
    fun stopResync()
}