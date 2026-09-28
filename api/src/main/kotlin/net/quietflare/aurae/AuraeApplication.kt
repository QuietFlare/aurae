package net.quietflare.aurae

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AuraeApplication

fun main(args: Array<String>) {
    runApplication<AuraeApplication>(*args)
}
