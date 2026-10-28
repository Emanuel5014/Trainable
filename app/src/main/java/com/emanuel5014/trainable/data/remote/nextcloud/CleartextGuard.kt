package com.emanuel5014.trainable.data.remote.nextcloud

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.net.UnknownServiceException

/**
 * Keeps Nextcloud https-only. Plain `http` used to be refused by Android itself; the app now allows
 * cleartext for the remote AI on the user's own network, and a Nextcloud login must not ride along.
 * It throws what the system threw, so every existing `catch` in the client behaves as before.
 */
object CleartextGuard : Interceptor {

    fun check(url: HttpUrl) {
        if (url.scheme == "http") {
            throw UnknownServiceException("CLEARTEXT communication to ${url.host} not permitted by network security policy")
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        check(chain.request().url)
        return chain.proceed(chain.request())
    }
}
