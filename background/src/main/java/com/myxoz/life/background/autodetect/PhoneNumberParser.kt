package com.myxoz.life.background.autodetect

object PhoneNumberParser {
    val numberRegex = Regex("^[+0][\\d|\\s]+$")
    fun normalize(number: String) = "+"+(if(number.startsWith("0")) "49${number.removePrefix("0")}" else number).replace("\\D".toRegex(), "")
    fun areEqual(number1: String, number2: String) = normalize(number1) == normalize(number2)
}