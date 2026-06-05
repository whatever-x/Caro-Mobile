@file:OptIn(ExperimentalForeignApi::class)

package com.whatever.caro.feature.login.provider

import GoogleSignIn.GIDSignIn
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL

fun handleGoogleSignInOpenURL(url: NSURL): Boolean = GIDSignIn.sharedInstance.handleURL(url)
