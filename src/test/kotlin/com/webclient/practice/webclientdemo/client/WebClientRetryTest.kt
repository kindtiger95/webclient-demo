package com.webclient.practice.webclientdemo.client

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

class WebClientRetryTest : DescribeSpec({
    lateinit var mockServer: MockWebServer

    describe("Retry 테스트 첫 번째") {
        mockServer = MockWebServer()
        mockServer.enqueue(
            MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
        )
        mockServer.enqueue(
            MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
        )
        mockServer.enqueue(
            MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
        )
        mockServer.enqueue(
            MockResponse()
                .setBody("hello")
                .setResponseCode(HttpStatus.OK.value())
        )
        mockServer.start()
        mockServer.url("/test")

        val secondWebClient = ClientConfiguration().secondWebClient()
        it("지연없이 retry를 최대 3번 시도한다.") {
            val result = secondWebClient.get().uri("http://localhost:${mockServer.port}/test").retrieve().bodyToMono(String::class.java)
                .retryWhen(Retry.max(3))
                .block()
            result shouldBe "hello"
        }
    }

    describe("Retry 테스트 두 번째") {
        mockServer = MockWebServer()
        mockServer.enqueue(
            MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
        )
        mockServer.enqueue(
            MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
        )
        mockServer.enqueue(
            MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
        )
        mockServer.enqueue(
            MockResponse()
                .setBody("hello")
                .setResponseCode(HttpStatus.OK.value())
        )
        mockServer.start()
        mockServer.url("/test")

        val secondWebClient = ClientConfiguration().secondWebClient()
        it("1초간 지연하여 최대 3번 재시도한다.") {
            val result = secondWebClient.get().uri("http://localhost:${mockServer.port}/test").retrieve().bodyToMono(String::class.java)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1)))
                .block()
            result shouldBe "hello"
        }
    }

    describe("Retry 테스트 세 번째") {
        mockServer = MockWebServer()
        mockServer.enqueue(
            MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
        )
        mockServer.enqueue(
            MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
        )
        mockServer.enqueue(
            MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
        )
        mockServer.enqueue(
            MockResponse()
                .setBody("hello")
                .setResponseCode(HttpStatus.OK.value())
        )
        mockServer.start()
        mockServer.url("/test")

        val secondWebClient = ClientConfiguration().secondWebClient()
        it("backoff 전략을 이용하여 테스트한다.") {
            val result = secondWebClient.get().uri("http://localhost:${mockServer.port}/test").retrieve().bodyToMono(String::class.java)
                // 여기 지연 시간을 재시도 할 때 마다 제곱하여 지연시킨다. ex) 첫 번째 -> 2^1, 두 번째 -> 2^2 ...
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .block()
            result shouldBe "hello"
        }
    }

    describe("Retry 테스트 네 번째") {
        mockServer = MockWebServer()
        mockServer.enqueue(
            MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
        )
        mockServer.enqueue(
            MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
        )
        mockServer.enqueue(
            MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
        )
        mockServer.enqueue(
            MockResponse()
                .setBody("hello")
                .setResponseCode(HttpStatus.OK.value())
        )
        mockServer.start()
        mockServer.url("/test")

        val secondWebClient = ClientConfiguration().secondWebClient()
        it("jitter 전략을 이용하여 테스트한다.") {
            val result = secondWebClient.get().uri("http://localhost:${mockServer.port}/test").retrieve().bodyToMono(String::class.java)
                // 여기는 backoff 전략에 랜덤 변수를 추가로 계산하여 지연시킨다. ex) 첫 번째 -> (2^1) + (2^1 * 0.0 ~ 0.5) 이런식으로?
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).jitter(0.5))
                .block()
            result shouldBe "hello"
        }
    }

    describe("Retry 테스트 다섯 번째") {
        mockServer = MockWebServer()
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
        )
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
        )
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
        )
        mockServer.enqueue(
            MockResponse()
                .setBody("hello")
                .setResponseCode(HttpStatus.OK.value())
        )
        mockServer.start()
        mockServer.url("/test")

        val secondWebClient = ClientConfiguration().secondWebClient()
        it("특정 response status를 받았을 때 재시도한다.") {
            val result = secondWebClient.get().uri("http://localhost:${mockServer.port}/test")
                .retrieve()
                // 500 에러일 때 다음 에러로 변환한다.
                .onStatus({ it.is5xxServerError }, { Mono.error(IllegalStateException("서버 에러")) })
                .bodyToMono(String::class.java)
                .retryWhen(
                    Retry.max(3)
                        // exception 타입이 해당 exception 때
                        .filter { it is IllegalStateException }
                )
                .block()
            result shouldBe "hello"
        }
    }
})