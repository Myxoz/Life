package com.myxoz.life.presentation

import com.myxoz.life.storage.api.helper.MixedBankingEntity
import kotlin.math.abs

object BankingFormating {
    fun Int.formatCents(euroSign: Boolean=true, showSign: Boolean = true) =
        (if(!showSign) "" else if(this<0) "-" else "+") + // Sign
        "${abs(this)/100},"+ // Before comma
        (abs(this)%100).toString().padStart(2, '0') + // decimals
        if(euroSign) "€" else "" // Euro Sign

    fun MixedBankingEntity.getIcon() = when{
        digital && cashless -> R.drawable.bank_transfer
        digital && !cashless -> R.drawable.pay_with_card
        !digital && cashless -> R.drawable.wireless_pay
        else -> R.drawable.cash
    }}