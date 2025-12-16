package com.myxoz.life.events.additionals

import com.myxoz.life.R

enum class Vehicle(override val id: Int, override val drawable: Int): TagLike{
    Walk(1, R.drawable.walk),
    Bike(2, R.drawable.bike),
    Car(3, R.drawable.car),
    Bus(4, R.drawable.bus),
    SBahn(5, R.drawable.sbahn),
    UBahn(6, R.drawable.ubahn),
    Train(7, R.drawable.train),
    RBahn(8, R.drawable.rbahn),
    ;
    companion object {
        fun getById(id: Int): Vehicle? = Vehicle.entries.firstOrNull { it.id == id }.apply { if(this==null) println("Couldnt find vehicle with id $id in Vehicles.getById") }
    }
}
