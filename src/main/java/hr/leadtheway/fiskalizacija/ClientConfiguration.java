package hr.leadtheway.fiskalizacija;

import hr.leadtheway.wsdl.FiskalizacijaPortType;
import hr.leadtheway.wsdl.FiskalizacijaService;
import jakarta.xml.ws.BindingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.security.KeyStore.PrivateKeyEntry;
import java.util.Collections;

import static hr.leadtheway.fiskalizacija.KeyStoreUtil.loadPKCS12;

@Configuration
public class ClientConfiguration {

    private final Resource p12Path;
    private final String storePass;
    private final String alias;
    private final String keyPass;
    private final String endpointUrl;

    public ClientConfiguration(
            @Value("${fina.keystore.path}") Resource p12Path,
            @Value("${fina.keystore.storepass}") String storePass,
            @Value("${fina.keystore.alias}") String alias,
            @Value("${fina.keystore.keypass}") String keyPass,
            @Value("${fina.endpoint.url}") String endpointUrl
    ) {
        this.p12Path = p12Path;
        this.storePass = storePass;
        this.alias = alias;
        this.keyPass = keyPass;
        this.endpointUrl = endpointUrl;
    }

    @Bean
    public PrivateKeyEntry fiscalKeyEntry() throws Exception {
        return loadPKCS12(
                p12Path,
                storePass.toCharArray(),
                alias,
                keyPass.toCharArray()
        );
    }

    @Bean
    public XmlSignatureOutboundHandler signatureHandler(PrivateKeyEntry fiscalKeyEntry) {
        return new XmlSignatureOutboundHandler(fiscalKeyEntry);
    }

    @Bean
    public FiskalizacijaPortType fiskalizacijaPort(hr.leadtheway.fiskalizacija.XmlSignatureOutboundHandler signatureHandler) {
        var service = new FiskalizacijaService();
        var port = service.getFiskalizacijaPortType();

        if (port instanceof BindingProvider bindingProvider) {
            bindingProvider.getBinding().setHandlerChain(Collections.singletonList(signatureHandler));
            bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
        } else {
            throw new IllegalArgumentException("Unsupported port");
        }

        return port;
    }
}