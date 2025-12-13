package com.myxoz.life.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LargeDataCache: ViewModel() {
    lateinit var bankMap: Map<String, Bank>
        private set
    lateinit var vorwahlMap: Map<String, String>
        private set
    lateinit var carrierMap: Map<String, String>
        private set
    lateinit var mobileCountryCodes: Map<String, String>
        private set
    fun loadBankMap(context: Context) {
        val newData = mutableMapOf<String, Bank>()
        // From https://www.bundesbank.de/de/aufgaben/unbarer-zahlungsverkehr/serviceangebot/bankleitzahlen/download-bankleitzahlen-602592
        forEachFileByLine(context, "blz.csv") {
            val split = it.split(";")
            newData[split[0].trim('"')] = Bank(
                split[2].trim('"'),
                split[3].trim('"'),
                split[4].trim('"'),
                split[7].trim('"')
            )
        }
        bankMap = newData
    }
    fun getIbanInformation(cutIban: String): String?{
        return bankMap.entries.firstOrNull {
            cutIban.startsWith(it.key)
        }?.let{
            "${it.value.name} · ${it.value.ort}, ${it.value.plz} · ${it.value.bic}"
        }
    }
    fun loadVorwahlen(context: Context) {
        val newData = mutableMapOf<String, String>()
        // From https://www.bundesnetzagentur.de/DE/Fachthemen/Telekommunikation/Nummerierung/ONRufnr/Einteilung_ONB/start.html?r=1
        forEachFileByLine(context, "vorwahlen.csv") {
            val split = it.split(";")
            newData[split[0]] = split[1]
        }
        vorwahlMap = newData
    }
    fun loadCarriers(context: Context) {
        val newData = mutableMapOf<String, String>()
        // From https://www.bundesnetzagentur.de/DE/Fachthemen/Telekommunikation/Nummerierung/MobileDienste/zugeteilteRNB/start.html
        forEachFileByLine(context, "cariermap.csv") {
            newData[it.substringBefore(";")] = it.substringAfter(";")
        }
        carrierMap = newData
    }
    fun loadMobileCountryCodes(context: Context) {
        val newData = mutableMapOf<String, String>()
        // From https://www.itu.int/dms_pub/itu-t/opb/sp/T-SP-E.164B-2012-PDF-E.pdf
        forEachFileByLine(context, "country_codes.csv") {
            newData[it.substringAfterLast(";")] = it.substringBeforeLast(";")
        }
        mobileCountryCodes = newData
    }
    private fun forEachFileByLine(context: Context, filename: String, method: (String)->Unit){
        val file = context.assets.open(filename)
        file.bufferedReader().useLines { j ->
            j.forEach {
                method(it)
            }
        }
    }
    suspend fun preloadAll(context: Context) = withContext(Dispatchers.IO) {
        loadBankMap(context)
        loadVorwahlen(context)
        loadMobileCountryCodes(context)
        loadCarriers(context)
    }
    data class Bank(val name: String, val plz: String, val ort: String, val bic: String){
        fun format() = "$name · $ort, $plz · $bic"
    }
}