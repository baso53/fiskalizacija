package hr.leadtheway.fiskalizacija;

import hr.leadtheway.wsdl.BrojRacunaType;
import hr.leadtheway.wsdl.FiskalizacijaPortType;
import hr.leadtheway.wsdl.NacinPlacanjaType;
import hr.leadtheway.wsdl.OznakaSlijednostiType;
import hr.leadtheway.wsdl.PdvType;
import hr.leadtheway.wsdl.PorezType;
import hr.leadtheway.wsdl.RacunPDType;
import hr.leadtheway.wsdl.RacunPDZahtjev;
import hr.leadtheway.wsdl.ZaglavljeType;
import jakarta.xml.bind.JAXBElement;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.xml.namespace.QName;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestService {

    private final FiskalizacijaPortType fiskalizacijaPortType;

    @Scheduled(initialDelay = 50000, fixedDelay = 50000)
    public void scheduled() {
        var zahtjevId = "racunPDId";
        var idPoruke = "3541f671-da0a-44ae-bf3b-800d63e454b9";
        var zagDatumVrijeme = "24.06.2025T09:59:43";  // wtf is this format

        var zaglavlje = ZaglavljeType.builder()
                .idPoruke(idPoruke)
                .datumVrijeme(zagDatumVrijeme)
                .build();

        var oib = "98765432198";
        boolean uSustPdv = true;
        var datVrijeme = "24.06.2025T09:59:43";  // wtf is this format
        var oznSlijed = OznakaSlijednostiType.P;

        var brRac = BrojRacunaType.builder()
                .brOznRac("25")
                .oznPosPr("POSL1")
                .oznNapUr("12")
                .build();

        var pdv = PdvType.builder()
                .porez(List.of(
                        PorezType.builder()
                                .stopa("25.00")
                                .osnovica("29.00")
                                .iznos("7.25")
                                .build()
                ))
                .build();

        var iznosUkupno = "36.25";
        var nacinPlac = NacinPlacanjaType.K;
        var oibOper = "01234567890";
        var zastKod = "c61f548d7fcbc17e7d1bee52740b5518";
        boolean nakDost = false;

        var racunPd = RacunPDType.builder()
                .oib(oib)
                .uSustPdv(uSustPdv)
                .datVrijeme(datVrijeme)
                .oznSlijed(oznSlijed)
                .brRac(brRac)
                .pdv(pdv)
                .iznosUkupno(iznosUkupno)
                .nacinPlac(nacinPlac)
                .oibOper(oibOper)
                .zastKod(zastKod)
                .nakDost(nakDost)
                .prateciDokument(RacunPDType.PrateciDokument.builder()
                        .jirPDOrZastKodPD(List.of(
                                new JAXBElement<>(
                                        new QName("http://www.apis-it.hr/fin/2012/types/f73", "ZastKodPD"),
                                        String.class,
                                        "9d0dbe601dff64ebf2000ce1f943dc8f"
                                )
                        ))
                        .build())
                .build();

        var zahtjev = RacunPDZahtjev.builder()
                .id(zahtjevId)
                .zaglavlje(zaglavlje)
                .racun(racunPd)
                .build();

        var response = fiskalizacijaPortType.racuniPD(zahtjev);

        var a = 1;
    }

    @Scheduled(initialDelay = 50000, fixedDelay = 50000)
    public void testEcho() {
        var response = fiskalizacijaPortType.echo("a");

        var a = 1;
    }
}
