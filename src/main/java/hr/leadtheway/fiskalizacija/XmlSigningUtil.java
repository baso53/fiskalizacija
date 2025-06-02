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
}