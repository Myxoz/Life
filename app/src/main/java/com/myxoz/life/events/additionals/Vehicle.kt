package com.myxoz.life.events.additionals

import android.util.Log
import com.myxoz.life.R

enum class Vehicle(override val id: Int, override val displayName: String, override val drawable: Int): TagLike{
    Gehen(1, "Gehen", R.drawable.walk),
    Fahrrad(2, "Fahrrad", R.drawable.bike),
    Auto(3, "Auto", R.drawable.car),
    Bus(4, "Bus", R.drawable.bus),
    SBahn(5, "S-Bahn", R.drawable.sbahn),
    UBahn(6, "U-Bahn", R.drawable.ubahn),
    Zug(7, "Zug", R.drawable.train),
    RBahn(8, "Regionalbahn", R.drawable.rbahn),
    ;
    companion object {
        fun getById(id: Int): Vehicle? = Vehicle.entries.firstOrNull { it.id == id }.apply { if(this==null) Log.e("Vehicle", "Couldnt find vehicle with id $id in Vehicles.getById") }
    }
}
