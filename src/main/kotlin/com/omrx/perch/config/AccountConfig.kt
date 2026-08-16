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
)