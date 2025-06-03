
package hr.leadtheway.fiskalizacija;

import hr.leadtheway.wsdl.BrojRacunaType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.security.PrivateKey;
import java.security.Signature;
import java.time.LocalDateTime;
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
            LocalDateTime datumIVrijemeIzdavanjaRacuna,
            BrojRacunaType brRac,
            String ukupniIznosRacuna,
            PrivateKey privateKey
    ) {
        var medjurezultat = oib;
        medjurezultat = medjurezultat + DATE_TIME_FORMATTER.format(datumIVrijemeIzdavanjaRacuna);
        medjurezultat = medjurezultat + brRac.getBrOznRac();
        medjurezultat = medjurezultat + brRac.getOznPosPr();
        medjurezultat = medjurezultat + brRac.getOznNapUr();
        medjurezultat = medjurezultat + ukupniIznosRacuna;
        byte[] potpisano = null;
        try {
            var biljeznik = Signature.getInstance("SHA1withRSA");
            biljeznik.initSign(privateKey);
            biljeznik.update(medjurezultat.getBytes());
            potpisano = biljeznik.sign();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return DigestUtils.md5Hex(potpisano);
    }
}