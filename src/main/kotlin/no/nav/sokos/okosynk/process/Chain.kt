package no.nav.sokos.okosynk.process

interface Chain<I, O> {
    fun process(data: I): O
}
