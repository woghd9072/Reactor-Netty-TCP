package com.jaehong

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ReactorNettyTcpApplication

fun main(args: Array<String>) {
	runApplication<ReactorNettyTcpApplication>(*args)
}
