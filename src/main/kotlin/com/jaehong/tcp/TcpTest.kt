package com.jaehong.tcp

import io.netty.channel.ChannelHandlerAdapter
import io.netty.channel.ChannelHandlerContext
import mu.KotlinLogging
import reactor.core.publisher.Mono
import reactor.netty.Connection
import reactor.netty.tcp.TcpClient
import reactor.netty.tcp.TcpServer
import java.util.concurrent.CountDownLatch

fun main() {
    val log = KotlinLogging.logger {  }
    val server: EchoServer

    object : Thread() {
        override fun run() {
            EchoServer()
        }
    }.start()

    Thread.sleep(1000)

    val strings = mutableListOf<String>()
    val clientConnection: Connection = TcpClient.create()
            .host("localhost") // default localhost
            .port(12012) // default 12012
            .handle { inbound, outbound ->
                outbound.sendString(Mono.just("hello server!!").log())
                        .then(
                                inbound.receive()
                                        .asString()
                                        .take(100)
                                        .flatMapIterable { s -> s.split("\\n") }
                                        .doOnNext { s ->
                                            log.info { "@@@@@@@@@@@@@@ s = $s" }
                                            strings.add(s)
                                        }.then()
                        )
            }
            .connectNow()
}
class EchoServer {
    val latch = CountDownLatch(1)
    val log = KotlinLogging.logger {}
    private val server = TcpServer.create()
            .port(12012)
            .doOnConnection { conn: Connection ->  // 클라이언트 연결시 호출
                conn.addHandler(object : ChannelHandlerAdapter() {
                    override fun handlerAdded(ctx: ChannelHandlerContext) {
                        log.info("client added")
                    }
                    override fun handlerRemoved(ctx: ChannelHandlerContext) {
                        log.info("client removed")
                    }
                })
            }
            .handle { inbound, outbound ->  // 연결된 커넥션에 대한 IN/OUT 처리
                inbound.receive() // 데이터 읽기 선언, ByteBufFlux 리턴
                        .asString() // 문자열로 변환 선언, Flux<String> 리턴
                        .flatMap { msg: String ->
                            log.debug("doOnNext: [{}]", msg)
                            outbound.sendString(Mono.just("echo: $msg\r\n"))
                        }
            }
            .bind()
            .block()
    init {
        latch()
    }
    fun latch() {
        try {
            latch.await()
        } catch (e: InterruptedException) {
        }
        log.info { "dispose server" }
        server!!.dispose()
    }
}