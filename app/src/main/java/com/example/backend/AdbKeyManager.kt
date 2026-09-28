package com.example.backend

import android.content.Context
import android.os.Build
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import org.conscrypt.Conscrypt
import java.io.File
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import sun.security.x509.AlgorithmId
import sun.security.x509.CertificateAlgorithmId
import sun.security.x509.CertificateIssuerName
import sun.security.x509.CertificateSerialNumber
import sun.security.x509.CertificateSubjectName
import sun.security.x509.CertificateValidity
import sun.security.x509.CertificateVersion
import sun.security.x509.CertificateX509Key
import sun.security.x509.X500Name
import sun.security.x509.X509CertImpl
import sun.security.x509.X509CertInfo

/**
 * A real, persisted ADB identity (RSA keypair + self-signed cert), the same shape every ADB
 * client (including Shizuku's own wireless-debugging path) uses to authorize with a target's
 * `adbd`. Generated once per app install and reused afterward, so the target only has to
 * authorize this app a single time.
 */
class AdbKeyManager private constructor(context: Context) : AbsAdbConnectionManager() {

    private val privKeyFile = File(context.filesDir, "afs_adbkey")
    private val certFile = File(context.filesDir, "afs_adbkey.cert")

    private val keyPair: Pair<PrivateKey, Certificate> by lazy { loadOrGenerate() }

    init {
        setApi(Build.VERSION.SDK_INT)
    }

    override fun getPrivateKey(): PrivateKey = keyPair.first
    override fun getCertificate(): Certificate = keyPair.second
    override fun getDeviceName(): String = "AFS-" + Build.MODEL.replace(" ", "_")

    private fun loadOrGenerate(): Pair<PrivateKey, Certificate> {
        if (privKeyFile.exists() && certFile.exists()) {
            try {
                val privBytes = privKeyFile.readBytes()
                val certBytes = certFile.readBytes()
                val priv = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(privBytes))
                val cert = CertificateFactory.getInstance("X.509").generateCertificate(certBytes.inputStream())
                return Pair(priv, cert)
            } catch (_: Exception) {
                // Fall through and regenerate a fresh identity.
            }
        }
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048, SecureRandom.getInstance("SHA1PRNG"))
        val pair = generator.generateKeyPair()
        val privateKey = pair.private
        val publicKey = pair.public

        val subject = "CN=Android Flashing Shortcuts"
        val algorithmName = "SHA512withRSA"
        val notBefore = Date()
        val notAfter = Date(System.currentTimeMillis() + 100L * 365 * 24 * 60 * 60 * 1000)
        val x500Name = X500Name(subject)
        val info = X509CertInfo()
        info.set("version", CertificateVersion(CertificateVersion.V3))
        info.set("serialNumber", CertificateSerialNumber(java.util.Random().nextInt().and(Int.MAX_VALUE)))
        info.set("algorithmID", CertificateAlgorithmId(AlgorithmId.get(algorithmName)))
        info.set("subject", CertificateSubjectName(x500Name))
        info.set("key", CertificateX509Key(publicKey))
        info.set("validity", CertificateValidity(notBefore, notAfter))
        info.set("issuer", CertificateIssuerName(x500Name))
        val cert = X509CertImpl(info)
        cert.sign(privateKey, algorithmName)

        try {
            privKeyFile.writeBytes(privateKey.encoded)
            certFile.writeBytes(cert.encoded)
        } catch (_: Exception) {
        }
        return Pair(privateKey, cert as Certificate)
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
