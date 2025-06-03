package hr.leadtheway.fiskalizacija;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import hr.leadtheway.wsdl.BrojRacunaType;
import hr.leadtheway.wsdl.NacinPlacanjaType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.google.zxing.BarcodeFormat.QR_CODE;
import static com.google.zxing.EncodeHintType.ERROR_CORRECTION;
import static com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage;
import static com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L;
import static org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode.APPEND;
import static org.apache.pdfbox.pdmodel.common.PDRectangle.A4;
import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA;
import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD;
import static org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromByteArray;

public final class PdfReceiptGenerator {

    private PdfReceiptGenerator() {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final float MARGIN = 50f;
    private static final float TITLE_GAP = 35f;
    private static final float BLOCK_GAP = 60f;
    private static final float META_BLOCK_GAP = 80f;
    private static final float TOTALS_BLOCK_GAP = 20f;
    private static final float ROW_HEIGHT = 18f;
    private static final float CELL_PADDING = 2f;
    private static final int QR_SIZE = 110;
    private static final float FOOTER_LINE1_GAP = 18f;
    private static final float FOOTER_LINE2_GAP = 32f;

    private static final float[] TABLE_COL_WIDTHS = {140f, 60f, 70f, 100f, 100f};

    public static void generatePdf(
            OutputStream out,
            List<String> supplierAddressLines,
            BrojRacunaType brojRacuna,
            LocalDateTime datumIVrijeme,
            NacinPlacanjaType nacinPlacanja,
            List<InvoiceItem> items,
            String iznosUkupno,
            String legalNotice,
            String jir,
            String zki,
            String operatorCode,
            String qrData
    ) throws IOException, WriterException {

        try (var doc = new PDDocument()) {
            var page = new PDPage(A4);
            doc.addPage(page);

            var font = new PDType1Font(HELVETICA);
            var fontBold = new PDType1Font(HELVETICA_BOLD);

            var pageWidth = page.getMediaBox().getWidth();
            var tableWidth = pageWidth - 2 * MARGIN;
            var leftX = MARGIN;
            var rightX = pageWidth / 2f + 10f;
            var y = page.getMediaBox().getHeight() - 60f;

            try (var cs = new PDPageContentStream(doc, page, APPEND, true, true)) {
                y = addTitle(cs, page, fontBold, y);
                y = addSupplierBlock(cs, font, supplierAddressLines, rightX, y);
                y = addMetaBlock(cs, font, brojRacuna, datumIVrijeme, nacinPlacanja, leftX, y);
                y = addItemsTable(cs, font, fontBold, items, leftX, y, tableWidth);
                y = addTotals(cs, fontBold, iznosUkupno, leftX + tableWidth, y);
                addLegalNotice(cs, font, legalNotice, jir, zki, operatorCode, leftX, y);
                addQrCodeAndFooter(cs, doc, page, font, qrData);
            }
            doc.save(out);
        }
    }

    private static float addTitle(PDPageContentStream cs, PDPage page, PDFont font, float y) throws IOException {
        center(cs, page, font, 16, "Racun", y);
        return y - TITLE_GAP;
    }

    private static float addSupplierBlock(PDPageContentStream cs,
                                          PDFont font,
                                          List<String> lines,
                                          float x,
                                          float y) throws IOException {
        cs.beginText();
        cs.setFont(font, 11);
        cs.newLineAtOffset(x, y);
        for (var i = 0; i < lines.size(); i++) {
            if (i > 0) cs.newLineAtOffset(0, -14);
            cs.showText(lines.get(i));
        }
        cs.endText();
        return y - BLOCK_GAP;
    }

    private static float addMetaBlock(PDPageContentStream cs,
                                      PDFont font,
                                      BrojRacunaType br,
                                      LocalDateTime dt,
                                      NacinPlacanjaType np,
                                      float x,
                                      float y) throws IOException {

        var lines = List.of(
                "Broj fakture:  " + br,
                "Datum izdavanja racuna:  " + DATE_FMT.format(dt),
                "Vrijeme izdavanja racuna:  " + TIME_FMT.format(dt),
                "Nacin placanja:  " + np
        );

        cs.beginText();
        cs.setFont(font, 11);
        cs.newLineAtOffset(x, y);
        for (var i = 0; i < lines.size(); i++) {
            if (i > 0) cs.newLineAtOffset(0, -14);
            cs.showText(lines.get(i));
        }
        cs.endText();
        return y - META_BLOCK_GAP;
    }

    private static float addItemsTable(PDPageContentStream cs,
                                       PDFont font,
                                       PDFont bold,
                                       List<InvoiceItem> items,
                                       float x,
                                       float y,
                                       float tableW) throws IOException {

        drawTableRow(cs, bold, x, y,
                new String[]{"Opis", "Kol.", "Porez", "Porez Iznos", "Neto iznos"});

        var cursorY = y - ROW_HEIGHT;
        for (var it : items) {
            drawTableRow(cs, font, x, cursorY, new String[]{
                    it.description(),
                    it.quantity().toString(),
                    "-",
                    "-",
                    money(it.netAmount())
            });
            cursorY -= ROW_HEIGHT;
        }

        cs.moveTo(x, cursorY + 4);
        cs.lineTo(x + tableW, cursorY + 4);
        cs.stroke();

        return cursorY - 25f;
    }

    private static float addTotals(PDPageContentStream cs,
                                   PDFont bold,
                                   String amount,
                                   float rightX,
                                   float y) throws IOException {

        for (var line : List.of(
                "Ukupan neto iznos    " + money(amount),
                "Ukupan iznos za placanje    " + money(amount))) {

            drawRightAlignedText(cs, bold, 11, rightX, y, line);
            y -= 16;
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
        cs.setFont(font, 10);
        cs.newLineAtOffset(x, y);
        cs.showText(notice);
        cs.newLineAtOffset(0, -16);
        cs.showText("JIR: " + jir);
        cs.newLineAtOffset(0, -14);
        cs.showText("ZKI: " + zki);
        cs.newLineAtOffset(0, -14);
        cs.showText("Racun izdao: " + operator);
        cs.endText();

        return y;
    }

    private static void addQrCodeAndFooter(PDPageContentStream cs,
                                           PDDocument doc,
                                           PDPage page,
                                           PDFont font,
                                           String qrData) throws IOException, WriterException {

        var qrImg = generateQrImage(qrData);
        var img = createFromByteArray(doc, bufferedImageToBytes(qrImg), "qr");
        var qrX = (page.getMediaBox().getWidth() - QR_SIZE) / 2f;
        var qrY = 120f;

        cs.drawImage(img, qrX, qrY, QR_SIZE, QR_SIZE);

        center(cs, page, font, 10,
                "Izdao/la u ime dobavljaca ..., obrt za usluge, vl. ...",
                qrY - FOOTER_LINE1_GAP);

        center(cs, page, font, 10,
                "Second footer line.",
                qrY - FOOTER_LINE2_GAP);
    }

    /* ────────── low-level drawing ────────── */

    private static void drawTableRow(PDPageContentStream cs,
                                     PDFont font,
                                     float x,
                                     float topY,
                                     String[] texts) throws IOException {

        var textY = topY - (ROW_HEIGHT - 11) / 2f;
        var cursorX = x;

        for (var i = 0; i < texts.length; i++) {
            var cellW = TABLE_COL_WIDTHS[i];
            var txt = texts[i];

            cs.beginText();
            cs.setFont(font, 11);
            cs.newLineAtOffset(cursorX + CELL_PADDING, textY);
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

    // QR generatoion

    private static BufferedImage generateQrImage(String data) throws WriterException {
        var writer = new QRCodeWriter();
        var hints = Map.of(
                EncodeHintType.MARGIN, 1,
                ERROR_CORRECTION, L
        );
        var matrix = writer.encode(data, QR_CODE, QR_SIZE, QR_SIZE, hints);
        return toBufferedImage(matrix);
    }

    private static byte[] bufferedImageToBytes(BufferedImage img) throws IOException {
        try (var baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        }
    }

    private static String money(String value) {
        return value + " EUR";
    }

    public record InvoiceItem(String description, Integer quantity, String netAmount) {
    }
}