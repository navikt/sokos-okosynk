package no.nav.sokos.okosynk.exception

class OppgaveException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class PdlException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class SftpException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
