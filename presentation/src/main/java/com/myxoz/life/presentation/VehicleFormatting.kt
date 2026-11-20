package com.myxoz.life.presentation

import androidx.annotation.DrawableRes
import com.myxoz.life.storage.events.additionals.Vehicle

enum class VehicleFormatting(
    val vehicle: Vehicle,
    override val displayName: String,
    @DrawableRes override val drawable: Int
): DisplayFormattable {
    Walk(Vehicle.Walk, "Gehen", R.drawable.walk),
    Bike(Vehicle.Bike, "Fahrrad", R.drawable.bike),
    Car(Vehicle.Car,"Auto", R.drawable.car),
    Bus(Vehicle.Bus, "Bus", R.drawable.bus),
    SBahn(Vehicle.SBahn, "S-Bahn", R.drawable.sbahn),
    UBahn(Vehicle.UBahn, "U-Bahn", R.drawable.ubahn),
    Train(Vehicle.Train, "Zug", R.drawable.train),
    RBahn(Vehicle.RBahn, "Regionalbahn", R.drawable.rbahn);
    companion object {
        fun Vehicle.formattable(): VehicleFormatting =
            when(this) {
                Vehicle.Walk -> Walk
                Vehicle.Bike -> Bike
                Vehicle.Car -> Car
                Vehicle.Bus -> Bus
                Vehicle.SBahn -> SBahn
                Vehicle.UBahn -> UBahn
                Vehicle.Train -> Train
                Vehicle.RBahn -> RBahn
            }
    }
}