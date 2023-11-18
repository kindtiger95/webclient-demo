package com.webclient.practice.webclientdemo.exception

class CustomException {
    class Response4xxError : RuntimeException()
    class Response5xxError : RuntimeException()
}