package com.omrx.perch.logging

import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Logger(private val name: String) {
    private val slf4jLogger = LoggerFactory.getLogger(name)
    private val dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    fun debug(message: String, throwable: Throwable? = null) {
        log("DEBUG", message, throwable)
        slf4jLogger.debug(message, throwable)
    }

    fun info(message: String, throwable: Throwable? = null) {
        log("INFO", message, throwable)
        slf4jLogger.info(message, throwable)
    }

    fun warn(message: String, throwable: Throwable? = null) {
        log("WARN", message, throwable)
        slf4jLogger.warn(message, throwable)
    }

    fun error(message: String, throwable: Throwable? = null) {
        log("ERROR", message, throwable)
        slf4jLogger.error(message, throwable)
    }

    private fun log(level: String, message: String, throwable: Throwable?) {
        val timestamp = LocalDateTime.now().format(dateFormat)
        val logMessage = "[$timestamp] [$level] [$name] $message"
        println(logMessage)
        throwable?.printStackTrace()
    }

    companion object {
        fun get(clazz: Class<*>): Logger = Logger(clazz.simpleName)
        fun get(name: String): Logger = Logger(name)
    }
}
