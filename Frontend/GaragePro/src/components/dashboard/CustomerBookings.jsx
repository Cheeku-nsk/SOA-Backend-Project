import { useState, useEffect } from 'react';
import { FaCalendarAlt, FaCar, FaClock, FaTools, FaTimesCircle, FaCheckCircle, FaSpinner, FaPlus, FaCheck, FaFileInvoiceDollar } from 'react-icons/fa';
import { api } from '../../utils/api';
import { useAuth } from '../../context/AuthContext';
import { useNavigate } from 'react-router-dom';

const CustomerBookings = () => {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [cancellingId, setCancellingId] = useState(null);
  const { setIsBookingOpen } = useAuth();
  const navigate = useNavigate();

  const fetchBookings = async () => {
    setLoading(true);
    setError('');
    try {
      const [bookingsData, recordsData] = await Promise.all([
        api.get('/bookings/my').catch(() => []),
        api.get('/records/customer').catch(() => []),
      ]);

      const myRecords = Array.isArray(recordsData) ? recordsData : [];
      const recordsByBookingId = new Map();
      myRecords.forEach((r) => {
        if (r.bookingId) {
          recordsByBookingId.set(r.bookingId, r);
        }
      });

      const updatedBookings = (Array.isArray(bookingsData) ? bookingsData : []).map((b) => {
        const matchingRecord = recordsByBookingId.get(b.bookingId);
        let effectiveStatus = b.status;
        if (matchingRecord) {
          if (matchingRecord.status === 'COMPLETED') {
            effectiveStatus = 'COMPLETED';
          } else if (matchingRecord.status === 'IN_PROGRESS' && b.status === 'CONFIRMED') {
            effectiveStatus = 'IN_PROGRESS';
          }
        }
        return {
          ...b,
          status: effectiveStatus,
          serviceRecord: matchingRecord,
        };
      });

      setBookings(updatedBookings);
    } catch (err) {
      setError(err.message || 'Failed to retrieve your bookings');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, []);

  const handleCancel = async (bookingId) => {
    try {
      await api.put(`/bookings/${bookingId}/cancel`);
      setCancellingId(null);
      fetchBookings();
    } catch (err) {
      setError(err.message || 'Failed to cancel booking');
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'CONFIRMED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">Confirmed</span>;
      case 'IN_PROGRESS':
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-500/10 text-blue-400 border border-blue-500/20">In Progress</span>;
      case 'COMPLETED':
        return (
          <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 flex items-center gap-1.5">
            <FaCheck className="text-[10px]" /> Service Completed
          </span>
        );
      case 'CANCELLED':
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-700/50 text-slate-400 border border-slate-600">Cancelled</span>;
      default:
        return <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">{status}</span>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-surface-border">
        <div>
          <h2 className="text-2xl font-bold text-white tracking-tight flex items-center gap-2">
            <FaCalendarAlt className="text-accent" /> My Bookings
          </h2>
          <p className="text-slate-400 text-sm mt-1">
            Review your scheduled workshop appointments and live service progress.
          </p>
        </div>
        <button
          onClick={() => navigate('/book-slot')}
          className="btn-primary flex items-center gap-2 px-4 py-2.5 text-sm cursor-pointer self-start sm:self-auto"
        >
          <FaPlus /> Book New Service
        </button>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/40 text-red-400 p-4 rounded-xl text-sm">
          {error}
        </div>
      )}

      {loading ? (
        <div className="py-20 flex flex-col items-center justify-center text-slate-400 gap-3">
          <FaSpinner className="animate-spin text-3xl text-accent" />
          <p className="text-sm">Loading your service appointments...</p>
        </div>
      ) : bookings.length === 0 ? (
        <div className="glass-card p-12 text-center rounded-2xl border border-surface-border flex flex-col items-center">
          <div className="w-20 h-20 bg-slate-800/60 rounded-full flex items-center justify-center text-slate-500 text-4xl mb-4 border border-surface-border">
            <FaCalendarAlt />
          </div>
          <h3 className="text-xl font-bold text-white mb-2">No Active Bookings</h3>
          <p className="text-slate-400 max-w-md text-sm mb-6">
            You do not have any scheduled appointments. Select a service and reserved bay to schedule maintenance.
          </p>
          <button
            onClick={() => navigate('/book-slot')}
            className="btn-primary flex items-center gap-2 px-6 py-3 cursor-pointer"
          >
            <FaPlus /> Schedule an Appointment
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {bookings.map((b) => {
            const isFinished = b.status === 'COMPLETED';
            const isCancellable = (b.status === 'CONFIRMED' || b.status === 'PENDING') && !isFinished;

            return (
              <div
                key={b.bookingId}
                className="glass-card p-6 rounded-xl border border-surface-border hover:border-accent/40 transition-all flex flex-col md:flex-row md:items-center justify-between gap-6"
              >
                <div className="space-y-3 flex-grow">
                  <div className="flex flex-wrap items-center gap-3">
                    <span className="text-xs font-mono font-bold px-2 py-0.5 bg-slate-800 text-accent rounded border border-surface-border">
                      #BK-{b.bookingId}
                    </span>
                    {getStatusBadge(b.status)}
                    <span className="text-xs text-slate-400">
                      Booked on {new Date(b.createdAt).toLocaleDateString()}
                    </span>
                  </div>

                  <h3 className="text-lg font-bold text-white tracking-tight">
                    {b.serviceType}
                  </h3>

                  <div className="flex flex-wrap items-center gap-y-2 gap-x-6 text-xs text-slate-300">
                    <span className="flex items-center gap-1.5 text-slate-400">
                      <FaCar className="text-accent" />
                      <strong className="text-white">{b.vehicleMakeModel || 'Vehicle'}</strong> ({b.vehicleRegistration})
                    </span>
                    <span className="flex items-center gap-1.5 text-slate-400">
                      <FaCalendarAlt className="text-accent" />
                      <strong className="text-white">{b.slotDate}</strong>
                    </span>
                    <span className="flex items-center gap-1.5 text-slate-400">
                      <FaClock className="text-accent" />
                      <strong className="text-white">{b.startTime} - {b.endTime}</strong>
                    </span>
                    <span className="flex items-center gap-1.5 text-slate-400">
                      <FaTools className="text-accent" />
                      <strong className="text-white">{b.bayNumber || 'Assigned Bay'}</strong> ({b.bayType || 'General'})
                    </span>
                  </div>

                  {b.notes && (
                    <p className="text-xs text-slate-400 italic bg-slate-950/50 p-2.5 rounded border border-surface-border/40">
                      Customer Request: "{b.notes}"
                    </p>
                  )}

                  {b.serviceRecord?.remarks && (
                    <div className="text-xs text-emerald-300/90 bg-emerald-950/40 p-3 rounded-lg border border-emerald-500/30 space-y-1">
                      <div className="flex items-center gap-2 font-semibold text-emerald-400">
                        <FaCheckCircle /> Certified Technician Inspection & Findings:
                      </div>
                      <p className="text-slate-300">{b.serviceRecord.remarks}</p>
                      {b.serviceRecord.technicianName && (
                        <p className="text-[11px] text-slate-400 pt-1">
                          Serviced by: <span className="text-white font-medium">{b.serviceRecord.technicianName}</span>
                        </p>
                      )}
                    </div>
                  )}
                </div>

                {/* Actions: ONLY show Cancel for active uncompleted bookings */}
                <div className="flex-shrink-0 flex items-center gap-2">
                  {isFinished ? (
                    <div className="flex flex-wrap items-center gap-2">
                      <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold">
                        <FaCheckCircle className="text-sm" /> Service Finished
                      </div>
                      <button
                        onClick={() => navigate('/dashboard/my-billings')}
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary/20 hover:bg-primary/30 text-accent border border-accent/20 text-xs font-semibold transition-colors cursor-pointer"
                      >
                        <FaFileInvoiceDollar /> View Invoice
                      </button>
                    </div>
                  ) : isCancellable ? (
                    cancellingId === b.bookingId ? (
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => setCancellingId(null)}
                          className="px-3 py-1.5 rounded text-xs text-slate-300 hover:text-white bg-slate-800 cursor-pointer"
                        >
                          Keep
                        </button>
                        <button
                          onClick={() => handleCancel(b.bookingId)}
                          className="px-3 py-1.5 rounded text-xs text-white bg-red-600 hover:bg-red-700 font-medium cursor-pointer"
                        >
                          Confirm Cancel
                        </button>
                      </div>
                    ) : (
                      <button
                        onClick={() => setCancellingId(b.bookingId)}
                        className="px-3 py-1.5 rounded-lg text-xs font-medium text-red-400 hover:text-red-300 bg-red-500/10 hover:bg-red-500/20 border border-red-500/20 transition-colors cursor-pointer"
                      >
                        Cancel Booking
                      </button>
                    )
                  ) : null}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default CustomerBookings;
