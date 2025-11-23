package io.github.dionisioc.kingandpeasant.strategy

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class KingStrategy(
    private val keyStorePath: Path,
    private val keyStorePassword: String,
    private val trustStorePath: Path,
    private val trustStorePassword: String,
    private val apiToken: String
) : AuthStrategy {

    override fun configureClient(builder: HttpClient.Builder): HttpClient.Builder {
        val keyStore = KeyStore.getInstance("PKCS12")
        Files.newInputStream(keyStorePath).use {
            keyStore.load(it, keyStorePassword.toCharArray())
        }
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, keyStorePassword.toCharArray())

        val trustStore = KeyStore.getInstance("PKCS12")
        Files.newInputStream(trustStorePath).use {
            trustStore.load(it, trustStorePassword.toCharArray())
        }
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(trustStore)

        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(kmf.keyManagers, tmf.trustManagers, SecureRandom())

        return builder.sslContext(sslContext)
    }

    override fun applyToRequest(builder: HttpRequest.Builder): HttpRequest.Builder {
        return builder.header("Authorization", "Bearer $apiToken")
    }
}