import { useState, useEffect } from 'react';
import {
  FaUsers,
  FaCheck,
  FaTimes,
  FaCalendarAlt,
  FaTools,
  FaSpinner,
  FaExclamationTriangle,
  FaCar,
  FaCheckCircle,
  FaSearch,
  FaFileInvoiceDollar,
  FaEye,
  FaDownload,
  FaPrint,
  FaShieldAlt,
  FaUserCheck,
  FaInfoCircle,
} from 'react-icons/fa';
import { api, API_BASE_URL } from '../../utils/api';
import { downloadClientSideInvoicePdf } from '../../utils/pdfGenerator';

const AdminDashboard = ({ activeTab = 'approvals' }) => {
  const [tab, setTab] = useState(activeTab);
  const [pendingTechs, setPendingTechs] = useState([]);
  const [allTechs, setAllTechs] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [bays, setBays] = useState([]);
  const [invoices, setInvoices] = useState([]);
  const [billingSummary, setBillingSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [invoiceSearchQuery, setInvoiceSearchQuery] = useState('');
  const [serviceFilter, setServiceFilter] = useState('ALL');
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [downloadingId, setDownloadingId] = useState(null);

  useEffect(() => {
    if (activeTab) {
      setTab(activeTab);
    }
  }, [activeTab]);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [pendingData, techsData, bookingsData, baysData, invoicesData, summaryData] = await Promise.all([
        api.get('/auth/technicians/pending').catch(() => []),
        api.get('/auth/technicians').catch(() => []),
        api.get('/bookings').catch(() => []),
        api.get('/bookings/bays').catch(() => []),
        api.get('/billing/admin/invoices').catch(() => []),
        api.get('/billing/admin/summary').catch(() => null),
      ]);
      setPendingTechs(Array.isArray(pendingData) ? pendingData : []);
      setAllTechs(Array.isArray(techsData) ? techsData : []);
      setBookings(Array.isArray(bookingsData) ? bookingsData : []);
      setBays(Array.isArray(baysData) ? baysData : []);
      setInvoices(Array.isArray(invoicesData) ? invoicesData : []);
      setBillingSummary(summaryData);
    } catch (err) {
      setError(err.message || 'Failed to fetch admin telemetry');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleApprove = async (id) => {
    setActionLoadingId(id);
    setError('');
    setSuccessMsg('');
    try {
      await api.put(`/auth/technicians/${id}/approve`);
      setSuccessMsg(`Technician ID #${id} approved successfully`);
      loadData();
    } catch (err) {
      setError(err.message || 'Failed to approve technician');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleReject = async (id) => {
    setActionLoadingId(id);
    setError('');
    setSuccessMsg('');
    try {
      await api.put(`/auth/technicians/${id}/reject`);
      setSuccessMsg(`Technician ID #${id} application rejected`);
      loadData();
    } catch (err) {
      setError(err.message || 'Failed to reject technician');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleDownloadPdf = async (inv) => {
    setDownloadingId(inv.invoiceId);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_BASE_URL}/billing/invoices/${inv.invoiceId}/pdf`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });

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

  const filteredBookings = bookings.filter((b) => {
    const q = searchQuery.toLowerCase();
    return (
      (b.serviceType && b.serviceType.toLowerCase().includes(q)) ||
      (b.customerEmail && b.customerEmail.toLowerCase().includes(q)) ||
      (b.customerName && b.customerName.toLowerCase().includes(q)) ||
      (b.vehicleRegistration && b.vehicleRegistration.toLowerCase().includes(q)) ||
      (b.bayNumber && b.bayNumber.toLowerCase().includes(q))
    );
  });

  const filteredInvoices = invoices.filter((inv) => {
    const q = invoiceSearchQuery.toLowerCase();
    const matchesSearch =
      !q ||
      (inv.invoiceNumber && inv.invoiceNumber.toLowerCase().includes(q)) ||
      (inv.customerName && inv.customerName.toLowerCase().includes(q)) ||
      (inv.customerEmail && inv.customerEmail.toLowerCase().includes(q)) ||
      (inv.vehicleRegistrationNumber && inv.vehicleRegistrationNumber.toLowerCase().includes(q)) ||
      (inv.serviceType && inv.serviceType.toLowerCase().includes(q));

    const matchesService =
      serviceFilter === 'ALL' ||
      (inv.serviceType && inv.serviceType.toLowerCase() === serviceFilter.toLowerCase());

    return matchesSearch && matchesService;
  });

  // Unique service types for filter dropdown
  const serviceTypes = Array.from(
    new Set(invoices.map((inv) => inv.serviceType).filter(Boolean))
  );

  return (
    <div className="space-y-6">
      {/* Top Banner & Quick Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="glass-card p-5 rounded-xl border border-surface-border">
          <div className="flex items-center justify-between text-slate-400 text-xs font-medium mb-2">
            <span>Pending Approvals</span>
            <FaUsers className="text-amber-400 text-base" />
          </div>
          <p className="text-3xl font-bold text-white">{pendingTechs.length}</p>
          <p className="text-[11px] text-slate-500 mt-1">Technicians awaiting review</p>
        </div>

        <div className="glass-card p-5 rounded-xl border border-surface-border">
          <div className="flex items-center justify-between text-slate-400 text-xs font-medium mb-2">
            <span>All Bookings</span>
            <FaCalendarAlt className="text-accent text-base" />
          </div>
          <p className="text-3xl font-bold text-white">{bookings.length}</p>
          <p className="text-[11px] text-slate-500 mt-1">Registered customer services</p>
        </div>

        <div className="glass-card p-5 rounded-xl border border-surface-border">
          <div className="flex items-center justify-between text-slate-400 text-xs font-medium mb-2">
            <span>Total Invoices</span>
            <FaFileInvoiceDollar className="text-emerald-400 text-base" />
          </div>
          <p className="text-3xl font-bold text-emerald-400">
            {billingSummary ? billingSummary.totalInvoices : invoices.length}
          </p>
          <p className="text-[11px] text-slate-500 mt-1">Completed service invoices</p>
        </div>

        <div className="glass-card p-5 rounded-xl border border-surface-border">
          <div className="flex items-center justify-between text-slate-400 text-xs font-medium mb-2">
            <span>Workshop Readiness</span>
            <FaTools className="text-accent text-base" />
          </div>
          <p className="text-3xl font-bold text-white">
            {bays.length > 0 ? bays.filter((b) => b.status === 'AVAILABLE').length : 4} / {bays.length || 4}
          </p>
          <p className="text-[11px] text-slate-500 mt-1">Operational service bays</p>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="flex flex-wrap items-center gap-2 border-b border-surface-border pb-2">
        <button
          onClick={() => setTab('approvals')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors cursor-pointer flex items-center gap-2 ${
            tab === 'approvals'
              ? 'bg-primary text-white shadow-sm'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <FaUsers /> Technician Approvals
          {pendingTechs.length > 0 && (
            <span className="px-1.5 py-0.5 text-[10px] rounded-full bg-amber-500 text-black font-bold">
              {pendingTechs.length}
            </span>
          )}
        </button>

        <button
          onClick={() => setTab('bookings')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors cursor-pointer flex items-center gap-2 ${
            tab === 'bookings'
              ? 'bg-primary text-white shadow-sm'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <FaCalendarAlt /> All Bookings ({bookings.length})
        </button>

        <button
          onClick={() => setTab('bays')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors cursor-pointer flex items-center gap-2 ${
            tab === 'bays'
              ? 'bg-primary text-white shadow-sm'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <FaTools /> Service Bays ({bays.length || 4})
        </button>

        <button
          onClick={() => setTab('invoices')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors cursor-pointer flex items-center gap-2 ${
            tab === 'invoices'
              ? 'bg-primary text-white shadow-sm'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <FaFileInvoiceDollar /> Billing Management ({invoices.length})
        </button>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/40 text-red-400 p-3.5 rounded-lg text-xs flex items-center gap-2">
          <FaExclamationTriangle /> {error}
        </div>
      )}

      {successMsg && (
        <div className="bg-emerald-500/10 border border-emerald-500/40 text-emerald-300 p-3.5 rounded-lg text-xs flex items-center gap-2">
          <FaCheckCircle /> {successMsg}
        </div>
      )}

      {loading ? (
        <div className="py-20 flex flex-col items-center justify-center text-slate-400 gap-3">
          <FaSpinner className="animate-spin text-3xl text-accent" />
          <p className="text-sm">Fetching administrative management records...</p>
        </div>
      ) : tab === 'approvals' ? (
        /* Technician Approvals Section */
        <div className="space-y-6">
          <div>
            <h3 className="text-lg font-bold text-white mb-1">Pending Approval Queue</h3>
            <p className="text-xs text-slate-400">
              Review technician applications. Only approved technicians can log in and access repair tasks.
            </p>
          </div>

          {pendingTechs.length === 0 ? (
            <div className="glass-card p-10 text-center rounded-xl border border-surface-border">
              <FaCheckCircle className="text-4xl text-emerald-400 mx-auto mb-3" />
              <h4 className="text-base font-bold text-white mb-1">Approval Queue is Clear</h4>
              <p className="text-xs text-slate-400">
                There are currently no technician registration applications awaiting administrator review.
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {pendingTechs.map((t) => (
                <div
                  key={t.id}
                  className="glass-card p-5 rounded-xl border border-amber-500/30 bg-amber-500/[0.03] flex flex-col justify-between space-y-4"
                >
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-xs font-mono font-bold px-2 py-0.5 rounded bg-amber-500/10 text-amber-400 border border-amber-500/30">
                        #TECH-{t.id}
                      </span>
                      <span className="text-[10px] text-slate-400">Status: {t.status}</span>
                    </div>
                    <h4 className="text-base font-bold text-white">{t.name}</h4>
                    <p className="text-xs text-slate-300">{t.email}</p>
                    {t.phone && <p className="text-xs text-slate-400 mt-1">Phone: {t.phone}</p>}
                    {t.specialization && (
                      <p className="text-xs text-slate-400">Specialization: {t.specialization}</p>
                    )}
                  </div>

                  <div className="flex items-center gap-2 pt-3 border-t border-surface-border/50">
                    <button
                      onClick={() => handleApprove(t.id)}
                      disabled={actionLoadingId === t.id}
                      className="flex-1 py-2 px-3 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                    >
                      {actionLoadingId === t.id ? <FaSpinner className="animate-spin" /> : <FaCheck />}
                      <span>Approve</span>
                    </button>
                    <button
                      onClick={() => handleReject(t.id)}
                      disabled={actionLoadingId === t.id}
                      className="flex-1 py-2 px-3 rounded-lg bg-red-600/20 hover:bg-red-600 text-red-300 hover:text-white border border-red-500/30 text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
                    >
                      {actionLoadingId === t.id ? <FaSpinner className="animate-spin" /> : <FaTimes />}
                      <span>Reject</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {allTechs.length > 0 && (
            <div className="pt-6 border-t border-surface-border space-y-3">
              <h4 className="text-sm font-bold text-white">Active Workshop Technicians ({allTechs.length})</h4>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {allTechs.map((t) => (
                  <div key={t.id} className="glass-card p-4 rounded-xl border border-surface-border space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-white">{t.name}</span>
                      <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        Active
                      </span>
                    </div>
                    <p className="text-xs text-slate-400">{t.email}</p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      ) : tab === 'bookings' ? (
        /* All Bookings Section */
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <h3 className="text-lg font-bold text-white">All Workshop Appointments</h3>
              <p className="text-xs text-slate-400">Total appointments across all customer accounts.</p>
            </div>

            <div className="relative w-full sm:w-64">
              <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 text-xs" />
              <input
                type="text"
                placeholder="Search bookings..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full bg-slate-900 border border-surface-border rounded-lg pl-8 pr-3 py-1.5 text-xs text-white focus:outline-none focus:border-accent"
              />
            </div>
          </div>

          <div className="space-y-3">
            {filteredBookings.map((b) => (
              <div
                key={b.bookingId}
                className="glass-card p-4 rounded-xl border border-surface-border flex flex-wrap items-center justify-between gap-4"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-mono font-bold px-2 py-0.5 bg-slate-800 text-accent rounded">
                      #BK-{b.bookingId}
                    </span>
                    <span className="text-xs font-bold text-white">{b.serviceType}</span>
                    <span
                      className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${
                        b.status === 'CONFIRMED'
                          ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                          : b.status === 'IN_PROGRESS'
                          ? 'bg-blue-500/10 text-blue-400 border border-blue-500/30'
                          : b.status === 'COMPLETED'
                          ? 'bg-teal-500/10 text-teal-400 border border-teal-500/30'
                          : 'bg-slate-700 text-slate-400'
                      }`}
                    >
                      {b.status}
                    </span>
                  </div>
                  <div className="flex flex-wrap items-center gap-x-4 text-xs text-slate-400">
                    <span>Customer: <strong className="text-white">{b.customerName}</strong> ({b.customerEmail})</span>
                    <span>Vehicle: <strong className="text-white">{b.vehicleRegistration}</strong></span>
                    <span>Slot: <strong className="text-white">{b.slotDate} ({b.startTime} - {b.endTime})</strong></span>
                  </div>
                </div>

                <div className="text-right text-xs text-slate-400">
                  <span>{b.bayNumber || 'General Bay'}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : tab === 'invoices' ? (
        /* Billing Management Section */
        <div className="space-y-6">
          {/* Header & Metrics */}
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <FaFileInvoiceDollar className="text-accent" /> Billing Management
              </h3>
              <p className="text-xs text-slate-400">
                Oversight of all certified final invoices automatically generated upon service completion.
              </p>
            </div>
            <button
              onClick={loadData}
              className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              <FaSpinner className={loading ? 'animate-spin' : ''} /> Refresh Data
            </button>
          </div>

          {/* Actual Billing Telemetry Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="glass-card p-5 rounded-xl border border-surface-border">
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
                Total Invoices Generated
              </span>
              <p className="text-3xl font-extrabold text-white">
                {billingSummary ? billingSummary.totalInvoices : invoices.length}
              </p>
              <p className="text-[11px] text-slate-500 mt-1">Verified completed jobs</p>
            </div>

            <div className="glass-card p-5 rounded-xl border border-accent/20 bg-accent/[0.02]">
              <span className="text-[11px] font-bold text-accent uppercase tracking-wider block mb-1">
                Total Value of Generated Invoices
              </span>
              <p className="text-3xl font-extrabold text-accent">
                ${billingSummary && billingSummary.totalInvoiceValue != null
                  ? Number(billingSummary.totalInvoiceValue).toFixed(2)
                  : invoices.reduce((acc, i) => acc + Number(i.totalAmount || 0), 0).toFixed(2)}
              </p>
              <p className="text-[11px] text-slate-400 mt-1">
                (Total value of generated invoices, not collected revenue)
              </p>
            </div>

            <div className="glass-card p-5 rounded-xl border border-surface-border">
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
                Average Invoice Value
              </span>
              <p className="text-3xl font-extrabold text-white">
                ${billingSummary && billingSummary.averageInvoiceValue != null
                  ? Number(billingSummary.averageInvoiceValue).toFixed(2)
                  : (
                      invoices.length > 0
                        ? (invoices.reduce((acc, i) => acc + Number(i.totalAmount || 0), 0) / invoices.length).toFixed(2)
                        : '0.00'
                    )}
              </p>
              <p className="text-[11px] text-slate-500 mt-1">Mean invoice amount</p>
            </div>
          </div>

          {/* Search & Service Filter */}
          <div className="flex flex-col sm:flex-row items-center gap-3 bg-slate-950/60 p-3 rounded-xl border border-surface-border">
            <div className="relative flex-1 w-full">
              <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 text-xs" />
              <input
                type="text"
                placeholder="Search by invoice #, customer name, email, or registration..."
                value={invoiceSearchQuery}
                onChange={(e) => setInvoiceSearchQuery(e.target.value)}
                className="w-full bg-slate-900 border border-surface-border rounded-lg pl-8 pr-3 py-2 text-xs text-white focus:outline-none focus:border-accent"
              />
            </div>

            <div className="w-full sm:w-auto flex items-center gap-2">
              <span className="text-xs text-slate-400">Service:</span>
              <select
                value={serviceFilter}
                onChange={(e) => setServiceFilter(e.target.value)}
                className="bg-slate-900 border border-surface-border rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:border-accent"
              >
                <option value="ALL">All Services</option>
                {serviceTypes.map((st) => (
                  <option key={st} value={st}>{st}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Invoices List / Table */}
          {filteredInvoices.length === 0 ? (
            <div className="glass-card p-10 text-center rounded-xl border border-surface-border">
              <FaFileInvoiceDollar className="text-4xl text-slate-500 mx-auto mb-3" />
              <h4 className="text-base font-bold text-white mb-1">No Invoices Match Query</h4>
              <p className="text-xs text-slate-400">
                {invoices.length === 0
                  ? 'No invoices have been generated yet. Invoices appear automatically after technicians mark services as COMPLETED.'
                  : 'Try adjusting your search criteria or service filter.'}
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {filteredInvoices.map((inv) => (
                <div
                  key={inv.invoiceId}
                  className="glass-card p-5 rounded-xl border border-surface-border hover:border-accent/40 transition-all flex flex-col md:flex-row md:items-center justify-between gap-4"
                >
                  <div className="space-y-1.5 flex-1">
                    <div className="flex flex-wrap items-center gap-2.5">
                      <span className="text-xs font-mono font-bold px-2.5 py-0.5 bg-slate-800 text-accent rounded border border-surface-border">
                        {inv.invoiceNumber}
                      </span>
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        Generated
                      </span>
                      <span className="text-xs text-slate-400 flex items-center gap-1">
                        <FaCalendarAlt className="text-[10px]" />
                        {inv.generatedAt ? new Date(inv.generatedAt).toLocaleDateString() : 'N/A'}
                      </span>
                    </div>

                    <h4 className="text-sm font-bold text-white">{inv.serviceType}</h4>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-1 text-xs text-slate-400">
                      <div>
                        Customer: <strong className="text-white">{inv.customerName}</strong> ({inv.customerEmail})
                      </div>
                      <div>
                        Vehicle: <strong className="text-white">{inv.vehicleMake} {inv.vehicleModel}</strong> ({inv.vehicleRegistrationNumber})
                      </div>
                      <div>
                        Work Order: <span>#BK-{inv.bookingId} / #REC-{inv.serviceRecordId}</span>
                      </div>
                      {inv.technicianName && (
                        <div>
                          Technician: <span className="text-slate-300">{inv.technicianName}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center justify-between md:flex-col md:items-end gap-3 pt-3 md:pt-0 border-t md:border-t-0 border-surface-border/50">
                    <div className="text-left md:text-right">
                      <span className="text-[10px] text-slate-400 uppercase tracking-wider block">Total Amount</span>
                      <span className="text-xl font-bold font-mono text-accent">
                        ${Number(inv.totalAmount).toFixed(2)}
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setSelectedInvoice(inv)}
                        className="px-3 py-1.5 rounded-lg bg-primary/20 hover:bg-primary/30 text-accent border border-accent/20 text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
                      >
                        <FaEye /> View Details
                      </button>
                      <button
                        onClick={() => handleDownloadPdf(inv)}
                        disabled={downloadingId === inv.invoiceId}
                        className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-surface-border text-xs transition-colors cursor-pointer"
                        title="Download Invoice PDF"
                      >
                        {downloadingId === inv.invoiceId ? <FaSpinner className="animate-spin" /> : <FaDownload />}
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      ) : (
        /* Service Bays Section */
        <div className="space-y-4">
          <div>
            <h3 className="text-lg font-bold text-white">Service Bays & Workstation Status</h3>
            <p className="text-xs text-slate-400">Real-time status of precision automotive bays.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {bays.map((bay) => (
              <div key={bay.bayId} className="glass-card p-5 rounded-xl border border-surface-border space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-bold text-white">{bay.bayNumber}</span>
                  <span
                    className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      bay.status === 'AVAILABLE'
                        ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                        : 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                    }`}
                  >
                    {bay.status}
                  </span>
                </div>
                <p className="text-xs text-slate-300 font-medium">{bay.bayType}</p>
                <div className="pt-2 border-t border-surface-border/50 text-[11px] text-slate-400 flex items-center justify-between">
                  <span>Daily Slots</span>
                  <span className="text-white font-mono font-semibold">5 sessions</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Admin Invoice Detail Modal */}
      {selectedInvoice && (
        <AdminInvoiceModal
          invoice={selectedInvoice}
          onClose={() => setSelectedInvoice(null)}
          onDownloadPdf={() => handleDownloadPdf(selectedInvoice)}
          downloading={downloadingId === selectedInvoice.invoiceId}
        />
      )}
    </div>
  );
};

const AdminInvoiceModal = ({ invoice, onClose, onDownloadPdf, downloading }) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 md:p-6 bg-black/80 backdrop-blur-sm">
      <div className="relative w-full max-w-3xl max-h-[92vh] flex flex-col bg-slate-900 border border-surface-border rounded-2xl shadow-2xl overflow-hidden print:border-none print:shadow-none print:bg-white print:text-black print:max-h-none print:overflow-visible">
        {/* Top bar - Sticky / Never Clipped */}
        <div className="flex items-center justify-between px-5 sm:px-6 py-4 border-b border-surface-border bg-slate-950/90 shrink-0 print:hidden">
          <div className="flex items-center gap-2 text-sm font-semibold text-white">
            <FaFileInvoiceDollar className="text-accent" />
            <span>Admin View: {invoice.invoiceNumber}</span>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => window.print()}
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

        {/* Scrollable Printable Area - Complete Content from Header to Footer */}
        <div className="flex-1 overflow-y-auto p-5 sm:p-6 md:p-8 space-y-6 print:p-0 print:overflow-visible custom-scrollbar">
          {/* Header Branding */}
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center pb-6 border-b border-surface-border gap-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <span className="w-8 h-8 rounded-lg bg-primary/20 text-accent flex items-center justify-center text-lg font-black border border-accent/20">
                  PA
                </span>
                <h1 className="text-2xl font-black text-white tracking-tight">PRECISIONAUTO CARE</h1>
              </div>
              <p className="text-xs text-slate-400">Certified Workshop Invoicing & Customer Records</p>
            </div>

            <div className="text-left sm:text-right">
              <span className="inline-block px-3 py-1 rounded-full text-xs font-bold bg-accent/10 text-accent border border-accent/20 mb-2">
                FINAL INVOICE
              </span>
              <p className="text-sm font-mono font-bold text-white">{invoice.invoiceNumber}</p>
              <p className="text-xs text-slate-400">
                Generated: {invoice.generatedAt ? new Date(invoice.generatedAt).toLocaleString() : 'N/A'}
              </p>
            </div>
          </div>

          {/* Details 2-Column Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 bg-slate-950/50 p-5 rounded-xl border border-surface-border/60 text-xs">
            <div className="space-y-1.5">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-2">
                Customer Details
              </span>
              <p className="text-sm font-semibold text-white">{invoice.customerName}</p>
              <p className="text-slate-300">{invoice.customerEmail}</p>
              {invoice.customerId && (
                <p className="text-slate-400">Customer ID: #CUST-{invoice.customerId}</p>
              )}
            </div>

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
            <div className="flex items-center justify-between text-xs">
              <span className="font-bold text-slate-300 uppercase tracking-wider">
                Service Type: {invoice.serviceType}
              </span>
              {invoice.technicianName && (
                <span className="text-slate-400 flex items-center gap-1.5">
                  <FaUserCheck className="text-accent" /> Assigned Technician: {invoice.technicianName}
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
              Official customer invoice recorded in precisionauto_billing database.
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

        {/* Modal Footer - Anchored & Always Accessible */}
        <div className="px-5 sm:px-6 py-3.5 bg-slate-950/90 border-t border-surface-border flex items-center justify-between text-xs text-slate-400 shrink-0 print:hidden">
          <span>Administrative billing oversight. Scope strictly excludes payment gateway integrations.</span>
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

export default AdminDashboard;
