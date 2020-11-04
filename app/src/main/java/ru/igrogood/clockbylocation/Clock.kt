package ru.igrogood.clockbylocation

data class Clock(var id:Int,
                 var name: String,
                 var descr: String,
                 var latitude: Double,
                 var longitude: Double,
                 var radius: Int,
                 var isActive: Boolean)