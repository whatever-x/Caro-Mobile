package com.whatever.caro

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform