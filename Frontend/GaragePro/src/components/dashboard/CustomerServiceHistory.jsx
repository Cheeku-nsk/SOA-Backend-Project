import { useState, useEffect } from 'react';
import { FaHistory, FaCheckCircle, FaSpinner, FaTools, FaCalendarAlt, FaUserCheck, FaFileAlt, FaFileInvoiceDollar } from 'react-icons/fa';
import { api } from '../../utils/api';
import { useNavigate } from 'react-router-dom';

const CustomerServiceHistory = () => {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const fetchHistory = async () => {
      setLoading(true);
      setError('');
      try {
        const data = await api.get('/records/customer');
        setRecords(Array.isArray(data) ? data : []);
      } catch (err) {
        setError(err.message || 'Failed to retrieve service history');
      } finally {
        setLoading(false);
      }
    };
    fetchHistory();
  }, []);

  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-surface-border">
        <h2 className="text-2xl font-bold text-white tracking-tight flex items-center gap-2">
          <FaHistory className="text-accent" /> Service History
        </h2>
        <p className="text-slate-400 text-sm mt-1">
          Complete diagnostic records and certified maintenance reports for your vehicles.
        </p>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/40 text-red-400 p-4 rounded-xl text-sm">
          {error}
        </div>
      )}

      {loading ? (
        <div className="py-20 flex flex-col items-center justify-center text-slate-400 gap-3">
          <FaSpinner className="animate-spin text-3xl text-accent" />
          <p className="text-sm">Retrieving your maintenance logs...</p>
        </div>
      ) : records.length === 0 ? (
        <div className="glass-card p-12 text-center rounded-2xl border border-surface-border flex flex-col items-center">
          <div className="w-20 h-20 bg-slate-800/60 rounded-full flex items-center justify-center text-slate-500 text-4xl mb-4 border border-surface-border">
            <FaHistory />
          </div>
          <h3 className="text-xl font-bold text-white mb-2">No Service Records Yet</h3>
          <p className="text-slate-400 max-w-md text-sm">
            Once your vehicle completes service in one of our bays, full technician inspection notes and work completion logs will appear here.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {records.map((r) => (
            <div
              key={r.recordId}
              className="glass-card p-6 rounded-xl border border-surface-border hover:border-accent/40 transition-all space-y-4"
            >
              <div className="flex flex-wrap items-center justify-between gap-4 pb-3 border-b border-surface-border/50">
                <div className="flex items-center gap-3">
                  <span className="text-xs font-mono font-bold px-2.5 py-0.5 bg-slate-800 text-accent rounded border border-surface-border">
                    #REC-{r.recordId}
                  </span>
                  <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                    r.status === 'COMPLETED'
                      ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                      : 'bg-blue-500/10 text-blue-400 border border-blue-500/20'
                  }`}>
                    {r.status}
                  </span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs text-slate-400 flex items-center gap-1.5">
                    <FaCalendarAlt /> {new Date(r.createdAt).toLocaleDateString()}
                  </span>
                  {r.status === 'COMPLETED' && (
                    <button
                      onClick={() => navigate('/dashboard/my-billings')}
                      className="px-2.5 py-1 rounded bg-primary/20 hover:bg-primary/30 text-accent border border-accent/20 text-xs font-semibold flex items-center gap-1 transition-colors cursor-pointer"
                    >
                      <FaFileInvoiceDollar /> View Invoice
                    </button>
                  )}
                </div>
              </div>

              <div>
                <h4 className="text-base font-bold text-white mb-1">{r.serviceType}</h4>
                <p className="text-xs text-slate-400">{r.description}</p>
              </div>

              {/* Technician and Findings */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-slate-950/60 p-4 rounded-lg border border-surface-border/40 text-xs">
                <div>
                  <span className="text-slate-500 block mb-1 flex items-center gap-1.5">
                    <FaUserCheck /> Assigned Technician
                  </span>
                  <span className="text-white font-medium">
                    {r.technicianName || r.mechanic || 'PrecisionAuto Workshop Team'}
                  </span>
                </div>
                <div>
                  <span className="text-slate-500 block mb-1 flex items-center gap-1.5">
                    <FaCheckCircle className="text-emerald-400" /> Completion Timestamp
                  </span>
                  <span className="text-slate-300">
                    {r.endTime ? new Date(r.endTime).toLocaleString() : 'In Progress'}
                  </span>
                </div>

                {r.remarks && (
                  <div className="md:col-span-2 pt-2 border-t border-surface-border/30">
                    <span className="text-slate-500 block mb-1 flex items-center gap-1.5">
                      <FaFileAlt /> Technician Remarks & Findings
                    </span>
                    <p className="text-slate-200 leading-relaxed bg-slate-900/80 p-3 rounded border border-surface-border/50">
                      {r.remarks}
                    </p>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default CustomerServiceHistory;
