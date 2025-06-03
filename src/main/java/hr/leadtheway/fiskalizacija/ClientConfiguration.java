package hr.leadtheway.fiskalizacija;

import hr.leadtheway.wsdl.FiskalizacijaPortType;
import hr.leadtheway.wsdl.FiskalizacijaService;
import jakarta.xml.ws.BindingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.security.KeyStore.PrivateKeyEntry;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import static java.security.KeyStore.getInstance;

@Configuration
public class ClientConfiguration {

    private final Resource p12PrivateKeyFile;
    private final char[] p12StorePass;
    private final String p12KeyAlias;
    private final char[] p12KeyPass;

    public ClientConfiguration(
            @Value("${fina.keystore.path}") Resource p12PrivateKeyFile,
            @Value("${fina.keystore.storepass}") char[] p12StorePass,
            @Value("${fina.keystore.alias}") String p12KeyAlias,
            @Value("${fina.keystore.keypass}") char[] p12KeyPass
    ) {
        this.p12PrivateKeyFile = p12PrivateKeyFile;
        this.p12StorePass = p12StorePass;
        this.p12KeyAlias = p12KeyAlias;
        this.p12KeyPass = p12KeyPass;
    }

    @Bean
    public PrivateKeyEntry p12PrivateKey() throws Exception {
        var ks = getInstance("PKCS12");
        try (var fis = p12PrivateKeyFile.getInputStream()) {
            ks.load(fis, p12StorePass);
        }

        Arrays.fill(p12StorePass, '\0');

        var key = ks.getKey(p12KeyAlias, p12KeyPass);
        Arrays.fill(p12KeyPass, '\0');
        var cert = ks.getCertificate(p12KeyAlias);

        return new PrivateKeyEntry((PrivateKey) key, new Certificate[]{cert});
    }

    @Bean
    public FiskalizacijaPortType fiskalizacijaPort(PrivateKeyEntry privateKeyEntry) {
        var port = new FiskalizacijaService().getFiskalizacijaPortType();

        var outboundSignatureHandler = new XmlSignatureOutboundHandler(privateKeyEntry.getPrivateKey(), (X509Certificate) privateKeyEntry.getCertificate());

        if (port instanceof BindingProvider bindingProvider) {
            bindingProvider.getBinding().setHandlerChain(List.of(outboundSignatureHandler));
        } else {
            throw new IllegalArgumentException("Unsupported port");
        }

        return port;
    }
}