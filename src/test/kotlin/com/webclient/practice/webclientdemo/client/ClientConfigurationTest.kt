package com.webclient.practice.webclientdemo.client

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.concurrent.TimeUnit

class ClientConfigurationTest : DescribeSpec({
    lateinit var mockServer: MockWebServer

    describe("첫 번째") {
        beforeTest {
            mockServer = MockWebServer()
            mockServer.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """
                    {"test": "1"}
                    """.trimIndent()
                ).setHeader("Content-Type", "application/json")
            )
            mockServer.start()
            mockServer.url("/test")
        }

        afterTest {
            mockServer.shutdown()
        }

        it("기본 WebClient 테스트") {
            val firstWebClient = ClientConfiguration().firstWebClient()
            val result = firstWebClient.get().uri("http://localhost:${mockServer.port}/test").retrieve().bodyToMono(String::class.java).block()
            result shouldBe "{\"test\": \"1\"}"
        }
    }

    describe("두 번째") {
        mockServer = MockWebServer()
        // 커넥션이 안될 때
        mockServer.enqueue(
            MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
        )
        // 커넥션은 되었는데 응답이 느릴 때
        mockServer.enqueue(
            MockResponse()
                .setBody("hello")
                .setBodyDelay(1, TimeUnit.SECONDS)
        )
        mockServer.start()
        mockServer.url("/test")

        val secondWebClient = ClientConfiguration().secondWebClient()

        it("커넥션 타임아웃 테스트") {
            shouldThrow<WebClientRequestException> {
                secondWebClient.get().uri("http://localhost:${mockServer.port}/test").retrieve().bodyToMono(String::class.java).block()
            }
        }

        it("응답 타임아웃 테스트") {
            shouldThrow<WebClientResponseException> {
                secondWebClient.get().uri("http://localhost:${mockServer.port}/test").retrieve().bodyToMono(String::class.java).block()
            }
        }

        mockServer.shutdown()
    }
})
