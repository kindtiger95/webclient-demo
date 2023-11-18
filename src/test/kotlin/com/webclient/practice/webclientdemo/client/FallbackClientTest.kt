package com.webclient.practice.webclientdemo.client

import com.webclient.practice.webclientdemo.exception.CustomException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit

class FallbackClientTest : DescribeSpec({
    lateinit var mockServer: MockWebServer

    describe("webclient를 사용해보자") {
        mockServer = MockWebServer()
        mockServer.enqueue(
            MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
        )
        mockServer.enqueue(
            MockResponse()
                .setBody("hello")
                .setBodyDelay(10, TimeUnit.SECONDS)
        )
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatus.BAD_REQUEST.value())
        )
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatus.BAD_REQUEST.value())
        )
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
        )
        mockServer.start()
        mockServer.url("/test")

        val secondWebClient = ClientConfiguration().secondWebClient()

        it("커넥션 타임아웃이 났을 때 failover") {
            val result = secondWebClient.get()
                .uri("http://localhost:${mockServer.port}/test")
                .retrieve()
                .bodyToMono(String::class.java)
                .onErrorResume(WebClientRequestException::class.java) { Mono.empty() }
                .block()
            result shouldBe null
        }

        it("response 타임아웃이 났을 때 failover") {
            val result = secondWebClient.get()
                .uri("http://localhost:${mockServer.port}/test")
                .retrieve()
                .bodyToMono(String::class.java)
                .onErrorResume(WebClientResponseException::class.java) { Mono.empty() }
                .block()
            result shouldBe null
        }

        it("4xx 에러를 받았을 때 failover를 하지 않고 다른 에러로 리턴") {
            shouldThrow<IllegalStateException> {
                secondWebClient.get()
                    .uri("http://localhost:${mockServer.port}/test")
                    .retrieve()
                    .onStatus({ it.is4xxClientError }, { Mono.error(CustomException.Response4xxError()) })
                    .bodyToMono(String::class.java)
                    .onErrorMap(CustomException.Response4xxError::class.java) { IllegalStateException("서버 에러") }
                    .block()
            }
        }

        it("5xx을 받았을 때는 failover하지 않고 다른 에러로 리턴하고 4xx을 받았을 때는 null로 리턴하자") {
            // 400 에러
            val result = secondWebClient.get()
                .uri("http://localhost:${mockServer.port}/test")
                .retrieve()
                .onStatus({ it.is4xxClientError }, { Mono.error(CustomException.Response4xxError()) })
                .onStatus({ it.is5xxServerError }, { Mono.error(CustomException.Response5xxError()) })
                .bodyToMono(String::class.java)
                .onErrorResume(CustomException.Response4xxError::class.java) { Mono.empty() }
                .onErrorMap(CustomException.Response5xxError::class.java) { IllegalStateException("서버 에러") }
                .block()
            result shouldBe null

            // 500 에러
            shouldThrow<IllegalStateException> {
                secondWebClient.get()
                    .uri("http://localhost:${mockServer.port}/test")
                    .retrieve()
                    .onStatus({ it.is4xxClientError }, { Mono.error(CustomException.Response4xxError()) })
                    .onStatus({ it.is5xxServerError }, { Mono.error(CustomException.Response5xxError()) })
                    .bodyToMono(String::class.java)
                    .onErrorResume(CustomException.Response4xxError::class.java) { Mono.empty() }
                    .onErrorMap(CustomException.Response5xxError::class.java) { IllegalStateException("서버 에러") }
                    .block()
            }
        }
    }
})