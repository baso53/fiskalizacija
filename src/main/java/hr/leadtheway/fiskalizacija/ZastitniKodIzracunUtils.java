
package hr.leadtheway.fiskalizacija;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.security.PrivateKey;
import java.security.Signature;

/**
 * ZastitniKodIzracun - klasa za izračun zaštitnog broja napisana tako da prati
 * pseudokod.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ZastitniKodIzracunUtils {
    public static String calculate(
            String oib,
            String datumIVrijemeIzdavanjaRacuna,
            String brojcanaOznakaRacuna,
            String oznakaPoslovnogProstora,
            String oznakaNaplatnogUredaja,
            String ukupniIznosRacuna,
            PrivateKey privateKey
    ) {
        var medjurezultat = oib;
        // medjurezultat = medjurezultat + datVrij
        medjurezultat = medjurezultat + datumIVrijemeIzdavanjaRacuna;
        // medjurezultat = medjurezultat + bor
        medjurezultat = medjurezultat + brojcanaOznakaRacuna;
        // pročitaj (opp – oznaka poslovnog prostora)
        // medjurezultat = medjurezultat + opp
        medjurezultat = medjurezultat + oznakaPoslovnogProstora;
        // pročitaj (onu – oznaka naplatnog uređaja)
        // medjurezultat = medjurezultat + onu
        medjurezultat = medjurezultat + oznakaNaplatnogUredaja;
        // pročitaj ( uir - ukupni iznos računa )
        // medjurezultat = medjurezultat + uir
        medjurezultat = medjurezultat + ukupniIznosRacuna;
        // elektronički potpiši medjurezultat koristeći RSA-SHA1 potpis
        byte[] potpisano = null;
        try {
            var biljeznik = Signature.getInstance("SHA1withRSA");
            biljeznik.initSign(privateKey);
            biljeznik.update(medjurezultat.getBytes());
            potpisano = biljeznik.sign();
        } catch (Exception e) {
            // nije uspjelo čitanje privatnog ključa
            e.printStackTrace();
        }
        // rezultatIspis = izračunajMD5(elektronički potpisani medjurezultat)
        var rezultatIspis = DigestUtils.md5Hex(potpisano);
        // kraj
        log.info("Dobiveni 32-znamenkasti zaštitni kod je: {}", rezultatIspis);

        return rezultatIspis;
    }
}