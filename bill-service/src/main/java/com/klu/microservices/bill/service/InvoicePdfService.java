package com.klu.microservices.bill.service;

import com.klu.microservices.bill.dto.InvoiceResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final int MAX_PAGE1_FINDING_LINES = 3;

    public byte[] generatePdf(InvoiceResponse inv) {
        try {
            return buildPdfDocument(inv);
        } catch (Exception e) {
            return buildFallbackPdf(inv);
        }
    }

    private byte[] buildPdfDocument(InvoiceResponse inv) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 1. PDF Header
        write(out, "%PDF-1.4\r\n%\u00e2\u00e3\u00cf\u00d3\r\n");

        // Determine if page 2 is needed
        String findings = inv.getRemarks() != null && !inv.getRemarks().isBlank() ? inv.getRemarks() : "";
        List<String> findingLines = wrapText(findings, 85);
        boolean needsPage2 = findingLines.size() > MAX_PAGE1_FINDING_LINES;
        int pageCount = needsPage2 ? 2 : 1;

        // Generate content for page 1
        List<byte[]> pageStreams = new ArrayList<>();
        ByteArrayOutputStream p1Stream = new ByteArrayOutputStream();
        buildPageContent(p1Stream, inv, 1, pageCount, findingLines);
        pageStreams.add(p1Stream.toByteArray());

        if (needsPage2) {
            ByteArrayOutputStream p2Stream = new ByteArrayOutputStream();
            buildPageContent(p2Stream, inv, 2, pageCount, findingLines);
            pageStreams.add(p2Stream.toByteArray());
        }

        // Object numbering scheme:
        // Obj 1: Catalog
        // Obj 2: Pages (Parent)
        // Obj 3: Font F1 (Helvetica)
        // Obj 4: Font F2 (Helvetica-Bold)
        // For each page i (0-based):
        //   Page object id = 5 + (i * 2)
        //   Content stream id = 5 + (i * 2) + 1
        int totalObjects = 4 + (pageCount * 2);
        long[] offsets = new long[totalObjects + 1];

        // Object 1: Catalog
        offsets[1] = out.size();
        write(out, "1 0 obj\r\n<< /Type /Catalog /Pages 2 0 R >>\r\nendobj\r\n");

        // Object 2: Pages root
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            int pageObjId = 5 + (i * 2);
            kids.append(pageObjId).append(" 0 R ");
        }
        offsets[2] = out.size();
        write(out, "2 0 obj\r\n<< /Type /Pages /Kids [" + kids.toString().trim() + "] /Count " + pageCount + " >>\r\nendobj\r\n");

        // Object 3: Font Helvetica
        offsets[3] = out.size();
        write(out, "3 0 obj\r\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\r\nendobj\r\n");

        // Object 4: Font Helvetica-Bold
        offsets[4] = out.size();
        write(out, "4 0 obj\r\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\r\nendobj\r\n");

        // Write Pages and Content Streams
        for (int i = 0; i < pageCount; i++) {
            int pageObjId = 5 + (i * 2);
            int streamObjId = pageObjId + 1;
            byte[] streamData = pageStreams.get(i);

            // Page Object
            offsets[pageObjId] = out.size();
            write(out, pageObjId + " 0 obj\r\n"
                    + "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792]\r\n"
                    + "   /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >>\r\n"
                    + "   /Contents " + streamObjId + " 0 R\r\n"
                    + ">>\r\nendobj\r\n");

            // Content Stream Object
            offsets[streamObjId] = out.size();
            write(out, streamObjId + " 0 obj\r\n"
                    + "<< /Length " + streamData.length + " >>\r\n"
                    + "stream\r\n");
            out.write(streamData);
            write(out, "\r\nendstream\r\nendobj\r\n");
        }

        // Cross-reference (Xref) Table
        long xrefOffset = out.size();
        write(out, "xref\r\n0 " + (totalObjects + 1) + "\r\n");
        write(out, "0000000000 65535 f \r\n");
        for (int i = 1; i <= totalObjects; i++) {
            write(out, String.format(Locale.US, "%010d 00000 n \r\n", offsets[i]));
        }

        // Trailer
        write(out, "trailer\r\n"
                + "<< /Size " + (totalObjects + 1) + "\r\n"
                + "   /Root 1 0 R\r\n"
                + ">>\r\n"
                + "startxref\r\n"
                + xrefOffset + "\r\n"
                + "%%EOF\r\n");

        return out.toByteArray();
    }

    private void buildPageContent(ByteArrayOutputStream cs, InvoiceResponse inv, int pageNumber, int totalPages, List<String> findingLines) throws IOException {
        // Enforce solid white background for the whole page (prevents dark mode/black inverted page)
        write(cs, "q\r\n");
        write(cs, "1 1 1 rg 0 0 612 792 re f\r\n");

        if (pageNumber == 1) {
            // Top Header Branding Banner (Precision Dark Slate with cyan border)
            write(cs, "0.06 0.09 0.16 rg 0 705 612 87 re f\r\n");
            write(cs, "0.22 0.74 0.97 RG 3.0 w 0 705 m 612 705 l S\r\n");

            // Brand Logo Box
            write(cs, "0.15 0.22 0.35 rg 40 722 36 36 re f\r\n");
            write(cs, "0.22 0.74 0.97 RG 1.5 w 40 722 36 36 re S\r\n");
            write(cs, "BT\r\n/F2 16 Tf 0.22 0.74 0.97 rg 48 734 Td (PA) Tj\r\nET\r\n");

            // Company Title & Subtitle
            write(cs, "BT\r\n/F2 18 Tf 1 1 1 rg 85 744 Td (PRECISIONAUTO CARE) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 9 Tf 0.7 0.8 0.9 rg 85 728 Td (Certified Automotive Engineering & Workshop Invoicing) Tj\r\nET\r\n");

            // Header Right - Invoice Title & Badge
            write(cs, "0.15 0.22 0.35 rg 430 740 142 22 re f\r\n");
            write(cs, "0.22 0.74 0.97 RG 1 w 430 740 142 22 re S\r\n");
            write(cs, "BT\r\n/F2 11 Tf 0.22 0.74 0.97 rg 452 746 Td (FINAL INVOICE) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F2 11 Tf 1 1 1 rg 430 724 Td (" + escape(inv.getInvoiceNumber()) + ") Tj\r\nET\r\n");

            // Metadata Bar (Invoice Date, Completed Date, Booking ID, Status)
            write(cs, "0.95 0.97 0.99 rg 40 645 532 50 re f\r\n");
            write(cs, "0.82 0.86 0.92 RG 1 w 40 645 532 50 re S\r\n");

            String genDate = inv.getGeneratedAt() != null ? inv.getGeneratedAt().format(DATE_FMT) : "N/A";
            String compDate = inv.getServiceCompletedAt() != null ? inv.getServiceCompletedAt().format(SHORT_DATE_FMT) : "N/A";

            write(cs, "BT\r\n/F2 8.5 Tf 0.3 0.35 0.45 rg 55 674 Td (INVOICE DATE:) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 8.5 Tf 0.1 0.1 0.1 rg 125 674 Td (" + escape(genDate) + ") Tj\r\nET\r\n");

            write(cs, "BT\r\n/F2 8.5 Tf 0.3 0.35 0.45 rg 55 656 Td (WORK ORDER:) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 8.5 Tf 0.1 0.1 0.1 rg 125 656 Td (#BK-" + inv.getBookingId() + "  |  Rec #" + inv.getServiceRecordId() + ") Tj\r\nET\r\n");

            write(cs, "BT\r\n/F2 8.5 Tf 0.3 0.35 0.45 rg 325 674 Td (SERVICE COMPLETED:) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 8.5 Tf 0.1 0.1 0.1 rg 435 674 Td (" + escape(compDate) + ") Tj\r\nET\r\n");

            write(cs, "BT\r\n/F2 8.5 Tf 0.3 0.35 0.45 rg 325 656 Td (PAYMENT STATUS:) Tj\r\nET\r\n");
            String status = inv.getPaymentStatus() != null ? inv.getPaymentStatus() : "ISSUED";
            write(cs, "BT\r\n/F2 8.5 Tf 0.05 0.5 0.25 rg 435 656 Td (" + escape(status) + ") Tj\r\nET\r\n");

            // Customer & Vehicle 2-Column Section
            // Left Box: Customer Info
            write(cs, "0.97 0.98 1.0 rg 40 535 256 95 re f\r\n");
            write(cs, "0.85 0.88 0.94 RG 1 w 40 535 256 95 re S\r\n");
            write(cs, "BT\r\n/F2 10 Tf 0.08 0.15 0.3 rg 55 612 Td (CUSTOMER INFORMATION) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F2 8.5 Tf 0.35 0.4 0.45 rg 55 592 Td (Customer Name:) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 9 Tf 0.1 0.1 0.1 rg 135 592 Td (" + escape(inv.getCustomerName()) + ") Tj\r\nET\r\n");
            write(cs, "BT\r\n/F2 8.5 Tf 0.35 0.4 0.45 rg 55 572 Td (Email Address:) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 8.5 Tf 0.1 0.1 0.1 rg 135 572 Td (" + escape(inv.getCustomerEmail()) + ") Tj\r\nET\r\n");
            write(cs, "BT\r\n/F2 8.5 Tf 0.35 0.4 0.45 rg 55 552 Td (Customer ID:) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 8.5 Tf 0.1 0.1 0.1 rg 135 552 Td (#CUST-" + (inv.getCustomerId() != null ? inv.getCustomerId() : "-") + ") Tj\r\nET\r\n");

            // Right Box: Vehicle & Technician Info
            write(cs, "0.97 0.98 1.0 rg 316 535 256 95 re f\r\n");
            write(cs, "0.85 0.88 0.94 RG 1 w 316 535 256 95 re S\r\n");
            write(cs, "BT\r\n/F2 10 Tf 0.08 0.15 0.3 rg 331 612 Td (VEHICLE & WORKSHOP) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F2 8.5 Tf 0.35 0.4 0.45 rg 331 592 Td (Registration:) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F2 9 Tf 0.1 0.4 0.7 rg 415 592 Td (" + escape(inv.getVehicleRegistrationNumber()) + ") Tj\r\nET\r\n");
            write(cs, "BT\r\n/F2 8.5 Tf 0.35 0.4 0.45 rg 331 572 Td (Make / Model:) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 9 Tf 0.1 0.1 0.1 rg 415 572 Td (" + escape(inv.getVehicleMake() + " " + inv.getVehicleModel()) + ") Tj\r\nET\r\n");
            write(cs, "BT\r\n/F2 8.5 Tf 0.35 0.4 0.45 rg 331 552 Td (Technician:) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 9 Tf 0.1 0.1 0.1 rg 415 552 Td (" + escape(inv.getTechnicianName()) + ") Tj\r\nET\r\n");

            // Service Performed & Findings Box
            write(cs, "0.96 0.97 0.99 rg 40 455 532 68 re f\r\n");
            write(cs, "0.85 0.88 0.94 RG 1 w 40 455 532 68 re S\r\n");
            write(cs, "BT\r\n/F2 9.5 Tf 0.08 0.15 0.3 rg 55 504 Td (Service Type: " + escape(inv.getServiceType()) + ") Tj\r\nET\r\n");
            write(cs, "BT\r\n/F2 8.5 Tf 0.3 0.35 0.45 rg 55 487 Td (Technician Findings / Remarks:) Tj\r\nET\r\n");

            int remarkY = 472;
            int linesToShow = Math.min(MAX_PAGE1_FINDING_LINES, findingLines.size());
            if (linesToShow == 0) {
                write(cs, "BT\r\n/F1 8.5 Tf 0.15 0.15 0.15 rg 55 " + remarkY + " Td (Scheduled service completed according to certified PrecisionAuto procedures.) Tj\r\nET\r\n");
            } else {
                for (int f = 0; f < linesToShow; f++) {
                    write(cs, "BT\r\n/F1 8.5 Tf 0.15 0.15 0.15 rg 55 " + remarkY + " Td (" + escape(findingLines.get(f)) + ") Tj\r\nET\r\n");
                    remarkY -= 13;
                }
            }

            // Itemized Charges Table
            // Table Header Row
            write(cs, "0.08 0.12 0.22 rg 40 405 532 26 re f\r\n");
            write(cs, "BT\r\n/F2 9 Tf 1 1 1 rg 55 414 Td (ITEM / SERVICE DESCRIPTION) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F2 9 Tf 1 1 1 rg 475 414 Td (AMOUNT (USD)) Tj\r\nET\r\n");

            int y = 380;
            drawTableRow(cs, y, "Certified Labor Charges (" + inv.getServiceType() + ")", "$" + String.format(Locale.US, "%.2f", inv.getLaborCharges()));
            y -= 26;
            drawTableRow(cs, y, "Workshop Spare Parts & Consumables", "$" + String.format(Locale.US, "%.2f", inv.getPartsCharges()));
            y -= 26;
            if (inv.getAdditionalCharges() != null && inv.getAdditionalCharges().doubleValue() > 0) {
                drawTableRow(cs, y, "Technician Additional Service & Diagnostics", "$" + String.format(Locale.US, "%.2f", inv.getAdditionalCharges()));
                y -= 26;
            }

            // Subtotal, Tax and Grand Total Box
            write(cs, "0.85 0.88 0.94 RG 1 w 300 " + (y + 16) + " m 572 " + (y + 16) + " l S\r\n");

            write(cs, "BT\r\n/F2 9.5 Tf 0.3 0.35 0.45 rg 320 " + (y) + " Td (Subtotal:) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 9.5 Tf 0.1 0.1 0.1 rg 480 " + (y) + " Td ($" + String.format(Locale.US, "%.2f", inv.getSubtotal()) + ") Tj\r\nET\r\n");
            y -= 18;

            write(cs, "BT\r\n/F2 9.5 Tf 0.3 0.35 0.45 rg 320 " + (y) + " Td (Tax (" + inv.getTaxPercentage() + "%):) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 9.5 Tf 0.1 0.1 0.1 rg 480 " + (y) + " Td ($" + String.format(Locale.US, "%.2f", inv.getTaxAmount()) + ") Tj\r\nET\r\n");
            y -= 28;

            // Grand Total Highlight Banner
            write(cs, "0.06 0.1 0.2 rg 300 " + y + " 272 30 re f\r\n");
            write(cs, "0.22 0.74 0.97 RG 1 w 300 " + y + " 272 30 re S\r\n");
            write(cs, "BT\r\n/F2 11 Tf 1 1 1 rg 315 " + (y + 10) + " Td (FINAL TOTAL:) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F2 13 Tf 0.22 0.74 0.97 rg 465 " + (y + 9) + " Td ($" + String.format(Locale.US, "%.2f", inv.getTotalAmount()) + ") Tj\r\nET\r\n");

            // Guarantee & Warranty Note
            write(cs, "0.96 0.98 1.0 rg 40 100 532 45 re f\r\n");
            write(cs, "0.85 0.9 0.96 RG 0.8 w 40 100 532 45 re S\r\n");
            write(cs, "BT\r\n/F2 8.5 Tf 0.08 0.15 0.3 rg 55 128 Td (PrecisionAuto Service Guarantee & Warranty) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 8 Tf 0.4 0.45 0.5 rg 55 113 Td (All service items and certified replacement components are covered by our 12-month / 12,000-mile precision guarantee.) Tj\r\nET\r\n");

            // Page Footer
            write(cs, "0.8 0.85 0.9 RG 0.5 w 40 70 m 572 70 l S\r\n");
            write(cs, "BT\r\n/F2 8 Tf 0.3 0.35 0.4 rg 40 54 Td (PrecisionAuto Care Certified Workshop) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 7.5 Tf 0.5 0.55 0.6 rg 40 42 Td (100 Workshop Boulevard, Tech District • support@precisionautocare.com • +1 (800) 555-AUTO) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 7.5 Tf 0.5 0.55 0.6 rg 510 42 Td (Page 1 of " + totalPages + ") Tj\r\nET\r\n");

            write(cs, "Q\r\n");
        } else {
            // Page 2 header and continuation
            write(cs, "0.06 0.09 0.16 rg 0 715 612 77 re f\r\n");
            write(cs, "0.22 0.74 0.97 RG 2.0 w 0 715 m 612 715 l S\r\n");
            write(cs, "BT\r\n/F2 16 Tf 1 1 1 rg 40 750 Td (PRECISIONAUTO CARE) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 9 Tf 0.7 0.8 0.9 rg 40 732 Td (Invoice Continuation - " + escape(inv.getInvoiceNumber()) + ") Tj\r\nET\r\n");
            write(cs, "BT\r\n/F2 10 Tf 0.22 0.74 0.97 rg 480 745 Td (PAGE 2 OF 2) Tj\r\nET\r\n");

            // Additional Technician Findings continuation box
            write(cs, "0.96 0.97 0.99 rg 40 480 532 205 re f\r\n");
            write(cs, "0.85 0.88 0.94 RG 1 w 40 480 532 205 re S\r\n");
            write(cs, "BT\r\n/F2 10 Tf 0.08 0.15 0.3 rg 55 660 Td (Extended Technician Findings & Workshop Diagnostic Notes:) Tj\r\nET\r\n");
            int p2Y = 640;
            for (int f = MAX_PAGE1_FINDING_LINES; f < Math.min(findingLines.size(), MAX_PAGE1_FINDING_LINES + 12); f++) {
                write(cs, "BT\r\n/F1 8.5 Tf 0.15 0.15 0.15 rg 55 " + p2Y + " Td (" + escape(findingLines.get(f)) + ") Tj\r\nET\r\n");
                p2Y -= 14;
            }

            // Quality & Authorization Sign-off box
            write(cs, "0.97 0.98 1.0 rg 40 330 532 120 re f\r\n");
            write(cs, "0.85 0.88 0.94 RG 1 w 40 330 532 120 re S\r\n");
            write(cs, "BT\r\n/F2 10 Tf 0.08 0.15 0.3 rg 55 425 Td (Workshop Quality & Compliance Sign-off) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 8.5 Tf 0.3 0.35 0.4 rg 55 405 Td (Work verified against certified manufacturer tolerances and diagnostic inspection criteria.) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F2 8.5 Tf 0.2 0.25 0.3 rg 55 375 Td (Lead Technician: " + escape(inv.getTechnicianName()) + "  |  Verified Status: COMPLETED) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 8.5 Tf 0.3 0.35 0.4 rg 55 355 Td (Digital verification signature recorded on blockchain-ready audit trail.) Tj\r\nET\r\n");

            // Page 2 Footer
            write(cs, "0.8 0.85 0.9 RG 0.5 w 40 70 m 572 70 l S\r\n");
            write(cs, "BT\r\n/F2 8 Tf 0.3 0.35 0.4 rg 40 54 Td (PrecisionAuto Care Certified Workshop) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 7.5 Tf 0.5 0.55 0.6 rg 40 42 Td (100 Workshop Boulevard, Tech District • support@precisionautocare.com • +1 (800) 555-AUTO) Tj\r\nET\r\n");
            write(cs, "BT\r\n/F1 7.5 Tf 0.5 0.55 0.6 rg 510 42 Td (Page 2 of 2) Tj\r\nET\r\n");
            write(cs, "Q\r\n");
        }
    }

    private void drawTableRow(ByteArrayOutputStream cs, int y, String desc, String amt) throws IOException {
        write(cs, "0.97 0.97 0.98 rg 40 " + (y - 5) + " 532 22 re f\r\n");
        write(cs, "0.88 0.9 0.94 RG 0.5 w 40 " + (y - 5) + " 532 22 re S\r\n");
        write(cs, "BT\r\n/F1 9 Tf 0.15 0.15 0.15 rg 55 " + y + " Td (" + escape(desc) + ") Tj\r\nET\r\n");
        write(cs, "BT\r\n/F2 9.5 Tf 0.08 0.15 0.3 rg 480 " + y + " Td (" + escape(amt) + ") Tj\r\nET\r\n");
    }

    private static List<String> wrapText(String text, int maxChars) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() + word.length() + 1 > maxChars) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                if (!current.isEmpty()) current.append(" ");
                current.append(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private byte[] buildFallbackPdf(InvoiceResponse inv) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            write(out, "%PDF-1.4\r\n");
            long o1 = out.size();
            write(out, "1 0 obj\r\n<< /Type /Catalog /Pages 2 0 R >>\r\nendobj\r\n");
            long o2 = out.size();
            write(out, "2 0 obj\r\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\r\nendobj\r\n");
            long o4 = out.size();
            write(out, "4 0 obj\r\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\r\nendobj\r\n");

            String streamText = "BT\r\n/F1 14 Tf 50 700 Td (PRECISIONAUTO CARE INVOICE) Tj\r\n"
                    + "0 -30 Td (Invoice: " + escape(inv.getInvoiceNumber()) + ") Tj\r\n"
                    + "0 -25 Td (Customer: " + escape(inv.getCustomerName()) + ") Tj\r\n"
                    + "0 -25 Td (Total Amount: $" + inv.getTotalAmount() + ") Tj\r\nET\r\n";
            byte[] streamBytes = streamText.getBytes(StandardCharsets.US_ASCII);

            long o5 = out.size();
            write(out, "5 0 obj\r\n<< /Length " + streamBytes.length + " >>\r\nstream\r\n");
            out.write(streamBytes);
            write(out, "\r\nendstream\r\nendobj\r\n");

            long o3 = out.size();
            write(out, "3 0 obj\r\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\r\nendobj\r\n");

            long xref = out.size();
            write(out, "xref\r\n0 6\r\n0000000000 65535 f \r\n");
            write(out, String.format(Locale.US, "%010d 00000 n \r\n", o1));
            write(out, String.format(Locale.US, "%010d 00000 n \r\n", o2));
            write(out, String.format(Locale.US, "%010d 00000 n \r\n", o3));
            write(out, String.format(Locale.US, "%010d 00000 n \r\n", o4));
            write(out, String.format(Locale.US, "%010d 00000 n \r\n", o5));

            write(out, "trailer\r\n<< /Size 6 /Root 1 0 R >>\r\nstartxref\r\n" + xref + "\r\n%%EOF\r\n");
            return out.toByteArray();
        } catch (Exception ignored) {
            return new byte[0];
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", " ");
    }

    private static void write(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.US_ASCII));
    }
}
