package com.alienkb.handler.core

/** Guesses which apps are banking / payment apps, so they're easy to protect. */
object Suggest {
    private val long = listOf(
        "bank", "wallet", "finance", "money", "payment", "paypal", "easypaisa", "jazzcash",
        "sadapay", "nayapay", "meezan", "alfalah", "askari", "konnect", "raast", "binance",
        "crypto", "invest", "trading", "broker", "upay", "pay",
    )
    private val short = setOf("hbl", "ubl", "mcb", "upi", "bop", "nbp", "jsbl", "scb", "pnb", "sbi")

    fun isFinance(pkg: String, label: String): Boolean {
        val text = "$pkg $label".lowercase()
        if (long.any { text.contains(it) }) return true
        val tokens = text.split(Regex("[^a-z0-9]+")).toSet()
        return tokens.any { it in short }
    }
}
