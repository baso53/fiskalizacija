package hr.leadtheway.fiskalizacija;

import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import lombok.RequiredArgsConstructor;
import org.apache.xml.security.Init;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Set;

import static jakarta.xml.soap.SOAPConstants.URI_NS_SOAP_ENVELOPE;
import static jakarta.xml.ws.handler.MessageContext.MESSAGE_OUTBOUND_PROPERTY;
import static java.lang.Boolean.TRUE;
import static org.apache.xml.security.algorithms.MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA1;
import static org.apache.xml.security.c14n.Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS;
import static org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1;
import static org.apache.xml.security.transforms.Transforms.TRANSFORM_ENVELOPED_SIGNATURE;

@RequiredArgsConstructor
public class XmlSignatureOutboundHandler implements SOAPHandler<SOAPMessageContext> {

    static {
        Init.init();
    }

    private final PrivateKey privateKey;
    private final X509Certificate certificate;

    @Override
    public boolean handleMessage(SOAPMessageContext ctx) {
        var outbound = (Boolean) ctx.get(MESSAGE_OUTBOUND_PROPERTY);
        if (TRUE.equals(outbound)) {
            try {
                var soapMessage = ctx.getMessage().getSOAPPart();
                var body = (Element) soapMessage.getElementsByTagNameNS(URI_NS_SOAP_ENVELOPE, "Body").item(0);
                // the first element inside the Body is our business root element
                var root = (Element) body.getFirstChild();

                var doc = root.getOwnerDocument();

                // 1) make sure the Id attribute is an XML ID
                if (!root.hasAttribute("Id")) {
                    root.setAttribute("Id", root.getLocalName()); // e.g. “RacunZahtjev”
                }
                root.setIdAttribute("Id", true);

                // 2) build the signature
                var sig = new XMLSignature(
                        doc,
                        "",                                              // baseURI
                        ALGO_ID_SIGNATURE_RSA_SHA1,
                        ALGO_ID_C14N_EXCL_OMIT_COMMENTS
                );

                // 3) add canonicalisation + enveloped transforms
                var transforms = new Transforms(doc);
                transforms.addTransform(TRANSFORM_ENVELOPED_SIGNATURE);
                transforms.addTransform(ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

                sig.addDocument(
                        "#" + root.getAttribute("Id"),
                        transforms,
                        ALGO_ID_DIGEST_SHA1
                );

                // 4) embed the certificate
                sig.addKeyInfo(certificate);
                sig.addKeyInfo(certificate.getPublicKey());

                // 5) append the <Signature> element inside the signed root
                root.appendChild(sig.getElement());

                // 6) calculate the signature value
                sig.sign(privateKey);

                ctx.getMessage().saveChanges();
            } catch (Exception ex) {
                throw new RuntimeException("XML signature creation failed", ex);
            }
        }
        return true;
    }

    // ---- boiler-plate ↓ ----------------------------------------------------
    @Override
    public boolean handleFault(SOAPMessageContext ctx) {
        return true;
    }

    @Override
    public void close(MessageContext ctx) {
    }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }
}