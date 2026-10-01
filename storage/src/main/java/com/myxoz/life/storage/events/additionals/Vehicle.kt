package com.myxoz.life.storage.events.additionals

import android.util.Log

enum class Vehicle(override val id: Int): TagLike {
    Gehen(1), Fahrrad(2), Auto(3), Bus(4),
    SBahn(5), UBahn(6), Zug(7), RBahn(8);
    companion object {
        fun getById(id: Int): Vehicle? = Vehicle.entries.firstOrNull { it.id == id }.apply { if(this==null) Log.e("Vehicle", "Couldnt find vehicle with id $id in Vehicles.getById") }
    }
}
