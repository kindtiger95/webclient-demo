package com.webclient.practice.webclientdemo.client

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
class ClientConfiguration {
    @Bean
    fun firstWebClient(): WebClient {
        return WebClient.builder().build()
    }

    @Bean
    fun secondWebClient(): WebClient {
    val httpClient = HttpClient.create(
        ConnectionProvider.builder("second-provider")
            .maxConnections(100)
            .maxIdleTime(Duration.ofMillis(5000))
            .maxLifeTime(Duration.ofMillis(5000))
            .build())
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100)
        .responseTimeout(Duration.ofMillis(500))
        .doOnConnected { conn ->
            conn.addHandlerLast(ReadTimeoutHandler(400, TimeUnit.MILLISECONDS))
                .addHandlerLast(WriteTimeoutHandler(400, TimeUnit.MILLISECONDS))
        }

        return WebClient.builder().clientConnector(ReactorClientHttpConnector(httpClient)).build()
    }

    @Bean
    fun thirdWebClient(): WebClient {
        return WebClient.builder().filter().build()
    }

    private fun customFilter() {

    }
}