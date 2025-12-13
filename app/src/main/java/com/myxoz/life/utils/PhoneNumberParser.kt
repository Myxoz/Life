package com.myxoz.life.utils

import com.myxoz.life.viewmodels.LargeDataCache

class PhoneNumberParser(val largeDataCache: LargeDataCache) {
    fun parse(number: String) = formatPhoneNumber(number, largeDataCache.vorwahlMap, largeDataCache.mobileCountryCodes, largeDataCache.carrierMap)
    fun getPhoneInfo(number: String): String? {
        val endNumber = number.replace("[^0-9]".toRegex(), "")
        return buildString {
            if(endNumber.startsWith("0") || endNumber.startsWith("49")) {
                append(largeDataCache.mobileCountryCodes["49"]+" · ")
                val trimN = if (endNumber.startsWith("0")) 1 else 2
                val withoutCountry = endNumber.substring(trimN)
                largeDataCache.carrierMap.entries.firstOrNull {
                    withoutCountry.startsWith(it.key)
                }?.let {
                    append(it.value)
                    return@buildString
                }
                largeDataCache.vorwahlMap.entries.firstOrNull {
                    withoutCountry.startsWith(it.key)
                }?.let {
                    append(it.value)
                    return@buildString
                }
                return null
            } else {
                largeDataCache.mobileCountryCodes.entries.firstOrNull {
                    endNumber.startsWith(it.key)
                }?.let {
                    append(it.value)
                    return@buildString
                }
                return null
            }
        }
    }
    companion object {
        fun formatPhoneNumber(number: String, vorwahlenMap: Map<String, String>, countryCodeMap: Map<String, String>, carierMap: Map<String, String>): String{
            val endNumber = number.replace("[^0-9]".toRegex(), "") + "               "
            if(endNumber.startsWith("0") || endNumber.startsWith("49")) {
                val trimN = if(endNumber.startsWith("0")) 1 else 2
                val withoutCountry = endNumber.substring(trimN)
                carierMap.keys.firstOrNull {
                    withoutCountry.startsWith(it)
                }?.let {
                    return "+49 $it ${withoutCountry.substring(it.length).trim()}"
                }
                vorwahlenMap.keys.firstOrNull {
                    withoutCountry.startsWith(it)
                }?.let {
                    return "+49 $it ${withoutCountry.substring(it.length).trim()}"
                }
                return "+49 $withoutCountry".trim() // Do not format if not mobile nor festnetz
            }
            countryCodeMap.keys.firstOrNull {
                endNumber.startsWith(it)
            }?.let {
                return "+$it ${endNumber.substring(it.length)}".trim()
            }
            return number // If everything fails, just return
        }
    }
}