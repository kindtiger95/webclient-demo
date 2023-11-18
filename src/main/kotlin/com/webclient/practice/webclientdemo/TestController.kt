package com.webclient.practice.webclientdemo

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient

@RestController
@RequestMapping("/test")
class TestController(
    private val firstWebClient: WebClient
) {

    @GetMapping
    fun test() {
        val result = firstWebClient.get()
            .uri("https://591b02cb-ccbb-45bf-9c81-71b48c03691d.mock.pstmn.io/test")
            .retrieve()
            .toEntity(String::class.java)
            .block()
        println(result)
    }
}