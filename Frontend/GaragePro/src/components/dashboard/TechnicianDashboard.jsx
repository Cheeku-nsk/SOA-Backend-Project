import { useState, useEffect } from 'react';
import {
  FaTools,
  FaCheckCircle,
  FaPlay,
  FaSpinner,
  FaExclamationTriangle,
  FaCar,
  FaClock,
  FaFileAlt,
  FaClipboardCheck,
} from 'react-icons/fa';
import { api } from '../../utils/api';
import { useAuth } from '../../context/AuthContext';

const TechnicianDashboard = ({ activeTab = 'active' }) => {
  const [tab, setTab] = useState(activeTab);
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [showCompleteModal, setShowCompleteModal] = useState(false);
  const [completingRecord, setCompletingRecord] = useState(null);
  const [remarks, setRemarks] = useState('');
  const { user } = useAuth();

  const loadRecords = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.get('/records/technician');
      setRecords(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to fetch technician workload');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRecords();
  }, []);

  const handleStartWork = async (recordId) => {
    setActionLoadingId(recordId);
    setError('');
    setSuccessMsg('');
    try {
      await api.put(`/records/${recordId}/status`, {
        status: 'IN_PROGRESS',
        technicianName: user?.name,
        technicianId: user?.id,
      });
      setSuccessMsg(`Service Job #${recordId} started. Status is now IN_PROGRESS.`);
      loadRecords();
    } catch (err) {
      setError(err.message || 'Failed to start service job');
    } finally {
      setActionLoadingId(null);
    }
  };

  const openCompleteModal = (record) => {
    setCompletingRecord(record);
    setRemarks('');
    setError('');
    setShowCompleteModal(true);
  };

  const handleCompleteWork = async (e) => {
    e.preventDefault();
    if (!remarks.trim()) {
      setError('Please provide maintenance notes/findings to complete service.');
      return;
    }
    setActionLoadingId(completingRecord.recordId);
    setError('');
    try {
      await api.put(`/records/${completingRecord.recordId}/status`, {
        status: 'COMPLETED',
        remarks: remarks.trim(),
        technicianName: user?.name,
        technicianId: user?.id,
      });
      setShowCompleteModal(false);
      setSuccessMsg(`Service Job #${completingRecord.recordId} successfully marked COMPLETED.`);
      loadRecords();
    } catch (err) {
      setError(err.message || 'Failed to complete service');
    } finally {
      setActionLoadingId(null);
    }
  };

  const activeJobs = records.filter((r) => r.status === 'PENDING' || r.status === 'IN_PROGRESS');
  const completedJobs = records.filter((r) => r.status === 'COMPLETED');

  return (
    <div className="space-y-6">
      {/* Top Banner & Quick Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="glass-card p-5 rounded-xl border border-surface-border">
          <div className="flex items-center justify-between text-slate-400 text-xs font-medium mb-2">
            <span>Active & Pending Jobs</span>
            <FaTools className="text-accent" />
          </div>
          <p className="text-3xl font-bold text-accent">{activeJobs.length}</p>
          <p className="text-[11px] text-slate-500 mt-1">Requiring technician attention</p>
        </div>

        <div className="glass-card p-5 rounded-xl border border-surface-border">
          <div className="flex items-center justify-between text-slate-400 text-xs font-medium mb-2">
            <span>In-Progress Service</span>
            <FaClock className="text-blue-400" />
          </div>
          <p className="text-3xl font-bold text-blue-400">
            {records.filter((r) => r.status === 'IN_PROGRESS').length}
          </p>
          <p className="text-[11px] text-slate-500 mt-1">Currently on the workshop hoist</p>
        </div>

        <div className="glass-card p-5 rounded-xl border border-surface-border">
          <div className="flex items-center justify-between text-slate-400 text-xs font-medium mb-2">
            <span>Completed Repairs</span>
            <FaCheckCircle className="text-emerald-400" />
          </div>
          <p className="text-3xl font-bold text-emerald-400">{completedJobs.length}</p>
          <p className="text-[11px] text-slate-500 mt-1">Finished certified work orders</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-surface-border pb-2">
        <button
          onClick={() => setTab('active')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors cursor-pointer flex items-center gap-2 ${
            tab === 'active'
              ? 'bg-primary text-white'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <FaTools /> Active Work Orders ({activeJobs.length})
        </button>

        <button
          onClick={() => setTab('completed')}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors cursor-pointer flex items-center gap-2 ${
            tab === 'completed'
              ? 'bg-primary text-white'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <FaClipboardCheck /> Completed History ({completedJobs.length})
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
          <p className="text-sm">Retrieving assigned technician work queue...</p>
        </div>
      ) : tab === 'active' ? (
        /* Active Jobs List */
        activeJobs.length === 0 ? (
          <div className="glass-card p-12 text-center rounded-xl border border-surface-border">
            <FaCheckCircle className="text-4xl text-emerald-400 mx-auto mb-3" />
            <h4 className="text-base font-bold text-white mb-1">No Active Repair Tasks</h4>
            <p className="text-xs text-slate-400 max-w-sm mx-auto">
              You are caught up! When new customer bookings are confirmed, they will populate here for diagnostics and service.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {activeJobs.map((r) => (
              <div
                key={r.recordId}
                className="glass-card p-6 rounded-xl border border-surface-border hover:border-accent/40 transition-all flex flex-col justify-between space-y-4"
              >
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-xs font-mono font-bold px-2 py-0.5 bg-slate-800 text-accent rounded border border-surface-border">
                      #REC-{r.recordId} (Booking #{r.bookingId})
                    </span>
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-[11px] font-semibold ${
                        r.status === 'IN_PROGRESS'
                          ? 'bg-blue-500/15 text-blue-300 border border-blue-500/30'
                          : 'bg-amber-500/15 text-amber-300 border border-amber-500/30'
                      }`}
                    >
                      {r.status}
                    </span>
                  </div>

                  <h4 className="text-base font-bold text-white mb-1">{r.serviceType}</h4>
                  <p className="text-xs text-slate-400 leading-relaxed mb-4">{r.description}</p>

                  <div className="bg-slate-950/60 p-3 rounded-lg border border-surface-border/50 text-xs space-y-1.5">
                    <div className="flex justify-between text-slate-400">
                      <span>Customer Email:</span>
                      <span className="text-slate-200 font-mono">{r.customerEmail || 'Registered Customer'}</span>
                    </div>
                    {r.startTime && (
                      <div className="flex justify-between text-slate-400">
                        <span>Started At:</span>
                        <span className="text-slate-200">{new Date(r.startTime).toLocaleTimeString()}</span>
                      </div>
                    )}
                  </div>
                </div>

                {/* Progress Controls */}
                <div className="pt-4 border-t border-surface-border/60 flex items-center justify-end gap-3">
                  {r.status === 'PENDING' && (
                    <button
                      onClick={() => handleStartWork(r.recordId)}
                      disabled={actionLoadingId === r.recordId}
                      className="btn-primary px-4 py-2 text-xs flex items-center gap-2 cursor-pointer disabled:opacity-50"
                    >
                      {actionLoadingId === r.recordId ? (
                        <FaSpinner className="animate-spin" />
                      ) : (
                        <FaPlay className="text-[10px]" />
                      )}
                      Start Service
                    </button>
                  )}

                  {r.status === 'IN_PROGRESS' && (
                    <button
                      onClick={() => openCompleteModal(r)}
                      disabled={actionLoadingId === r.recordId}
                      className="px-4 py-2 rounded-lg text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-500 flex items-center gap-2 cursor-pointer disabled:opacity-50 transition-colors"
                    >
                      <FaCheckCircle /> Complete Service
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )
      ) : (
        /* Completed History */
        completedJobs.length === 0 ? (
          <div className="glass-card p-12 text-center rounded-xl border border-surface-border">
            <FaFileAlt className="text-4xl text-slate-500 mx-auto mb-3" />
            <h4 className="text-base font-bold text-white mb-1">No Completed Service Records</h4>
            <p className="text-xs text-slate-400">Work orders marked completed will appear in this archive.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {completedJobs.map((r) => (
              <div
                key={r.recordId}
                className="glass-card p-5 rounded-xl border border-surface-border space-y-3"
              >
                <div className="flex flex-wrap items-center justify-between gap-3 pb-2 border-b border-surface-border/50">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-mono font-bold text-accent">#REC-{r.recordId}</span>
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                      COMPLETED
                    </span>
                  </div>
                  <span className="text-xs text-slate-400">
                    Finished on {r.endTime ? new Date(r.endTime).toLocaleString() : 'Recent'}
                  </span>
                </div>

                <div className="flex flex-col md:flex-row justify-between gap-4">
                  <div>
                    <h4 className="text-sm font-bold text-white">{r.serviceType}</h4>
                    <p className="text-xs text-slate-400">{r.description}</p>
                  </div>
                  <div className="text-xs text-slate-300 md:text-right">
                    <span className="text-slate-500 block">Lead Mechanic:</span>
                    <span className="font-medium text-white">{r.technicianName || user?.name}</span>
                  </div>
                </div>

                {r.remarks && (
                  <div className="bg-slate-950/70 p-3 rounded-lg border border-surface-border/50 text-xs">
                    <span className="text-slate-400 font-semibold block mb-1">Service Findings & Notes:</span>
                    <p className="text-slate-200">{r.remarks}</p>
                  </div>
                )}
              </div>
            ))}
          </div>
        )
      )}

      {/* Completion Modal */}
      {showCompleteModal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-[150] flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-surface-border rounded-2xl w-full max-w-lg p-6 md:p-8 relative shadow-2xl animate-in fade-in zoom-in-95">
            <button
              onClick={() => setShowCompleteModal(false)}
              className="absolute top-4 right-4 text-slate-400 hover:text-white p-2"
              aria-label="Close"
            >
              &times;
            </button>

            <h3 className="text-xl font-bold text-white mb-2 flex items-center gap-2">
              <FaClipboardCheck className="text-emerald-400" />
              Complete Service Work Order
            </h3>
            <p className="text-xs text-slate-400 mb-6">
              Record completion findings, inspection checkpoints, and notes for Job #{completingRecord?.recordId}.
            </p>

            {error && (
              <div className="bg-red-500/10 border border-red-500/40 text-red-400 p-3 rounded-lg text-xs mb-4">
                {error}
              </div>
            )}

            <form onSubmit={handleCompleteWork} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Service Type
                </label>
                <input
                  type="text"
                  disabled
                  value={completingRecord?.serviceType || ''}
                  className="input-field opacity-75"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Technician Remarks & Maintenance Summary *
                </label>
                <textarea
                  required
                  rows={4}
                  placeholder="e.g. Performed 40-point safety check, replaced synthetic engine oil, checked brake pad thickness (front 8mm, rear 7mm), pressure tested fluid hydraulics. System normal."
                  value={remarks}
                  onChange={(e) => setRemarks(e.target.value)}
                  className="input-field text-xs resize-none"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-surface-border">
                <button
                  type="button"
                  onClick={() => setShowCompleteModal(false)}
                  className="px-4 py-2.5 rounded-lg text-sm text-slate-300 hover:text-white bg-slate-800 cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoadingId === completingRecord?.recordId}
                  className="px-6 py-2.5 rounded-lg text-sm font-semibold text-white bg-emerald-600 hover:bg-emerald-500 flex items-center gap-2 cursor-pointer disabled:opacity-60"
                >
                  {actionLoadingId === completingRecord?.recordId && <FaSpinner className="animate-spin" />}
                  Submit Work Completion
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default TechnicianDashboard;
