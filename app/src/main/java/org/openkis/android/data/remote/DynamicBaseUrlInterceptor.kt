package org.openkis.android.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicBaseUrlInterceptor @Inject constructor() : Interceptor {

    @Volatile
    var baseUrl: String = ""

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (baseUrl.isNotBlank()) {
            val newBaseUrl = baseUrl.trimEnd('/').toHttpUrlOrNull()
            if (newBaseUrl != null) {
                val newUrl = request.url.newBuilder()
                    .scheme(newBaseUrl.scheme)
                    .host(newBaseUrl.host)
                    .port(newBaseUrl.port)
                    .encodedPath(
                        newBaseUrl.encodedPath.trimEnd('/') + "/" +
                                request.url.encodedPath.trimStart('/')
                    )
                    .build()
                request = request.newBuilder().url(newUrl).build()
            }
        }

        return chain.proceed(request)
    }
}
