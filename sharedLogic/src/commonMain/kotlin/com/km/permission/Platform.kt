package com.km.permission

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform