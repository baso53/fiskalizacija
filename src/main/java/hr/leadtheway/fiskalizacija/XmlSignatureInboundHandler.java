package hr.leadtheway.fiskalizacija;

import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import lombok.RequiredArgsConstructor;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.security.cert.X509Certificate;
import java.util.Set;

import static hr.leadtheway.fiskalizacija.XmlSigningUtil.verify;
import static jakarta.xml.ws.handler.MessageContext.MESSAGE_OUTBOUND_PROPERTY;
import static java.lang.Boolean.FALSE;

@RequiredArgsConstructor
public class XmlSignatureInboundHandler implements SOAPHandler<SOAPMessageContext> {

    private final X509Certificate certificate;

    @Override
    public boolean handleMessage(SOAPMessageContext ctx) {
        // If this is an inbound message (i.e. a response), check signature
        var outbound = (Boolean) ctx.get(MESSAGE_OUTBOUND_PROPERTY);
        if (FALSE.equals(outbound)) {
            try {
                var msg = ctx.getMessage();
                var part = msg.getSOAPPart();

                // locate the Body and its first child (your business element)
                var body = part.getEnvelope().getBody();
                var root = (Element) body.getFirstChild();

                root.setIdAttribute("Id", true);

                // verify the <Signature> under that root element
                verify(root, certificate);

            } catch (Exception e) {
                throw new RuntimeException("XML Signature verification failed", e);
            }
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        // you may also wish to validate faults
        return true;
    }

    @Override
    public void close(MessageContext context) { }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }
}