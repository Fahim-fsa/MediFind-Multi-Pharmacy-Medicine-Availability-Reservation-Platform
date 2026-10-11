package com.medifind.util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * SLP: Admin Reporting & Configuration → "Export reports as CSV/PDF"
 */
public final class PdfReportBuilder {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private PdfReportBuilder() {
    }

    public static byte[] buildTableReport(String title, List<String> columnHeaders, List<List<String>> rows) {
        Document document = new Document(PageSize.A4, 40, 40, 60, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(14, 124, 102));
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            Paragraph heading = new Paragraph(title, titleFont);
            heading.setSpacingAfter(4f);
            document.add(heading);

            Paragraph generatedAt = new Paragraph(
                    "MediFind — generated " + LocalDateTime.now().format(TIMESTAMP_FORMAT), metaFont);
            generatedAt.setSpacingAfter(16f);
            document.add(generatedAt);

            PdfPTable table = new PdfPTable(columnHeaders.size());
            table.setWidthPercentage(100);

            for (String header : columnHeaders) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(22, 50, 74));
                cell.setPadding(6f);
                table.addCell(cell);
            }

            boolean shaded = false;
            for (List<String> row : rows) {
                Color bg = shaded ? new Color(244, 246, 245) : Color.WHITE;
                for (String value : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, bodyFont));
                    cell.setBackgroundColor(bg);
                    cell.setPadding(5f);
                    table.addCell(cell);
                }
                shaded = !shaded;
            }

            if (rows.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("No data for this report yet.", bodyFont));
                empty.setColspan(columnHeaders.size());
                empty.setPadding(8f);
                table.addCell(empty);
            }

            document.add(table);
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate PDF report", e);
        } finally {
            document.close();
        }

        try {
            out.close();
        } catch (IOException ignored) {
            // ByteArrayOutputStream#close() is a no-op; nothing to handle.
        }

        return out.toByteArray();
    }
}
