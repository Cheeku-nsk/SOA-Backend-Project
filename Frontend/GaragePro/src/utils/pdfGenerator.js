/**
 * PrecisionAuto Care - Certified Invoice PDF Generator
 * Generates 100% standard-compliant ISO 32000-1 (PDF 1.4) vector documents
 * with selectable text, clean printable white background, crisp typography,
 * and exact financial breakdown. Zero external dependencies.
 */

function escapePdf(str) {
  if (!str) return '';
  return String(str)
    .replace(/\\/g, '\\\\')
    .replace(/\(/g, '\\(')
    .replace(/\)/g, '\\)')
    .replace(/[^\x20-\x7E]/g, ' ');
}

function wrapText(text, maxChars) {
  if (!text) return [];
  const words = String(text).split(/\s+/);
  const lines = [];
  let current = '';
  for (const word of words) {
    if ((current + ' ' + word).trim().length > maxChars) {
      if (current) lines.push(current.trim());
      current = word;
    } else {
      current = (current + ' ' + word).trim();
    }
  }
  if (current) lines.push(current.trim());
  return lines;
}

function formatDate(dateStr) {
  if (!dateStr) return 'N/A';
  try {
    const d = new Date(dateStr);
    return isNaN(d.getTime()) ? String(dateStr) : d.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return String(dateStr);
  }
}

function formatShortDate(dateStr) {
  if (!dateStr) return 'N/A';
  try {
    const d = new Date(dateStr);
    return isNaN(d.getTime()) ? String(dateStr) : d.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  } catch {
    return String(dateStr);
  }
}

/**
 * Builds standard-compliant PDF 1.4 binary data
 */
export function createInvoicePdfBlob(inv) {
  const chunks = [];
  const append = (str) => chunks.push(new TextEncoder().encode(str));
  const appendRaw = (bytes) => chunks.push(bytes);

  // 1. PDF Header
  append('%PDF-1.4\r\n%\xE2\xE3\xCF\xD3\r\n');

  // Build Page 1 Content Stream
  const csParts = [];
  const cs = (str) => csParts.push(str);

  // Base white background
  cs('q\r\n');
  cs('1 1 1 rg 0 0 612 792 re f\r\n');

  // Top Header Branding Banner (Dark Slate with cyan line)
  cs('0.06 0.09 0.16 rg 0 705 612 87 re f\r\n');
  cs('0.22 0.74 0.97 RG 3.0 w 0 705 m 612 705 l S\r\n');

  // PA Brand Badge
  cs('0.15 0.22 0.35 rg 40 722 36 36 re f\r\n');
  cs('0.22 0.74 0.97 RG 1.5 w 40 722 36 36 re S\r\n');
  cs('BT\r\n/F2 16 Tf 0.22 0.74 0.97 rg 48 734 Td (PA) Tj\r\nET\r\n');

  // Header Title
  cs('BT\r\n/F2 18 Tf 1 1 1 rg 85 744 Td (PRECISIONAUTO CARE) Tj\r\nET\r\n');
  cs('BT\r\n/F1 9 Tf 0.7 0.8 0.9 rg 85 728 Td (Certified Automotive Engineering & Workshop Invoicing) Tj\r\nET\r\n');

  // Header Right - Invoice Number Badge
  cs('0.15 0.22 0.35 rg 430 740 142 22 re f\r\n');
  cs('0.22 0.74 0.97 RG 1 w 430 740 142 22 re S\r\n');
  cs('BT\r\n/F2 11 Tf 0.22 0.74 0.97 rg 452 746 Td (FINAL INVOICE) Tj\r\nET\r\n');
  cs('BT\r\n/F2 11 Tf 1 1 1 rg 430 724 Td (' + escapePdf(inv.invoiceNumber) + ') Tj\r\nET\r\n');

  // Metadata Bar
  cs('0.95 0.97 0.99 rg 40 645 532 50 re f\r\n');
  cs('0.82 0.86 0.92 RG 1 w 40 645 532 50 re S\r\n');

  const genDate = formatDate(inv.generatedAt);
  const compDate = formatShortDate(inv.serviceCompletedAt);

  cs('BT\r\n/F2 8.5 Tf 0.3 0.35 0.45 rg 55 674 Td (INVOICE DATE:) Tj\r\nET\r\n');
  cs('BT\r\n/F1 8.5 Tf 0.1 0.1 0.1 rg 125 674 Td (' + escapePdf(genDate) + ') Tj\r\nET\r\n');

  cs('BT\r\n/F2 8.5 Tf 0.3 0.35 0.45 rg 55 656 Td (WORK ORDER:) Tj\r\nET\r\n');
  cs('BT\r\n/F1 8.5 Tf 0.1 0.1 0.1 rg 125 656 Td (#BK-' + (inv.bookingId || '-') + '  |  Rec #' + (inv.serviceRecordId || '-') + ') Tj\r\nET\r\n');

  cs('BT\r\n/F2 8.5 Tf 0.3 0.35 0.45 rg 325 674 Td (SERVICE COMPLETED:) Tj\r\nET\r\n');
  cs('BT\r\n/F1 8.5 Tf 0.1 0.1 0.1 rg 435 674 Td (' + escapePdf(compDate) + ') Tj\r\nET\r\n');

  cs('BT\r\n/F2 8.5 Tf 0.3 0.35 0.45 rg 325 656 Td (PAYMENT STATUS:) Tj\r\nET\r\n');
  const status = inv.paymentStatus || 'ISSUED';
  cs('BT\r\n/F2 8.5 Tf 0.05 0.5 0.25 rg 435 656 Td (' + escapePdf(status) + ') Tj\r\nET\r\n');

  // Customer Information Box (Left)
  cs('0.97 0.98 1.0 rg 40 535 256 95 re f\r\n');
  cs('0.85 0.88 0.94 RG 1 w 40 535 256 95 re S\r\n');
  cs('BT\r\n/F2 10 Tf 0.08 0.15 0.3 rg 55 612 Td (CUSTOMER INFORMATION) Tj\r\nET\r\n');
  cs('BT\r\n/F2 8.5 Tf 0.35 0.4 0.45 rg 55 592 Td (Customer Name:) Tj\r\nET\r\n');
  cs('BT\r\n/F1 9 Tf 0.1 0.1 0.1 rg 135 592 Td (' + escapePdf(inv.customerName) + ') Tj\r\nET\r\n');
  cs('BT\r\n/F2 8.5 Tf 0.35 0.4 0.45 rg 55 572 Td (Email Address:) Tj\r\nET\r\n');
  cs('BT\r\n/F1 8.5 Tf 0.1 0.1 0.1 rg 135 572 Td (' + escapePdf(inv.customerEmail) + ') Tj\r\nET\r\n');
  cs('BT\r\n/F2 8.5 Tf 0.35 0.4 0.45 rg 55 552 Td (Customer ID:) Tj\r\nET\r\n');
  cs('BT\r\n/F1 8.5 Tf 0.1 0.1 0.1 rg 135 552 Td (#CUST-' + (inv.customerId || '-') + ') Tj\r\nET\r\n');

  // Vehicle Information Box (Right)
  cs('0.97 0.98 1.0 rg 316 535 256 95 re f\r\n');
  cs('0.85 0.88 0.94 RG 1 w 316 535 256 95 re S\r\n');
  cs('BT\r\n/F2 10 Tf 0.08 0.15 0.3 rg 331 612 Td (VEHICLE & WORKSHOP) Tj\r\nET\r\n');
  cs('BT\r\n/F2 8.5 Tf 0.35 0.4 0.45 rg 331 592 Td (Registration:) Tj\r\nET\r\n');
  cs('BT\r\n/F2 9 Tf 0.1 0.4 0.7 rg 415 592 Td (' + escapePdf(inv.vehicleRegistrationNumber) + ') Tj\r\nET\r\n');
  cs('BT\r\n/F2 8.5 Tf 0.35 0.4 0.45 rg 331 572 Td (Make / Model:) Tj\r\nET\r\n');
  cs('BT\r\n/F1 9 Tf 0.1 0.1 0.1 rg 415 572 Td (' + escapePdf((inv.vehicleMake || '') + ' ' + (inv.vehicleModel || '')) + ') Tj\r\nET\r\n');
  cs('BT\r\n/F2 8.5 Tf 0.35 0.4 0.45 rg 331 552 Td (Technician:) Tj\r\nET\r\n');
  cs('BT\r\n/F1 9 Tf 0.1 0.1 0.1 rg 415 552 Td (' + escapePdf(inv.technicianName || 'PrecisionAuto Service Team') + ') Tj\r\nET\r\n');

  // Service Type & Findings Box
  cs('0.96 0.97 0.99 rg 40 455 532 68 re f\r\n');
  cs('0.85 0.88 0.94 RG 1 w 40 455 532 68 re S\r\n');
  cs('BT\r\n/F2 9.5 Tf 0.08 0.15 0.3 rg 55 504 Td (Service Type: ' + escapePdf(inv.serviceType || 'General Maintenance') + ') Tj\r\nET\r\n');
  cs('BT\r\n/F2 8.5 Tf 0.3 0.35 0.45 rg 55 487 Td (Technician Findings / Remarks:) Tj\r\nET\r\n');

  const findings = inv.remarks || 'Scheduled maintenance completed per certified workshop checklist.';
  const remarkLines = wrapText(findings, 85);
  let remY = 472;
  for (let f = 0; f < Math.min(2, remarkLines.length); f++) {
    cs('BT\r\n/F1 8.5 Tf 0.15 0.15 0.15 rg 55 ' + remY + ' Td (' + escapePdf(remarkLines[f]) + ') Tj\r\nET\r\n');
    remY -= 13;
  }

  // Itemized Table Header
  cs('0.08 0.12 0.22 rg 40 405 532 26 re f\r\n');
  cs('BT\r\n/F2 9 Tf 1 1 1 rg 55 414 Td (ITEM / SERVICE DESCRIPTION) Tj\r\nET\r\n');
  cs('BT\r\n/F2 9 Tf 1 1 1 rg 475 414 Td (AMOUNT (USD)) Tj\r\nET\r\n');

  const drawRow = (y, desc, amt) => {
    cs('0.97 0.97 0.98 rg 40 ' + (y - 5) + ' 532 22 re f\r\n');
    cs('0.88 0.9 0.94 RG 0.5 w 40 ' + (y - 5) + ' 532 22 re S\r\n');
    cs('BT\r\n/F1 9 Tf 0.15 0.15 0.15 rg 55 ' + y + ' Td (' + escapePdf(desc) + ') Tj\r\nET\r\n');
    cs('BT\r\n/F2 9.5 Tf 0.08 0.15 0.3 rg 480 ' + y + ' Td (' + escapePdf(amt) + ') Tj\r\nET\r\n');
  };

  let tableY = 380;
  drawRow(tableY, `Certified Labor Charges (${inv.serviceType || 'General Maintenance'})`, `$${Number(inv.laborCharges || 0).toFixed(2)}`);
  tableY -= 26;
  drawRow(tableY, 'Workshop Spare Parts & Consumables', `$${Number(inv.partsCharges || 0).toFixed(2)}`);
  tableY -= 26;
  if (Number(inv.additionalCharges || 0) > 0) {
    drawRow(tableY, 'Technician Additional Service & Diagnostics', `$${Number(inv.additionalCharges).toFixed(2)}`);
    tableY -= 26;
  }

  // Subtotal & Tax Area
  cs('0.85 0.88 0.94 RG 1 w 300 ' + (tableY + 16) + ' m 572 ' + (tableY + 16) + ' l S\r\n');

  cs('BT\r\n/F2 9.5 Tf 0.3 0.35 0.45 rg 320 ' + tableY + ' Td (Subtotal:) Tj\r\nET\r\n');
  cs('BT\r\n/F1 9.5 Tf 0.1 0.1 0.1 rg 480 ' + tableY + ' Td ($' + Number(inv.subtotal || 0).toFixed(2) + ') Tj\r\nET\r\n');
  tableY -= 18;

  cs('BT\r\n/F2 9.5 Tf 0.3 0.35 0.45 rg 320 ' + tableY + ' Td (Tax (' + (inv.taxPercentage || 10) + '%):) Tj\r\nET\r\n');
  cs('BT\r\n/F1 9.5 Tf 0.1 0.1 0.1 rg 480 ' + tableY + ' Td ($' + Number(inv.taxAmount || 0).toFixed(2) + ') Tj\r\nET\r\n');
  tableY -= 28;

  // Grand Total Banner
  cs('0.06 0.1 0.2 rg 300 ' + tableY + ' 272 30 re f\r\n');
  cs('0.22 0.74 0.97 RG 1 w 300 ' + tableY + ' 272 30 re S\r\n');
  cs('BT\r\n/F2 11 Tf 1 1 1 rg 315 ' + (tableY + 10) + ' Td (FINAL TOTAL:) Tj\r\nET\r\n');
  cs('BT\r\n/F2 13 Tf 0.22 0.74 0.97 rg 465 ' + (tableY + 9) + ' Td ($' + Number(inv.totalAmount || 0).toFixed(2) + ') Tj\r\nET\r\n');

  // Guarantee Box
  cs('0.96 0.98 1.0 rg 40 100 532 45 re f\r\n');
  cs('0.85 0.9 0.96 RG 0.8 w 40 100 532 45 re S\r\n');
  cs('BT\r\n/F2 8.5 Tf 0.08 0.15 0.3 rg 55 128 Td (PrecisionAuto Service Guarantee & Warranty) Tj\r\nET\r\n');
  cs('BT\r\n/F1 8 Tf 0.4 0.45 0.5 rg 55 113 Td (All service items and certified replacement components are covered by our 12-month / 12,000-mile precision guarantee.) Tj\r\nET\r\n');

  // Page Footer
  cs('0.8 0.85 0.9 RG 0.5 w 40 70 m 572 70 l S\r\n');
  cs('BT\r\n/F2 8 Tf 0.3 0.35 0.4 rg 40 54 Td (PrecisionAuto Care Certified Workshop) Tj\r\nET\r\n');
  cs('BT\r\n/F1 7.5 Tf 0.5 0.55 0.6 rg 40 42 Td (100 Workshop Boulevard, Tech District • support@precisionautocare.com • +1 (800) 555-AUTO) Tj\r\nET\r\n');
  cs('BT\r\n/F1 7.5 Tf 0.5 0.55 0.6 rg 510 42 Td (Page 1 of 1) Tj\r\nET\r\n');
  cs('Q\r\n');

  const streamBytes = new TextEncoder().encode(csParts.join(''));

  // Object Calculation:
  // Obj 1: Catalog
  // Obj 2: Pages
  // Obj 3: Font Helvetica
  // Obj 4: Font Helvetica-Bold
  // Obj 5: Page 1
  // Obj 6: Content stream 1
  const totalObjects = 6;
  const offsets = new Array(totalObjects + 1);

  let currentPos = 0;
  for (const chunk of chunks) {
    currentPos += chunk.length;
  }

  // Object 1: Catalog
  offsets[1] = currentPos;
  const obj1 = '1 0 obj\r\n<< /Type /Catalog /Pages 2 0 R >>\r\nendobj\r\n';
  append(obj1);
  currentPos += new TextEncoder().encode(obj1).length;

  // Object 2: Pages
  offsets[2] = currentPos;
  const obj2 = '2 0 obj\r\n<< /Type /Pages /Kids [5 0 R] /Count 1 >>\r\nendobj\r\n';
  append(obj2);
  currentPos += new TextEncoder().encode(obj2).length;

  // Object 3: Font F1
  offsets[3] = currentPos;
  const obj3 = '3 0 obj\r\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\r\nendobj\r\n';
  append(obj3);
  currentPos += new TextEncoder().encode(obj3).length;

  // Object 4: Font F2
  offsets[4] = currentPos;
  const obj4 = '4 0 obj\r\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\r\nendobj\r\n';
  append(obj4);
  currentPos += new TextEncoder().encode(obj4).length;

  // Object 5: Page 1
  offsets[5] = currentPos;
  const obj5 = '5 0 obj\r\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792]\r\n'
    + '   /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >>\r\n'
    + '   /Contents 6 0 R\r\n>>\r\nendobj\r\n';
  append(obj5);
  currentPos += new TextEncoder().encode(obj5).length;

  // Object 6: Content Stream
  offsets[6] = currentPos;
  const streamHeader = `6 0 obj\r\n<< /Length ${streamBytes.length} >>\r\nstream\r\n`;
  append(streamHeader);
  currentPos += new TextEncoder().encode(streamHeader).length;

  appendRaw(streamBytes);
  currentPos += streamBytes.length;

  const streamFooter = '\r\nendstream\r\nendobj\r\n';
  append(streamFooter);
  currentPos += new TextEncoder().encode(streamFooter).length;

  // Cross-reference (Xref) Table
  const xrefOffset = currentPos;
  let xrefText = `xref\r\n0 ${totalObjects + 1}\r\n0000000000 65535 f \r\n`;
  for (let i = 1; i <= totalObjects; i++) {
    xrefText += `${String(offsets[i]).padStart(10, '0')} 00000 n \r\n`;
  }
  append(xrefText);

  // Trailer
  const trailerText = `trailer\r\n<< /Size ${totalObjects + 1}\r\n   /Root 1 0 R\r\n>>\r\nstartxref\r\n${xrefOffset}\r\n%%EOF\r\n`;
  append(trailerText);

  return new Blob(chunks, { type: 'application/pdf' });
}

/**
 * Downloads invoice PDF directly to user's computer
 */
export function downloadClientSideInvoicePdf(invoice) {
  const blob = createInvoicePdfBlob(invoice);
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `Invoice-${invoice.invoiceNumber || 'PA-INVOICE'}.pdf`;
  document.body.appendChild(a);
  a.click();
  window.URL.revokeObjectURL(url);
  document.body.removeChild(a);
}
