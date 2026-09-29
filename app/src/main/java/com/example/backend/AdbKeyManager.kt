package com.example.backend

import android.content.Context
import android.os.Build
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.conscrypt.Conscrypt
import java.io.File
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date

/**
 * A real, persisted ADB identity (RSA keypair + self-signed cert), the same shape every ADB
 * client (including Shizuku's own wireless-debugging path) uses to authorize with a target's
 * `adbd`. Generated once per app install and reused afterward, so the target only has to
 * authorize this app a single time.
 */
class AdbKeyManager private constructor(context: Context) : AbsAdbConnectionManager() {

    private val privKeyFile = File(context.filesDir, "afs_adbkey")
    private val certFile = File(context.filesDir, "afs_adbkey.cert")

    private val identity: Pair<PrivateKey, Certificate> by lazy { loadOrGenerate() }

    init {
        setApi(Build.VERSION.SDK_INT)
    }

    override fun getPrivateKey(): PrivateKey = identity.first
    override fun getCertificate(): Certificate = identity.second
    override fun getDeviceName(): String = "AFS-" + Build.MODEL.replace(" ", "_")

    private fun loadOrGenerate(): Pair<PrivateKey, Certificate> {
        if (privKeyFile.exists() && certFile.exists()) {
            try {
                val priv = KeyFactory.getInstance("RSA")
                    .generatePrivate(PKCS8EncodedKeySpec(privKeyFile.readBytes()))
                val cert = CertificateFactory.getInstance("X.509")
                    .generateCertificate(certFile.readBytes().inputStream())
                return Pair(priv, cert)
            } catch (_: Exception) {
                // Fall through and regenerate a fresh identity.
            }
        }
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048, SecureRandom())
        val pair = generator.generateKeyPair()
        val cert = generateSelfSignedCert(pair)
        try {
            privKeyFile.writeBytes(pair.private.encoded)
            certFile.writeBytes(cert.encoded)
        } catch (_: Exception) {
        }
        return Pair(pair.private, cert)
    }

    /** Self-signed cert via BouncyCastle - avoids sun.security.x509, which JDK 9+ module rules block at compile time. */
    private fun generateSelfSignedCert(pair: KeyPair): Certificate {
        val now = System.currentTimeMillis()
        val notBefore = Date(now - 24L * 60 * 60 * 1000)
        val notAfter = Date(now + 100L * 365 * 24 * 60 * 60 * 1000)
        val owner = X500Name("CN=Android Flashing Shortcuts")
        val serial = BigInteger(64, SecureRandom())
        val builder = JcaX509v3CertificateBuilder(owner, serial, notBefore, notAfter, owner, pair.public)
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(pair.private)
        return JcaX509CertificateConverter().getCertificate(builder.build(signer))
    }

    companion object {
        @Volatile private var instance: AdbKeyManager? = null
        @Volatile private var conscryptRegistered = false

        fun getInstance(context: Context): AdbKeyManager {
            registerConscrypt()
            return instance ?: synchronized(this) {
                instance ?: AdbKeyManager(context.applicationContext).also { instance = it }
            }
        }

        private fun registerConscrypt() {
            if (conscryptRegistered) return
            synchronized(this) {
                if (conscryptRegistered) return
                try {
                    Security.insertProviderAt(Conscrypt.newProvider(), 1)
                } catch (_: Throwable) {
                }
                conscryptRegistered = true
            }
        }
    }
}
