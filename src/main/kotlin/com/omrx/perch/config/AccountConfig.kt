package com.omrx.perch.config

enum class AccountType {
    GMAIL,
    GENERIC_IMAP
}

data class AccountConfig(
    val id: String,
    val displayName: String,
    val email: String,
    val type: AccountType,
    val imapHost: String,
    val imapPort: Int = 993
) {
    init {
        require(imapHost.isNotBlank()) { "IMAP host cannot be blank" }
        require(imapPort in 1..65535) { "IMAP port must be between 1 and 65535" }
        require(email.isNotBlank()) { "Email cannot be blank" }
    }
}
