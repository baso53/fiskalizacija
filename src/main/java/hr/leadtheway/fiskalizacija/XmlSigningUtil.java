package hr.leadtheway.fiskalizacija;

import org.apache.xml.security.Init;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.w3c.dom.Element;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.apache.xml.security.algorithms.MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA1;
import static org.apache.xml.security.c14n.Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS;
import static org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1;
import static org.apache.xml.security.transforms.Transforms.TRANSFORM_ENVELOPED_SIGNATURE;
import static org.apache.xml.security.utils.Constants.SignatureSpecNS;
import static org.apache.xml.security.utils.Constants._TAG_SIGNATURE;

public final class XmlSigningUtil {

    static {
        Init.init();
    }

    private XmlSigningUtil() {
    }

    public static void sign(Element root, PrivateKey privateKey, X509Certificate cert) throws Exception {
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
        sig.addKeyInfo(cert);
        sig.addKeyInfo(cert.getPublicKey());

        // 5) append the <Signature> element inside the signed root
        root.appendChild(sig.getElement());

        // 6) calculate the signature value
        sig.sign(privateKey);
    }

    /**
     * Locate the first <Signature> element under the given root
     * and verify it against the supplied certificate.
     *
     * @param root the element that was signed (and which holds the Signature child)
     * @param cert the X509Certificate to verify the signature with
     * @throws Exception on any problem or if the signature does not verify
     */
    public static void verify(Element root, X509Certificate cert) throws Exception {
        // Find the Signature element in the XMLDSig namespace
        var nl = root.getElementsByTagNameNS(
                SignatureSpecNS,
                _TAG_SIGNATURE
        );
        if (nl.getLength() == 0) {
            throw new Exception("No XML Signature element found under " + root.getLocalName());
        }

        var sigElem = (Element) nl.item(0);
        var signature = new XMLSignature(sigElem, "");

        // This will validate both the Reference digests and the actual signature value
        var coreValidity = signature.checkSignatureValue(cert);

        if (!coreValidity) {
            throw new Exception("XML Signature verification failed for element "
                                + root.getLocalName());
        }
    }
}