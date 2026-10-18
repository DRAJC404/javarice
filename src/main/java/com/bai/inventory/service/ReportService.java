package com.bai.inventory.service;

import com.bai.inventory.dao.ReportDao;
import com.bai.inventory.model.Product;
import com.bai.inventory.model.Role;
import com.bai.inventory.model.StockMovement;
import com.bai.inventory.model.User;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private final ReportDao reportDao;

    public ReportService() {
        this.reportDao = new ReportDao();
    }

    public ReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public List<StockMovement> getMovements(User user, LocalDateTime from, LocalDateTime to) {
        int orgId = (user != null && user.getOrganizationId() > 0) ? user.getOrganizationId() : 1;
        return reportDao.getStockMovementsReport(orgId, from, to);
    }

    public List<Product> getInventory(User user) {
        int orgId = (user != null && user.getOrganizationId() > 0) ? user.getOrganizationId() : 1;
        List<Product> list = reportDao.getInventoryReport(orgId);
        if (user != null && user.getRole() == Role.EMPLOYEE) {
            for (Product p : list) p.setCostPrice(BigDecimal.ZERO);
        }
        return list;
    }

    public List<Product> getProducts(User user) {
        int orgId = (user != null && user.getOrganizationId() > 0) ? user.getOrganizationId() : 1;
        List<Product> list = reportDao.getProductCatalogReport(orgId);
        if (user != null && user.getRole() == Role.EMPLOYEE) {
            for (Product p : list) p.setCostPrice(BigDecimal.ZERO);
        }
        return list;
    }

    public List<Product> getLowStock(User user) {
        int orgId = (user != null && user.getOrganizationId() > 0) ? user.getOrganizationId() : 1;
        List<Product> list = reportDao.getLowStockReport(orgId);
        if (user != null && user.getRole() == Role.EMPLOYEE) {
            for (Product p : list) p.setCostPrice(BigDecimal.ZERO);
        }
        return list;
    }

    public void exportToCsv(File file, List<String> headers, List<List<String>> rows) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            // Write CSV header
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < headers.size(); i++) {
                sb.append(escapeCsv(headers.get(i)));
                if (i < headers.size() - 1) sb.append(",");
            }
            sb.append("\n");
            writer.write(sb.toString());

            // Write CSV rows
            for (List<String> row : rows) {
                sb.setLength(0);
                for (int i = 0; i < row.size(); i++) {
                    sb.append(escapeCsv(row.get(i)));
                    if (i < row.size() - 1) sb.append(",");
                }
                sb.append("\n");
                writer.write(sb.toString());
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "\"\"";
        String s = value.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\n") || s.contains("\r") || s.contains("\"")) {
            return "\"" + s + "\"";
        }
        return "\"" + s + "\"";
    }

    public void exportToPdf(File file, String title, String subtitle, List<String> headers, List<List<String>> rows) throws DocumentException, IOException {
        // Landscape A4 for wide table presentation
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        // Fonts
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(15, 23, 42));
        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139));
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(30, 41, 59));
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(148, 163, 184));

        // Header block
        Paragraph titlePara = new Paragraph("bAI Inventory System — " + title, titleFont);
        titlePara.setSpacingAfter(4);
        document.add(titlePara);

        String genDate = LocalDateTime.now().format(DISPLAY_TIME);
        Paragraph subPara = new Paragraph((subtitle != null ? subtitle + " • " : "") + "Generated on " + genDate, subFont);
        subPara.setSpacingAfter(16);
        document.add(subPara);

        // Table
        PdfPTable table = new PdfPTable(headers.size());
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        Color brandColor = new Color(37, 99, 235);
        Color zebraColor = new Color(248, 250, 252);
        Color borderColor = new Color(226, 232, 240);

        // Add headers
        for (String headerText : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(headerText, headerFont));
            cell.setBackgroundColor(brandColor);
            cell.setPadding(6);
            cell.setBorderColor(brandColor);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }

        // Add data rows
        int rowIndex = 0;
        for (List<String> row : rows) {
            Color rowBg = (rowIndex % 2 == 1) ? zebraColor : Color.WHITE;
            for (String val : row) {
                PdfPCell cell = new PdfPCell(new Phrase(val != null ? val : "", cellFont));
                cell.setBackgroundColor(rowBg);
                cell.setPadding(5);
                cell.setBorderColor(borderColor);
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(cell);
            }
            rowIndex++;
        }

        document.add(table);

        // Summary footer
        Paragraph footerPara = new Paragraph("Total Records: " + rows.size() + " | Confidential • Internal Business Use Only", footerFont);
        footerPara.setAlignment(Element.ALIGN_RIGHT);
        footerPara.setSpacingBefore(8);
        document.add(footerPara);

        document.close();
    }
}
