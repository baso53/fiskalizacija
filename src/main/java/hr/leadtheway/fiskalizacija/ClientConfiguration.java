package hr.leadtheway.fiskalizacija;

import hr.leadtheway.wsdl.FiskalizacijaPortType;
import hr.leadtheway.wsdl.FiskalizacijaService;
import jakarta.xml.ws.BindingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.security.cert.X509Certificate;
import java.util.List;

import static hr.leadtheway.fiskalizacija.KeyUtils.loadPKCS12;
import static hr.leadtheway.fiskalizacija.KeyUtils.loadX509;

@Configuration
public class ClientConfiguration {

    private final Resource p12PrivateKeyFile;
    private final String p12StorePass;
    private final String p12KeyAlias;
    private final String p12KeyPass;
    private final Resource responsePublicKeyFile;

    public ClientConfiguration(
            @Value("${fina.keystore.path}") Resource p12PrivateKeyFile,
            @Value("${fina.keystore.storepass}") String p12StorePass,
            @Value("${fina.keystore.alias}") String p12KeyAlias,
            @Value("${fina.keystore.keypass}") String p12KeyPass,
            @Value("${fina.response.public-key}") Resource responsePublicKeyFile
    ) {
        this.p12PrivateKeyFile = p12PrivateKeyFile;
        this.p12StorePass = p12StorePass;
        this.p12KeyAlias = p12KeyAlias;
        this.p12KeyPass = p12KeyPass;
        this.responsePublicKeyFile = responsePublicKeyFile;
    }

    @Bean
    public FiskalizacijaPortType fiskalizacijaPort() throws Exception {
        var port = new FiskalizacijaService().getFiskalizacijaPortType();

        var p12 = loadPKCS12(
                p12PrivateKeyFile,
                p12StorePass.toCharArray(),
                p12KeyAlias,
                p12KeyPass.toCharArray()
        );

        var outboundSignatureHandler = new XmlSignatureOutboundHandler(p12.getPrivateKey(), (X509Certificate) p12.getCertificate());
        var inboundSignatureHandler = new XmlSignatureInboundHandler(loadX509(responsePublicKeyFile));

        if (port instanceof BindingProvider bindingProvider) {
            bindingProvider.getBinding().setHandlerChain(List.of(outboundSignatureHandler));
        } else {
            throw new IllegalArgumentException("Unsupported port");
        }

        return port;
    }
}