package hr.leadtheway.fiskalizacija;

import org.springframework.core.io.Resource;

import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static java.security.KeyStore.getInstance;

public final class KeyUtils {

    private KeyUtils() {
    }

    public static PrivateKeyEntry loadPKCS12(
            Resource resource,
            char[] storePassword,
            String alias,
            char[] keyPassword
    ) throws Exception {
        var ks = getInstance("PKCS12");
        try (var fis = resource.getInputStream()) {
            ks.load(fis, storePassword);
        }

        var key = ks.getKey(alias, keyPassword);
        var cert = ks.getCertificate(alias);

        return new PrivateKeyEntry((PrivateKey) key, new Certificate[]{cert});
    }

    public static X509Certificate loadX509(Resource resource) throws Exception {
        var cf = CertificateFactory.getInstance("X.509");
        try (var in = resource.getInputStream()) {
            return (X509Certificate) cf.generateCertificate(in);
        }
    }
}