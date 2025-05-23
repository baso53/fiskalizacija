package hr.leadtheway.fiskalizacija;

import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Set;

import static hr.leadtheway.fiskalizacija.XmlSigner.sign;
import static jakarta.xml.soap.SOAPConstants.URI_NS_SOAP_ENVELOPE;
import static jakarta.xml.ws.handler.MessageContext.MESSAGE_OUTBOUND_PROPERTY;
import static java.lang.Boolean.TRUE;

public class XmlSignatureOutboundHandler implements SOAPHandler<SOAPMessageContext> {

    private final PrivateKey privateKey;
    private final X509Certificate certificate;

    public XmlSignatureOutboundHandler(KeyStore.PrivateKeyEntry entry) {
        this.privateKey  = entry.getPrivateKey();
        this.certificate = (X509Certificate) entry.getCertificate();
    }

    @Override
    public boolean handleMessage(SOAPMessageContext ctx) {
        var outbound = (Boolean) ctx.get(MESSAGE_OUTBOUND_PROPERTY);
        if (TRUE.equals(outbound)) {
            try {
                var doc = ctx.getMessage().getSOAPPart();
                var body = (Element) doc.getElementsByTagNameNS(URI_NS_SOAP_ENVELOPE, "Body")
                        .item(0);
                // the first element inside the Body is our business root element
                var root = (Element) body.getFirstChild();

                sign(root, privateKey, certificate);

                ctx.getMessage().saveChanges();
            } catch (Exception ex) {
                throw new RuntimeException("XML signature creation failed", ex);
            }
        }
        return true;
    }

    // ---- boiler-plate ↓ ----------------------------------------------------
    @Override public boolean handleFault(SOAPMessageContext ctx){return true;}
    @Override public void close(MessageContext ctx) {}
    @Override public Set<QName> getHeaders(){return null;}
}