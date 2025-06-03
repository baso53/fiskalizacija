
package hr.leadtheway.fiskalizacija;

import hr.leadtheway.wsdl.BrojRacunaType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.security.PrivateKey;
import java.security.Signature;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ZastitniKodIzracun - klasa za izračun zaštitnog broja napisana tako da prati
 * pseudokod.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ZastitniKodIzracunUtils {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy' 'HH:mm:ss");

    public static String calculate(
            String oib,
            ZonedDateTime datumIVrijemeIzdavanjaRacuna,
            BrojRacunaType brRac,
            String ukupniIznosRacuna,
            PrivateKey privateKey
    ) {
        var medjurezultat = oib;
        // medjurezultat = medjurezultat + datVrij
        medjurezultat = medjurezultat + DATE_TIME_FORMATTER.format(datumIVrijemeIzdavanjaRacuna);
        // medjurezultat = medjurezultat + bor
        medjurezultat = medjurezultat + brRac.getBrOznRac();
        // pročitaj (opp – oznaka poslovnog prostora)
        // medjurezultat = medjurezultat + opp
        medjurezultat = medjurezultat + brRac.getOznPosPr();
        // pročitaj (onu – oznaka naplatnog uređaja)
        // medjurezultat = medjurezultat + onu
        medjurezultat = medjurezultat + brRac.getOznNapUr();
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