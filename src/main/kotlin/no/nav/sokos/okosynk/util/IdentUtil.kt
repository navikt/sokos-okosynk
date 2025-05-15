package no.nav.sokos.okosynk.util

object IdentUtil {
    fun String.isDnr(): Boolean = this.length == 11 && this[0] > '3'

    fun String.isBnr(): Boolean = this.length == 11 && this.substring(2, 4).toInt() in 21..32
}
