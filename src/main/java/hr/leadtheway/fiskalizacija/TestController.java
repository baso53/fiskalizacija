package hr.leadtheway.fiskalizacija;

import hr.leadtheway.wsdl.BrojRacunaType;
import hr.leadtheway.wsdl.FiskalizacijaPortType;
import hr.leadtheway.wsdl.NacinPlacanjaType;
import hr.leadtheway.wsdl.OznakaSlijednostiType;
import hr.leadtheway.wsdl.PdvType;
import hr.leadtheway.wsdl.PorezType;
import hr.leadtheway.wsdl.ProvjeraZahtjev;
import hr.leadtheway.wsdl.RacunType;
import hr.leadtheway.wsdl.ZaglavljeType;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class TestController {

    private final FiskalizacijaPortType fiskalizacijaPortType;
    private final Validator validator;

    public void validate(Object object) {
        var violations = validator.validate(object);

        if (!violations.isEmpty()) {
            var a = 1;
        }
    }

    @GetMapping("/echo")
    public String echo() {
        var response = fiskalizacijaPortType.echo("echo-test");

        return response;
    }

    @GetMapping("/racun")
    public String racun() {

        var zahtjevId = "racunPDId";
        var idPoruke = "3541f671-da0a-44ae-bf3b-800d63e454b9";
        var zagDatumVrijeme = "24.06.2025T09:59:43";  // wtf is this format

        var zaglavlje = ZaglavljeType.builder()
                .idPoruke(idPoruke)
                .datumVrijeme(zagDatumVrijeme)
                .build();

        var oib = "00797008853";
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

        var racunPd = RacunType.builder()
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
                .build();

        var zahtjev = ProvjeraZahtjev.builder()
                .id(zahtjevId)
                .zaglavlje(zaglavlje)
                .racun(racunPd)
                .build();

        validate(zahtjev);

        var response = fiskalizacijaPortType.provjera(zahtjev);

        return "";
    }
}
