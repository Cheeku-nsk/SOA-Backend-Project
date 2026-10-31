import { useState, useEffect } from 'react';
import {
  FaFileInvoiceDollar,
  FaCalendarAlt,
  FaCar,
  FaTools,
  FaCheckCircle,
  FaSpinner,
  FaDownload,
  FaPrint,
  FaTimes,
  FaEye,
  FaUserCheck,
  FaShieldAlt,
  FaInfoCircle,
} from 'react-icons/fa';
import { api, fetchWithAuth, API_BASE_URL } from '../../utils/api';
import { downloadClientSideInvoicePdf } from '../../utils/pdfGenerator';

const CustomerBillings = () => {
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [downloadingId, setDownloadingId] = useState(null);

  const fetchInvoices = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.get('/billing/my-invoices');
      setInvoices(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to retrieve your billing records');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvoices();
  }, []);

  const handleDownloadPdf = async (inv) => {
    setDownloadingId(inv.invoiceId);
    try {
      const response = await fetchWithAuth(`/billing/invoices/${inv.invoiceId}/pdf`);

      if (!response.ok) {
        throw new Error('Server PDF route unavailable');
      }

      const contentType = response.headers.get('content-type');
      if (contentType && !contentType.includes('application/pdf')) {
        throw new Error('Invalid content type from server');
      }

      const blob = await response.blob();
      if (blob.size < 100) {
        throw new Error('Corrupted or empty PDF from server');
      }

      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Invoice-${inv.invoiceNumber}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch {
      // High-quality client-side standard PDF generation fallback
      downloadClientSideInvoicePdf(inv);
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="pb-4 border-b border-surface-border flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-white tracking-tight flex items-center gap-2">
            <FaFileInvoiceDollar className="text-accent" /> My Billings
          </h2>
          <p className="text-slate-400 text-sm mt-1">
            Review certified final invoices and itemized cost breakdowns for your completed services.
          </p>
        </div>

        {invoices.length > 0 && (
          <div className="px-3.5 py-1.5 rounded-lg bg-slate-900 border border-surface-border text-xs text-slate-300 flex items-center gap-2 w-max">
            <span className="font-semibold text-white">{invoices.length}</span> Invoices Generated
          </div>
        )}
      </div>

      {error && (
        <div className="glass-card p-6 rounded-xl border border-red-500/30 bg-red-500/10 text-center space-y-3">
          <div className="w-12 h-12 bg-red-500/20 text-red-400 rounded-full flex items-center justify-center text-xl mx-auto border border-red-500/30">
            <FaInfoCircle />
          </div>
          <div>
            <h3 className="text-base font-bold text-white mb-1">Billing Service Notice</h3>
            <p className="text-red-300 text-xs max-w-md mx-auto">{error}</p>
          </div>
          <button
            onClick={fetchInvoices}
            className="px-4 py-1.5 rounded-lg bg-red-600 hover:bg-red-500 text-white text-xs font-semibold transition-colors cursor-pointer inline-flex items-center gap-2"
          >
            Retry Connection
          </button>
        </div>
      )}

      {loading ? (
        <div className="py-20 flex flex-col items-center justify-center text-slate-400 gap-3">
          <FaSpinner className="animate-spin text-3xl text-accent" />
          <p className="text-sm">Retrieving your invoices from billing service...</p>
        </div>
      ) : error ? null : invoices.length === 0 ? (
        /* Empty State: Only displayed when request succeeded with 0 invoices */
        <div className="glass-card p-12 text-center rounded-2xl border border-surface-border flex flex-col items-center">
          <div className="w-20 h-20 bg-slate-800/60 rounded-full flex items-center justify-center text-slate-500 text-4xl mb-4 border border-surface-border">
            <FaFileInvoiceDollar />
          </div>
          <h3 className="text-xl font-bold text-white mb-2">No Invoices Available</h3>
          <p className="text-slate-400 max-w-md text-sm">
            No invoices yet. Your invoice will appear here after your vehicle service is completed.
          </p>
        </div>
      ) : (
        /* Invoices List */
        <div className="space-y-4">
          {invoices.map((inv) => (
            <div
              key={inv.invoiceId}
              className="glass-card p-6 rounded-xl border border-surface-border hover:border-accent/40 transition-all space-y-4"
            >
              <div className="flex flex-wrap items-center justify-between gap-4 pb-3 border-b border-surface-border/50">
                <div className="flex items-center gap-3">
                  <span className="text-xs font-mono font-bold px-2.5 py-1 bg-slate-800 text-accent rounded border border-surface-border">
                    {inv.invoiceNumber}
                  </span>
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 flex items-center gap-1">
                    <FaCheckCircle className="text-[10px]" /> Invoice Generated
                  </span>
                </div>
                <div className="flex items-center gap-4 text-xs text-slate-400">
                  <span className="flex items-center gap-1.5">
                    <FaCalendarAlt /> Generated: {inv.generatedAt ? new Date(inv.generatedAt).toLocaleDateString() : 'N/A'}
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-center">
                {/* Service & Vehicle Info */}
                <div className="md:col-span-2 space-y-1">
                  <h4 className="text-base font-bold text-white tracking-tight">{inv.serviceType}</h4>
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-300">
                    <span className="flex items-center gap-1.5 text-slate-400">
                      <FaCar className="text-accent" />
                      <strong className="text-white">{inv.vehicleMake} {inv.vehicleModel}</strong> ({inv.vehicleRegistrationNumber})
                    </span>
                    <span className="text-slate-400">
                      Booking: <strong className="text-white">#BK-{inv.bookingId}</strong>
                    </span>
                  </div>
                  {inv.serviceCompletedAt && (
                    <p className="text-[11px] text-slate-400">
                      Service Completed: {new Date(inv.serviceCompletedAt).toLocaleString()}
                    </p>
                  )}
                </div>

                {/* Amount */}
                <div className="text-left md:text-right">
                  <span className="text-[11px] text-slate-400 block uppercase tracking-wider">Total Amount</span>
                  <span className="text-2xl font-bold text-accent">
                    ${Number(inv.totalAmount).toFixed(2)}
                  </span>
                  <span className="text-[10px] text-slate-500 block">Includes {inv.taxPercentage}% tax</span>
                </div>

                {/* Actions */}
                <div className="flex items-center justify-start md:justify-end gap-2">
                  <button
                    onClick={() => setSelectedInvoice(inv)}
                    className="px-4 py-2 rounded-lg bg-primary/20 hover:bg-primary/30 text-accent border border-accent/20 hover:border-accent/40 text-xs font-semibold flex items-center gap-2 transition-all cursor-pointer"
                  >
                    <FaEye /> View Invoice
                  </button>
                  <button
                    onClick={() => handleDownloadPdf(inv)}
                    disabled={downloadingId === inv.invoiceId}
                    className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-surface-border text-xs transition-colors cursor-pointer"
                    title="Download Invoice PDF"
                  >
                    {downloadingId === inv.invoiceId ? (
                      <FaSpinner className="animate-spin" />
                    ) : (
                      <FaDownload />
                    )}
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Invoice Details Modal */}
      {selectedInvoice && (
        <InvoiceDetailModal
          invoice={selectedInvoice}
          onClose={() => setSelectedInvoice(null)}
          onDownloadPdf={() => handleDownloadPdf(selectedInvoice)}
          downloading={downloadingId === selectedInvoice.invoiceId}
        />
      )}
    </div>
  );
};

const InvoiceDetailModal = ({ invoice, onClose, onDownloadPdf, downloading }) => {
  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 md:p-6 bg-black/80 backdrop-blur-sm">
      <div className="relative w-full max-w-3xl max-h-[92vh] flex flex-col bg-slate-900 border border-surface-border rounded-2xl shadow-2xl overflow-hidden print:border-none print:shadow-none print:bg-white print:text-black print:max-h-none print:overflow-visible">
        {/* Modal Top Bar - Sticky / Never Clipped */}
        <div className="flex items-center justify-between px-5 sm:px-6 py-4 border-b border-surface-border bg-slate-950/90 shrink-0 print:hidden">
          <div className="flex items-center gap-2 text-sm font-semibold text-white">
            <FaFileInvoiceDollar className="text-accent" />
            <span>Official Invoice: {invoice.invoiceNumber}</span>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={handlePrint}
              className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-surface-border text-xs flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              <FaPrint /> Print
            </button>
            <button
              onClick={onDownloadPdf}
              disabled={downloading}
              className="px-3 py-1.5 rounded-lg bg-primary hover:bg-primary-hover text-white text-xs font-medium flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              {downloading ? <FaSpinner className="animate-spin" /> : <FaDownload />}
              <span>Download PDF</span>
            </button>
            <button
              onClick={onClose}
              className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors cursor-pointer"
            >
              <FaTimes size={18} />
            </button>
          </div>
        </div>

        {/* Scrollable Invoice Document Body - Complete Content from Header to Footer */}
        <div id="invoice-printable-area" className="flex-1 overflow-y-auto p-5 sm:p-6 md:p-8 space-y-6 print:p-0 print:overflow-visible custom-scrollbar">
          {/* Header Branding */}
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center pb-6 border-b border-surface-border gap-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <span className="w-8 h-8 rounded-lg bg-primary/20 text-accent flex items-center justify-center text-lg font-black border border-accent/20">
                  PA
                </span>
                <h1 className="text-2xl font-black text-white tracking-tight">PRECISIONAUTO CARE</h1>
              </div>
              <p className="text-xs text-slate-400">Certified Workshop & Automotive Engineering</p>
              <p className="text-xs text-slate-400">100 Workshop Boulevard, Tech District • support@precisionautocare.com</p>
            </div>

            <div className="text-left sm:text-right">
              <span className="inline-block px-3 py-1 rounded-full text-xs font-bold bg-accent/10 text-accent border border-accent/20 mb-2">
                FINAL INVOICE
              </span>
              <p className="text-sm font-mono font-bold text-white">{invoice.invoiceNumber}</p>
              <p className="text-xs text-slate-400">
                Date: {invoice.generatedAt ? new Date(invoice.generatedAt).toLocaleDateString() : 'N/A'}
              </p>
            </div>
          </div>

          {/* Details 2-Column Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 bg-slate-950/50 p-5 rounded-xl border border-surface-border/60 text-xs">
            {/* Customer Details */}
            <div className="space-y-1.5">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-2">
                Customer Information
              </span>
              <p className="text-sm font-semibold text-white">{invoice.customerName}</p>
              <p className="text-slate-300">{invoice.customerEmail}</p>
              {invoice.customerId && (
                <p className="text-slate-400">Customer ID: #CUST-{invoice.customerId}</p>
              )}
            </div>

            {/* Vehicle & Service References */}
            <div className="space-y-1.5">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-2">
                Vehicle & Work Order Details
              </span>
              <p className="text-sm font-semibold text-white">
                {invoice.vehicleMake} {invoice.vehicleModel}
              </p>
              <p className="text-slate-300">
                Registration: <span className="font-mono font-bold text-accent">{invoice.vehicleRegistrationNumber}</span>
              </p>
              <p className="text-slate-400">
                Booking ID: #BK-{invoice.bookingId} • Service Record: #REC-{invoice.serviceRecordId}
              </p>
              {invoice.serviceCompletedAt && (
                <p className="text-slate-400">
                  Completed: {new Date(invoice.serviceCompletedAt).toLocaleString()}
                </p>
              )}
            </div>
          </div>

          {/* Service & Technician Findings */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-300 uppercase tracking-wider">
                Service Performed: {invoice.serviceType}
              </span>
              {invoice.technicianName && (
                <span className="text-xs text-slate-400 flex items-center gap-1.5">
                  <FaUserCheck className="text-accent" /> Serviced by {invoice.technicianName}
                </span>
              )}
            </div>

            {invoice.remarks && (
              <div className="p-3.5 rounded-lg bg-slate-950/70 border border-surface-border text-xs space-y-1">
                <span className="font-semibold text-emerald-400 flex items-center gap-1.5">
                  <FaCheckCircle className="text-xs" /> Certified Technician Findings:
                </span>
                <p className="text-slate-300 leading-relaxed">{invoice.remarks}</p>
              </div>
            )}
          </div>

          {/* Itemized Charges Table */}
          <div className="border border-surface-border rounded-xl overflow-hidden">
            <table className="w-full text-xs text-left">
              <thead className="bg-slate-950/80 text-slate-400 uppercase text-[10px] tracking-wider border-b border-surface-border">
                <tr>
                  <th className="py-3 px-4">Service / Charge Item</th>
                  <th className="py-3 px-4 text-right">Amount (USD)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-surface-border/50 text-slate-300">
                <tr>
                  <td className="py-3 px-4 font-medium text-white">
                    Certified Labor Charges ({invoice.serviceType})
                  </td>
                  <td className="py-3 px-4 text-right font-mono">
                    ${Number(invoice.laborCharges || 0).toFixed(2)}
                  </td>
                </tr>
                <tr>
                  <td className="py-3 px-4 font-medium text-white">
                    Spare Parts & Workshop Fluids
                  </td>
                  <td className="py-3 px-4 text-right font-mono">
                    ${Number(invoice.partsCharges || 0).toFixed(2)}
                  </td>
                </tr>
                {Number(invoice.additionalCharges || 0) > 0 && (
                  <tr>
                    <td className="py-3 px-4 font-medium text-white">
                      Technician Additional Service & Consumables
                    </td>
                    <td className="py-3 px-4 text-right font-mono">
                      ${Number(invoice.additionalCharges).toFixed(2)}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Subtotal, Tax and Grand Total */}
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 pt-2">
            <div className="text-xs text-slate-500 max-w-sm">
              <span className="flex items-center gap-1.5 text-slate-400 font-medium mb-1">
                <FaShieldAlt className="text-accent" /> PrecisionAuto Service Guarantee
              </span>
              All workshop services and replacement parts include our certified 12-month / 12,000-mile precision warranty.
            </div>

            <div className="w-full sm:w-64 space-y-2 text-xs">
              <div className="flex justify-between text-slate-400">
                <span>Subtotal:</span>
                <span className="font-mono text-white">${Number(invoice.subtotal || 0).toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Tax ({invoice.taxPercentage}%):</span>
                <span className="font-mono text-white">${Number(invoice.taxAmount || 0).toFixed(2)}</span>
              </div>
              <div className="flex justify-between items-center pt-2 border-t border-surface-border text-sm font-bold text-white">
                <span>Grand Total:</span>
                <span className="text-xl font-bold font-mono text-accent">
                  ${Number(invoice.totalAmount || 0).toFixed(2)}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Modal Footer Note - Anchored & Always Accessible */}
        <div className="px-5 sm:px-6 py-3.5 bg-slate-950/90 border-t border-surface-border flex items-center justify-between text-xs text-slate-400 shrink-0 print:hidden">
          <span>Final certified invoice. No payment processing required in this system release.</span>
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-colors cursor-pointer"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default CustomerBillings;
