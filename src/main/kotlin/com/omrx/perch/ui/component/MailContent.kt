package com.omrx.perch.ui.component

sealed class MailContent {
    // inlineImages: keyed by Content-ID (WITHOUT surrounding angle brackets, e.g.
    // "abc123@mail" not "<abc123@mail>") mapped to the attachment's raw bytes.
    // Populate this from whatever MIME part parsing extracts embedded images —
    // typically parts with a Content-ID header and Content-Disposition: inline.
    // CidSchemeHandlerFactory (in JcefBootstrap.kt) strips angle brackets on lookup,
    // so store keys either way as long as it's consistent with how you parse them.
    data class Html(val html: String, val inlineImages: Map<String, ByteArray> = emptyMap()) : MailContent()
    data class PlainText(val text: String) : MailContent()
    data class ImageOnly(val bytes: ByteArray, val mimeType: String) : MailContent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ImageOnly

            if (!bytes.contentEquals(other.bytes)) return false
            if (mimeType != other.mimeType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + mimeType.hashCode()
            return result
        }
    }

    object Empty : MailContent()
}