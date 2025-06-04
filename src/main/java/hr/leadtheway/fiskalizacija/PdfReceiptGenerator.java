package hr.leadtheway.fiskalizacija;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import hr.leadtheway.wsdl.BrojRacunaType;
import hr.leadtheway.wsdl.NacinPlacanjaType;
import lombok.Builder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.google.zxing.BarcodeFormat.QR_CODE;
import static com.google.zxing.EncodeHintType.ERROR_CORRECTION;
import static com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage;
import static com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.Q;
import static org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode.APPEND;
import static org.apache.pdfbox.pdmodel.common.PDRectangle.A4;
import static org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromByteArray;

@Service
public final class PdfReceiptGenerator {

    private final Resource regularFontFile;
    private final Resource boldFontFile;

    public PdfReceiptGenerator(
            @Value("${pdf.fonts.regular}") Resource regularFontFile,
            @Value("${pdf.fonts.bold}") Resource boldFontFile
    ) {
        this.regularFontFile = regularFontFile;
        this.boldFontFile = boldFontFile;
    }

    /* ────────── string literals ────────── */
    private static final String QR_URL_BASE = "https://porezna.gov.hr/rn?";
    private static final String JIR_PARAM = "jir=";
    private static final String ZKI_PARAM = "zki=";
    private static final String DATE_PARAM = "datv=";
    private static final String AMOUNT_PARAM = "izn=";
    private static final String RACUN_TITLE = "Račun";
    private static final String BROJ_FAKTURE_LABEL = "Broj fakture: ";
    private static final String DATUM_IZDAVANJA_LABEL = "Datum izdavanja računa: ";
    private static final String VRIJEME_IZDAVANJA_LABEL = "Vrijeme izdavanja računa: ";
    private static final String NACIN_PLACANJA_LABEL = "Način plaćanja: ";
    private static final String HEADER_OPIS = "Opis";
    private static final String HEADER_KOLICINA = "Količina";
    private static final String HEADER_POREZ = "Porez";
    private static final String HEADER_IZNOS_POREZA = "Iznos poreza";
    private static final String HEADER_NETO_IZNOS = "Neto iznos";
    private static final String UKUPAN_NETO_IZNOS_LABEL = "Ukupan neto iznos: ";
    private static final String UKUPAN_PLACANJE_IZNOS_LABEL = "Ukupan iznos za plaćanje: ";
    private static final String JIR_LABEL = "JIR: ";
    private static final String ZKI_LABEL = "ZKI: ";
    private static final String RACUN_IZDAO_LABEL = "Račun izdao: ";
    private static final String FOOTER_LINE1 = "Izdao/la u ime dobavljača ..., obrt za usluge, vl. ...";
    private static final String FOOTER_LINE2 = "Second footer line.";
    private static final String QR_IMG_NAME = "qr";
    private static final String PNG_FORMAT = "png";
    private static final String EUR_SUFFIX = " EUR";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter QR_DATETIME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    /* ────────── layout ────────── */
    private static final float MARGIN = 50f;
    private static final float TITLE_GAP = 36f;
    private static final float SUPPLIER_BLOCK_GAP = 60f;
    private static final float META_BLOCK_GAP = 80f;
    private static final float TOTALS_BLOCK_GAP = 20f;
    private static final float TABLE_ROW_HEIGHT = 20f;
    private static final float TABLE_CELL_PADDING = 2f;
    private static final float PAGE_TOP_MARGIN = 60f;
    private static final float RIGHT_COLUMN_OFFSET = 10f;
    private static final float HORIZONTAL_RULE_OFFSET = 4f;
    private static final float AFTER_TABLE_GAP = 26f;

    /* ────────── font sizes & line spacing ────────── */
    private static final int TITLE_FONT_SIZE = 16;
    private static final int DEFAULT_FONT_SIZE = 11;
    private static final int LEGAL_FONT_SIZE = 10;
    private static final float DEFAULT_LINE_SPACING = 14f;
    private static final float TOTALS_LINE_SPACING = 16f;
    private static final float LEGAL_NOTICE_FIRST_LINE_GAP = 24f;
    private static final float LEGAL_NOTICE_LINE_GAP = 14f;

    /* ────────── QR / footer ────────── */
    private static final int QR_SIZE = 160;
    private static final float QR_Y = 120f;
    private static final int QR_HINT_MARGIN = 0;
    private static final float FOOTER_LINE1_GAP = 18f;
    private static final float FOOTER_LINE2_GAP = 32f;

    private static final float[] TABLE_COL_WIDTHS = {140f, 60f, 70f, 100f, 100f};

    public byte[] generatePdf(
            List<String> supplierAddressLines,
            BrojRacunaType brojRacuna,
            LocalDateTime datumIVrijeme,
            NacinPlacanjaType nacinPlacanja,
            List<InvoiceItem> items,
            String iznosUkupno,
            String legalNotice,
            String jir,
            String zki,
            String operatorCode
    ) throws IOException, WriterException {

        try (var doc = new PDDocument()) {
            var page = new PDPage(A4);
            doc.addPage(page);

            var font = PDType0Font.load(doc, regularFontFile.getInputStream(), true);
            var fontBold = PDType0Font.load(doc, boldFontFile.getInputStream(), true);

            var pageWidth = page.getMediaBox().getWidth();
            var tableWidth = pageWidth - 2 * MARGIN;
            var leftX = MARGIN;
            var rightX = pageWidth / 2f + RIGHT_COLUMN_OFFSET;
            var y = page.getMediaBox().getHeight() - PAGE_TOP_MARGIN;

            try (var cs = new PDPageContentStream(doc, page, APPEND, true, true)) {
                y = addTitle(cs, page, fontBold, y);
                y = addSupplierBlock(cs, font, supplierAddressLines, rightX, y);
                y = addMetaBlock(cs, font, brojRacuna, datumIVrijeme, nacinPlacanja, leftX, y);
                y = addItemsTable(cs, font, fontBold, items, leftX, y, tableWidth);
                y = addTotals(cs, fontBold, iznosUkupno, leftX + tableWidth, y);
                addLegalNotice(cs, font, legalNotice, jir, zki, operatorCode, leftX, y);

                var qrData = buildQrData(jir, zki, datumIVrijeme, iznosUkupno);
                addQrCodeAndFooter(cs, doc, page, font, qrData);
            }

            try (var baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                return baos.toByteArray();
            }
        }
    }

    private static String buildQrData(String jir,
                                      String zki,
                                      LocalDateTime dt,
                                      String totalAmount) {

        var idParam = (jir != null && !jir.isBlank())
                ? JIR_PARAM + jir
                : ZKI_PARAM + zki;

        var datePart = DATE_PARAM + QR_DATETIME_FMT.format(dt);
        var amountPart = AMOUNT_PARAM + formatAmountForQr(totalAmount);

        return QR_URL_BASE + idParam + "&" + datePart + "&" + amountPart;
    }

    private static String formatAmountForQr(String amountTxt) {
        return amountTxt.replace(".", "");
    }

    private static float addTitle(PDPageContentStream cs, PDPage page, PDFont font, float y) throws IOException {
        center(cs, page, font, TITLE_FONT_SIZE, RACUN_TITLE, y);
        return y - TITLE_GAP;
    }

    private static float addSupplierBlock(PDPageContentStream cs,
                                          PDFont font,
                                          List<String> lines,
                                          float x,
                                          float y) throws IOException {
        cs.beginText();
        cs.setFont(font, DEFAULT_FONT_SIZE);
        cs.newLineAtOffset(x, y);
        for (var i = 0; i < lines.size(); i++) {
            if (i > 0) cs.newLineAtOffset(0, -DEFAULT_LINE_SPACING);
            cs.showText(lines.get(i));
        }
        cs.endText();
        return y - SUPPLIER_BLOCK_GAP;
    }

    private static float addMetaBlock(PDPageContentStream cs,
                                      PDFont font,
                                      BrojRacunaType br,
                                      LocalDateTime dt,
                                      NacinPlacanjaType np,
                                      float x,
                                      float y) throws IOException {

        var lines = List.of(
                BROJ_FAKTURE_LABEL + generateBrojFakture(br),
                DATUM_IZDAVANJA_LABEL + DATE_FMT.format(dt),
                VRIJEME_IZDAVANJA_LABEL + TIME_FMT.format(dt),
                NACIN_PLACANJA_LABEL + generateNacinPlacanja(np)
        );

        cs.beginText();
        cs.setFont(font, DEFAULT_FONT_SIZE);
        cs.newLineAtOffset(x, y);
        for (var i = 0; i < lines.size(); i++) {
            if (i > 0) cs.newLineAtOffset(0, -DEFAULT_LINE_SPACING);
            cs.showText(lines.get(i));
        }
        cs.endText();
        return y - META_BLOCK_GAP;
    }

    private static String generateBrojFakture(BrojRacunaType br) {
        return br.getBrOznRac() + "/" + br.getOznPosPr() + "/" + br.getOznNapUr();
    }

    private static String generateNacinPlacanja(NacinPlacanjaType np) {
        return switch (np) {
            case G -> "Gotovina";
            case K -> "Kartica";
            case C -> "Ček";
            case T -> "Transakcijski račun";
            case O -> "Ostalo";
        };
    }

    private static float addItemsTable(PDPageContentStream cs,
                                       PDFont font,
                                       PDFont bold,
                                       List<InvoiceItem> items,
                                       float x,
                                       float y,
                                       float tableW) throws IOException {

        drawTableRow(cs, bold, x, y,
                new String[]{HEADER_OPIS, HEADER_KOLICINA, HEADER_POREZ, HEADER_IZNOS_POREZA, HEADER_NETO_IZNOS});

        var cursorY = y - TABLE_ROW_HEIGHT;
        for (var it : items) {
            drawTableRow(cs, font, x, cursorY, new String[]{
                    it.description(),
                    it.quantity().toString(),
                    "-",
                    "-",
                    money(it.netAmount())
            });
            cursorY -= TABLE_ROW_HEIGHT;
        }

        cs.moveTo(x, cursorY + HORIZONTAL_RULE_OFFSET);
        cs.lineTo(x + tableW, cursorY + HORIZONTAL_RULE_OFFSET);
        cs.stroke();

        return cursorY - AFTER_TABLE_GAP;
    }

    private static float addTotals(PDPageContentStream cs,
                                   PDFont bold,
                                   String amount,
                                   float rightX,
                                   float y) throws IOException {

        for (var line : List.of(
                UKUPAN_NETO_IZNOS_LABEL + money(amount),
                UKUPAN_PLACANJE_IZNOS_LABEL + money(amount)
        )) {
            drawRightAlignedText(cs, bold, DEFAULT_FONT_SIZE, rightX, y, line);
            y -= TOTALS_LINE_SPACING;
        }

        return y - TOTALS_BLOCK_GAP;
    }

    private static float addLegalNotice(PDPageContentStream cs,
                                        PDFont font,
                                        String notice,
                                        String jir,
                                        String zki,
                                        String operator,
                                        float x,
                                        float y) throws IOException {

        cs.beginText();
        cs.setFont(font, LEGAL_FONT_SIZE);
        cs.newLineAtOffset(x, y);
        cs.showText(notice);
        cs.newLineAtOffset(0, -LEGAL_NOTICE_FIRST_LINE_GAP);
        cs.showText(JIR_LABEL + jir);
        cs.newLineAtOffset(0, -LEGAL_NOTICE_LINE_GAP);
        cs.showText(ZKI_LABEL + zki);
        cs.newLineAtOffset(0, -LEGAL_NOTICE_LINE_GAP);
        cs.showText(RACUN_IZDAO_LABEL + operator);
        cs.endText();

        return y;
    }

    private static void addQrCodeAndFooter(PDPageContentStream cs,
                                           PDDocument doc,
                                           PDPage page,
                                           PDFont font,
                                           String qrData) throws IOException, WriterException {

        var qrImg = generateQrImage(qrData);
        var img = createFromByteArray(doc, bufferedImageToBytes(qrImg), QR_IMG_NAME);
        var qrX = (page.getMediaBox().getWidth() - QR_SIZE) / 2f;

        cs.drawImage(img, qrX, QR_Y, QR_SIZE, QR_SIZE);

        center(cs, page, font, LEGAL_FONT_SIZE,
                FOOTER_LINE1,
                QR_Y - FOOTER_LINE1_GAP);

        center(cs, page, font, LEGAL_FONT_SIZE,
                FOOTER_LINE2,
                QR_Y - FOOTER_LINE2_GAP);
    }

    /* ────────── low-level drawing ────────── */

    private static void drawTableRow(PDPageContentStream cs,
                                     PDFont font,
                                     float x,
                                     float topY,
                                     String[] texts) throws IOException {

        var textY = topY - (TABLE_ROW_HEIGHT - DEFAULT_FONT_SIZE) / 2f;
        var cursorX = x;

        for (var i = 0; i < texts.length; i++) {
            var cellW = TABLE_COL_WIDTHS[i];
            var txt = texts[i];

            cs.beginText();
            cs.setFont(font, DEFAULT_FONT_SIZE);
            cs.newLineAtOffset(cursorX + TABLE_CELL_PADDING, textY);
            cs.showText(txt);
            cs.endText();

            cursorX += cellW;
        }
    }

    private static void drawRightAlignedText(PDPageContentStream cs,
                                             PDFont f,
                                             int size,
                                             float rightX,
                                             float y,
                                             String txt) throws IOException {
        var textW = f.getStringWidth(txt) / 1000 * size;
        cs.beginText();
        cs.setFont(f, size);
        cs.newLineAtOffset(rightX - textW, y);
        cs.showText(txt);
        cs.endText();
    }

    private static void center(PDPageContentStream cs,
                               PDPage page,
                               PDFont font,
                               float size,
                               String txt,
                               float y) throws IOException {
        var textW = font.getStringWidth(txt) / 1000 * size;
        var x = (page.getMediaBox().getWidth() - textW) / 2f;
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(txt);
        cs.endText();
    }

    // QR generation

    private static BufferedImage generateQrImage(String data) throws WriterException {
        var writer = new QRCodeWriter();
        var hints = Map.of(
                EncodeHintType.MARGIN, QR_HINT_MARGIN,
                ERROR_CORRECTION, Q
        );
        var matrix = writer.encode(data, QR_CODE, QR_SIZE, QR_SIZE, hints);
        return toBufferedImage(matrix);
    }

    private static byte[] bufferedImageToBytes(BufferedImage img) throws IOException {
        try (var baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, PNG_FORMAT, baos);
            return baos.toByteArray();
        }
    }

    private static String money(String value) {
        return value + EUR_SUFFIX;
    }

    @Builder(toBuilder = true)
    public record InvoiceItem(String description, Integer quantity, String netAmount) {
    }
}