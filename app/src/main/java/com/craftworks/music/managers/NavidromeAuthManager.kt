package com.craftworks.music.managers

import android.annotation.SuppressLint
import android.util.Log
import com.craftworks.music.data.NavidromeProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Serializable
private data class LoginRequest(val username: String, val password: String)

@Serializable
private data class LoginResponse(val token: String? = null)

/**
 * Authenticates against Navidrome's native REST API (`/auth/login`) and hands
 * out Bearer tokens for the native API endpoints via the `X-ND-Authorization`
 * header. Tokens are stored per-server in [NavidromeProvider.jwtToken] and
 * therefore survive app restarts.
 *
 * The dedicated HTTP clients deliberately omit HttpCache so probes and
 * pagination always hit the network.
 */
@Singleton
class NavidromeAuthManager @Inject constructor() {
    companion object {
        private const val TAG = "NAVIDROME_AUTH"
        private const val AUTH_HEADER = "X-ND-Authorization"
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val apiClient: HttpClient by lazy { buildApiHttpClient(insecure = false) }
    private val insecureApiClient: HttpClient by lazy { buildApiHttpClient(insecure = true) }

    /** Picks the client matching the server's self-signed certificate setting. */
    fun clientFor(server: NavidromeProvider): HttpClient =
        if (server.allowSelfSignedCert == true) insecureApiClient else apiClient

    /**
     * Exchanges the stored credentials for a new JWT. Returns the token or
     * null on failure; on success the token is persisted via [NavidromeManager].
     */
    suspend fun login(server: NavidromeProvider): String? = withContext(Dispatchers.IO) {
        try {
            val body = json.encodeToString(LoginRequest(server.username, server.password))
            val response: HttpResponse = clientFor(server).post("${server.url}/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (response.status != HttpStatusCode.OK) {
                Log.w(TAG, "Login failed: HTTP ${response.status} for ${server.url}")
                return@withContext null
            }
            val token = json.decodeFromString<LoginResponse>(response.bodyAsText()).token
            if (token.isNullOrEmpty()) {
                Log.w(TAG, "Login response contained no token for ${server.url}")
                return@withContext null
            }
            NavidromeManager.persistServerToken(server.id, token)
            Log.d(TAG, "Login successful for ${server.url}")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Login error for ${server.url}", e)
            null
        }
    }

    /**
     * Cheaply validates the stored token with a `/api/keepalive/0` heartbeat
     * request, falling back to a full [login] on 401. Returns a usable token
     * or null.
     */
    suspend fun probeStoredToken(server: NavidromeProvider): String? = withContext(Dispatchers.IO) {
        val token = server.jwtToken ?: return@withContext login(server)
        try {
            // 200 -> {"response":"ok","id":"keepalive"}
            // 401 -> {"error":"Not authenticated"} (expired or missing token)
            val response: HttpResponse = clientFor(server).get("${server.url}/api/keepalive/0") {
                header(AUTH_HEADER, "Bearer $token")
            }
            when {
                response.status == HttpStatusCode.OK -> token
                response.status == HttpStatusCode.Unauthorized -> login(server)
                else -> {
                    Log.w(TAG, "Token probe returned HTTP ${response.status} for ${server.url}")
                    token
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token probe error for ${server.url}", e)
            null
        }
    }

    /** Returns a token for the current server, logging in if none is stored. */
    suspend fun ensureValidToken(): String? {
        val server = NavidromeManager.getCurrentServer() ?: return null
        return server.jwtToken ?: login(server)
    }

    @SuppressLint("CustomX509TrustManager")
    private fun buildApiHttpClient(insecure: Boolean): HttpClient {
        val builder = if (insecure) {
            OkHttp.create {
                config {
                    val trustAllCerts = arrayOf<TrustManager>(
                        @SuppressLint("TrustAllX509TrustManager")
                        object : X509TrustManager {
                            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                        }
                    )
                    val sslContext = SSLContext.getInstance("SSL")
                    sslContext.init(null, trustAllCerts, SecureRandom())
                    sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                    hostnameVerifier { _, _ -> true }
                }
            }
        } else {
            OkHttp.create { }
        }

        return HttpClient(builder) {
            install(Logging) {
                level = LogLevel.ALL
                logger = Logger.SIMPLE
                // Keep Bearer tokens out of logcat.
                sanitizeHeader(AUTH_HEADER) { true }
            }
        }
    }
}
