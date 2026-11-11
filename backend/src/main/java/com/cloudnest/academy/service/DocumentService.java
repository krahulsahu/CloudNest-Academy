package com.cloudnest.academy.service;

import com.cloudnest.academy.config.AppConfig;
import com.cloudnest.academy.domain.Certificate;
import com.cloudnest.academy.domain.PurchaseOrder;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Generates certificate and invoice PDFs with OpenPDF. */
@Service
public class DocumentService {
    private static final Color PURPLE = new Color(92, 29, 127);
    private static final Color GREY = new Color(90, 90, 100);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy").withZone(ZoneId.of("Asia/Kolkata"));
    private final AppConfig cfg;

    public DocumentService(AppConfig cfg) { this.cfg = cfg; }

    private static Paragraph centered(String text, Font font, float spacingBefore) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(spacingBefore);
        return p;
    }

    public byte[] certificatePdf(Certificate c) {
        try {
            return buildCertificate(c);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create certificate PDF", e);
        }
    }

    public byte[] invoicePdf(PurchaseOrder o) {
        try {
            return buildInvoice(o);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create invoice PDF", e);
        }
    }

    private byte[] buildCertificate(Certificate c) throws Exception {
        Rectangle page = PageSize.A4.rotate();
        Document doc = new Document(page, 60, 60, 60, 60);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        doc.open();
        PdfContentByte cb = writer.getDirectContentUnder();
        cb.setColorStroke(PURPLE);
        cb.setLineWidth(8f);
        cb.rectangle(22, 22, page.getWidth() - 44, page.getHeight() - 44);
        cb.stroke();
        cb.setColorStroke(new Color(56, 189, 248));
        cb.setLineWidth(2f);
        cb.rectangle(36, 36, page.getWidth() - 72, page.getHeight() - 72);
        cb.stroke();

        doc.add(centered(cfg.name.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PURPLE), 10));
        doc.add(centered("Certificate of Completion", FontFactory.getFont(FontFactory.TIMES_BOLD, 40, PURPLE), 26));
        doc.add(centered("This certifies that", FontFactory.getFont(FontFactory.HELVETICA, 14, GREY), 26));
        doc.add(centered(c.user.name, FontFactory.getFont(FontFactory.TIMES_BOLDITALIC, 36, Color.BLACK), 10));
        doc.add(centered("has successfully completed the course", FontFactory.getFont(FontFactory.HELVETICA, 14, GREY), 16));
        doc.add(centered(c.course.title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PURPLE), 10));
        String instructor = c.course.instructor != null ? "Instructor: " + c.course.instructor.name + "     " : "";
        doc.add(centered(instructor + "Completed on " + DATE.format(c.issuedAt), FontFactory.getFont(FontFactory.HELVETICA, 12, GREY), 30));
        doc.add(centered("Certificate ID: " + c.code, FontFactory.getFont(FontFactory.COURIER_BOLD, 12, Color.BLACK), 26));
        doc.add(centered("Verify at " + cfg.frontendUrl + "/verify/" + c.code, FontFactory.getFont(FontFactory.HELVETICA, 10, GREY), 4));
        doc.close();
        return out.toByteArray();
    }

    private byte[] buildInvoice(PurchaseOrder o) throws Exception {
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();
        Font h = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PURPLE);
        Font b = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font n = FontFactory.getFont(FontFactory.HELVETICA, 11);
        doc.add(new Paragraph(cfg.name, h));
        doc.add(new Paragraph("TAX INVOICE", b));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Invoice number: " + o.invoiceNumber, n));
        doc.add(new Paragraph("Date: " + (o.paidAt == null ? "-" : DATE.format(o.paidAt)), n));
        doc.add(new Paragraph("Billed to: " + o.user.name + " <" + o.user.email + ">", n));
        if (o.billingCountry != null) doc.add(new Paragraph("Country: " + o.billingCountry, n));
        doc.add(new Paragraph("Payment: " + o.provider + (o.providerPaymentId != null ? " (" + o.providerPaymentId + ")" : ""), n));
        doc.add(new Paragraph(" "));
        PdfPTable t = new PdfPTable(new float[]{4, 1.4f});
        t.setWidthPercentage(100);
        String cur = o.currency + " ";
        row(t, "Description", "Amount", b, true);
        row(t, o.itemName(), cur + o.listPrice, n, false);
        if (o.credit > 0) row(t, "Upgrade credit (courses already owned)", "- " + cur + o.credit, n, false);
        if (o.discount > 0) row(t, "Coupon " + o.couponCode, "- " + cur + o.discount, n, false);
        if (o.taxAmount > 0) row(t, "Includes GST @" + cfg.gstPercent + "%", cur + o.taxAmount, n, false);
        row(t, "Total paid", cur + o.total, b, true);
        doc.add(t);
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Status: " + o.status + ". Prices are inclusive of applicable taxes.", n));
        doc.add(new Paragraph("Thank you for learning with " + cfg.name + ".", n));
        doc.close();
        return out.toByteArray();
    }

    private static void row(PdfPTable t, String a, String b, Font f, boolean shade) {
        PdfPCell c1 = new PdfPCell(new Phrase(a, f));
        PdfPCell c2 = new PdfPCell(new Phrase(b, f));
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        for (PdfPCell c : new PdfPCell[]{c1, c2}) {
            c.setPadding(8);
            if (shade) c.setBackgroundColor(new Color(240, 236, 245));
            t.addCell(c);
        }
    }
}
