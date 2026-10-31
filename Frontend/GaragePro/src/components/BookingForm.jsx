import { useState, useEffect } from 'react';
import {
  FaCalendarAlt,
  FaCar,
  FaCheckCircle,
  FaSpinner,
  FaTools,
  FaPlus,
  FaExclamationTriangle,
  FaClock,
} from 'react-icons/fa';
import { useAuth } from '../context/AuthContext';
import { api } from '../utils/api';
import { useNavigate, useLocation } from 'react-router-dom';

const SUPPORTED_SERVICES = [
  { name: 'General Maintenance', price: '$89.99', duration: '60 mins' },
  { name: 'Oil & Fluid Service', price: '$49.99', duration: '30 mins' },
  { name: 'Brake Services', price: '$129.99', duration: '90 mins' },
  { name: 'Electrical Diagnostics', price: '$79.99', duration: '45 mins' },
  { name: 'A/C & Heating', price: '$99.99', duration: '60 mins' },
  { name: 'Engine Repair', price: '$199.99', duration: '120 mins' },
];

const BookingForm = ({ onSuccess, initialService }) => {
  const { isAuthenticated, user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Selected state passed from Explore Services page (if any)
  const preSelectedService =
    initialService || location.state?.selectedService || 'General Maintenance';

  const [serviceType, setServiceType] = useState(preSelectedService);
  const [vehicles, setVehicles] = useState([]);
  const [selectedVehicleId, setSelectedVehicleId] = useState('');
  const [selectedDate, setSelectedDate] = useState(() => {
    const today = new Date();
    return today.toISOString().split('T')[0];
  });
  const [slots, setSlots] = useState([]);
  const [selectedSlotId, setSelectedSlotId] = useState(null);
  const [notes, setNotes] = useState('');

  const [loadingVehicles, setLoadingVehicles] = useState(true);
  const [loadingSlots, setLoadingSlots] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [confirmedBooking, setConfirmedBooking] = useState(null);

  // Quick inline add-vehicle modal state
  const [showVehicleModal, setShowVehicleModal] = useState(false);
  const [newVehicle, setNewVehicle] = useState({
    registrationNumber: '',
    make: '',
    model: '',
    year: new Date().getFullYear(),
    fuelType: 'Petrol',
    vehicleType: 'Sedan',
  });
  const [vehicleModalLoading, setVehicleModalLoading] = useState(false);
  const [vehicleModalError, setVehicleModalError] = useState('');

  // 1. Fetch Vehicles
  const fetchVehicles = async () => {
    if (!isAuthenticated) return;
    setLoadingVehicles(true);
    try {
      const data = await api.get('/vehicles');
      const list = Array.isArray(data) ? data : [];
      setVehicles(list);
      if (list.length > 0 && !selectedVehicleId) {
        setSelectedVehicleId(list[0].vehicleId);
      }
    } catch (err) {
      setError(err.message || 'Failed to fetch registered vehicles');
    } finally {
      setLoadingVehicles(false);
    }
  };

  // 2. Fetch Slots for selected date
  const fetchSlots = async (date) => {
    if (!isAuthenticated) return;
    setLoadingSlots(true);
    setSelectedSlotId(null);
    try {
      const data = await api.get(`/bookings/slots?date=${date}`);
      setSlots(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to fetch slot availability');
    } finally {
      setLoadingSlots(false);
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      fetchVehicles();
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (isAuthenticated && selectedDate) {
      fetchSlots(selectedDate);
    }
  }, [isAuthenticated, selectedDate]);

  // Handle Quick Add Vehicle
  const handleQuickAddVehicle = async (e) => {
    e.preventDefault();
    setVehicleModalLoading(true);
    setVehicleModalError('');
    try {
      const saved = await api.post('/vehicles', newVehicle);
      await fetchVehicles();
      setSelectedVehicleId(saved.vehicleId);
      setShowVehicleModal(false);
      setNewVehicle({
        registrationNumber: '',
        make: '',
        model: '',
        year: new Date().getFullYear(),
        fuelType: 'Petrol',
        vehicleType: 'Sedan',
      });
    } catch (err) {
      setVehicleModalError(err.message || 'Failed to register vehicle');
    } finally {
      setVehicleModalLoading(false);
    }
  };

  // Handle Booking Submit
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!selectedVehicleId) {
      setError('Please select or register a vehicle first.');
      return;
    }
    if (!selectedSlotId) {
      setError('Please select an available service time slot.');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        vehicleId: Number(selectedVehicleId),
        slotId: Number(selectedSlotId),
        serviceType,
        notes,
      };

      const booking = await api.post('/bookings', payload);
      setConfirmedBooking(booking);
      if (onSuccess) {
        // Callback if inside modal
      }
    } catch (err) {
      setError(err.message || 'Failed to create booking. Please try another slot.');
      // Refresh slots in case of concurrency clash
      fetchSlots(selectedDate);
    } finally {
      setSubmitting(false);
    }
  };

  if (!isAuthenticated) {
    return (
      <div className="p-8 text-center text-slate-400 space-y-4">
        <FaCar className="text-4xl text-accent mx-auto mb-2" />
        <h3 className="text-xl font-bold text-white">Sign In Required</h3>
        <p className="text-sm">Please log in to your customer account to view vehicle slots and schedule service appointments.</p>
      </div>
    );
  }

  // Success Confirmation Screen
  if (confirmedBooking) {
    return (
      <div className="p-6 md:p-10 text-center motion-page-enter space-y-6">
        <div className="w-20 h-20 bg-emerald-500/20 text-emerald-400 rounded-full flex items-center justify-center mx-auto status-indicator-breathe border border-emerald-500/30">
          <FaCheckCircle className="text-4xl" />
        </div>

        <div>
          <span className="text-xs font-mono font-bold px-3 py-1 bg-slate-800 text-accent rounded-full border border-surface-border inline-block mb-2">
            Booking ID: #BK-{confirmedBooking.bookingId}
          </span>
          <h3 className="text-2xl font-bold text-white">Appointment Confirmed!</h3>
          <p className="text-slate-400 text-sm max-w-md mx-auto mt-1">
            Your vehicle has been scheduled for service. A certified technician will be assigned upon bay arrival.
          </p>
        </div>

        {/* Booking Details Card */}
        <div className="glass-card max-w-lg mx-auto p-5 rounded-xl border border-surface-border text-left text-xs space-y-2.5 bg-slate-950/60">
          <div className="flex justify-between py-1 border-b border-surface-border/40">
            <span className="text-slate-400">Service:</span>
            <span className="text-white font-bold">{confirmedBooking.serviceType}</span>
          </div>
          <div className="flex justify-between py-1 border-b border-surface-border/40">
            <span className="text-slate-400">Vehicle:</span>
            <span className="text-white font-medium">
              {confirmedBooking.vehicleMakeModel} ({confirmedBooking.vehicleRegistration})
            </span>
          </div>
          <div className="flex justify-between py-1 border-b border-surface-border/40">
            <span className="text-slate-400">Date & Bay:</span>
            <span className="text-accent font-medium">
              {confirmedBooking.slotDate} — {confirmedBooking.bayNumber || 'Assigned Bay'}
            </span>
          </div>
          <div className="flex justify-between py-1 border-b border-surface-border/40">
            <span className="text-slate-400">Time Window:</span>
            <span className="text-emerald-400 font-semibold font-mono">
              {confirmedBooking.startTime} - {confirmedBooking.endTime}
            </span>
          </div>
          <div className="flex justify-between py-1">
            <span className="text-slate-400">Status:</span>
            <span className="text-emerald-400 font-bold">{confirmedBooking.status}</span>
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-center gap-4 pt-2">
          <button
            onClick={() => navigate('/dashboard/my-bookings')}
            className="btn-primary px-6 py-3 text-sm cursor-pointer"
          >
            View in My Bookings
          </button>
          <button
            onClick={() => {
              setConfirmedBooking(null);
              setSelectedSlotId(null);
              fetchSlots(selectedDate);
            }}
            className="px-6 py-3 rounded-lg text-sm text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 cursor-pointer transition-colors"
          >
            Book Another Slot
          </button>
        </div>
      </div>
    );
  }

  // Group slots by Bay Number
  const slotsByBay = slots.reduce((acc, slot) => {
    const bayName = slot.bayNumber || 'General Bay';
    if (!acc[bayName]) {
      acc[bayName] = [];
    }
    acc[bayName].push(slot);
    return acc;
  }, {});

  return (
    <form onSubmit={handleSubmit} className="space-y-8">
      {error && (
        <div className="bg-red-500/10 border border-red-500/40 text-red-400 p-4 rounded-xl text-xs flex items-center gap-2">
          <FaExclamationTriangle className="text-base flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Step 1: Service Type Selection */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-sm font-bold text-white flex items-center gap-2">
            <span className="w-5 h-5 rounded-full bg-primary text-white text-xs flex items-center justify-center font-bold">1</span>
            Select Precision Service
          </label>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {SUPPORTED_SERVICES.map((s) => (
            <div
              key={s.name}
              onClick={() => setServiceType(s.name)}
              className={`p-3.5 rounded-xl border cursor-pointer transition-all ${
                serviceType === s.name
                  ? 'bg-primary/20 border-accent shadow-[0_0_12px_rgba(56,189,248,0.2)]'
                  : 'bg-slate-900 border-surface-border hover:border-slate-600'
              }`}
            >
              <div className="flex justify-between items-start mb-1">
                <span className="font-semibold text-xs text-white leading-tight">{s.name}</span>
                <span className="text-xs font-bold text-accent font-mono">{s.price}</span>
              </div>
              <span className="text-[11px] text-slate-400 flex items-center gap-1">
                <FaClock className="text-[10px]" /> ~{s.duration}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Step 2: Vehicle Selection */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-sm font-bold text-white flex items-center gap-2">
            <span className="w-5 h-5 rounded-full bg-primary text-white text-xs flex items-center justify-center font-bold">2</span>
            Select Registered Vehicle
          </label>
          <button
            type="button"
            onClick={() => setShowVehicleModal(true)}
            className="text-xs text-accent hover:underline flex items-center gap-1 cursor-pointer font-medium"
          >
            <FaPlus className="text-[10px]" /> Add New Vehicle
          </button>
        </div>

        {loadingVehicles ? (
          <div className="p-4 bg-slate-900 rounded-lg text-xs text-slate-400 flex items-center gap-2">
            <FaSpinner className="animate-spin" /> Loading your vehicles...
          </div>
        ) : vehicles.length === 0 ? (
          <div className="p-6 bg-slate-900/80 rounded-xl border border-dashed border-surface-border text-center space-y-3">
            <FaCar className="text-3xl text-slate-500 mx-auto" />
            <p className="text-xs text-slate-400">
              You haven't registered any vehicles yet. Please add a vehicle to continue.
            </p>
            <button
              type="button"
              onClick={() => setShowVehicleModal(true)}
              className="btn-primary px-4 py-2 text-xs flex items-center gap-1.5 mx-auto cursor-pointer"
            >
              <FaPlus /> Register Vehicle Now
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {vehicles.map((v) => (
              <div
                key={v.vehicleId}
                onClick={() => setSelectedVehicleId(v.vehicleId)}
                className={`p-3.5 rounded-xl border cursor-pointer transition-all ${
                  selectedVehicleId === v.vehicleId
                    ? 'bg-primary/20 border-accent shadow-[0_0_12px_rgba(56,189,248,0.2)]'
                    : 'bg-slate-900 border-surface-border hover:border-slate-600'
                }`}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="font-bold text-xs text-white">{v.make} {v.model}</span>
                  <span className="text-[10px] font-mono font-semibold px-2 py-0.5 bg-slate-800 text-slate-300 rounded">
                    {v.registrationNumber}
                  </span>
                </div>
                <div className="text-[11px] text-slate-400 flex items-center justify-between">
                  <span>{v.year} • {v.vehicleType}</span>
                  <span className="text-accent">{v.fuelType}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Step 3: Date & Service Bay Slots */}
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <label className="text-sm font-bold text-white flex items-center gap-2">
            <span className="w-5 h-5 rounded-full bg-primary text-white text-xs flex items-center justify-center font-bold">3</span>
            Select Date & Service Bay Slot
          </label>
          <div className="flex items-center gap-2">
            <FaCalendarAlt className="text-accent text-sm" />
            <input
              type="date"
              min={new Date().toISOString().split('T')[0]}
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="bg-slate-900 border border-surface-border rounded-lg px-3 py-1.5 text-xs text-white focus:outline-none focus:border-accent"
            />
          </div>
        </div>

        {/* Availability Legend */}
        <div className="flex flex-wrap items-center gap-4 p-3 bg-slate-950 rounded-lg border border-surface-border text-[11px]">
          <span className="text-slate-400 font-semibold uppercase tracking-wider text-[10px]">Legend:</span>
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded bg-emerald-500/30 border border-emerald-500"></span>
            <span className="text-emerald-400 font-medium">Green: Available Slot</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded bg-amber-500/40 border border-amber-400"></span>
            <span className="text-amber-300 font-medium">Yellow: Selected Slot</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded bg-slate-800 border border-slate-700"></span>
            <span className="text-slate-500 font-medium">Gray: Unavailable / Booked</span>
          </div>
        </div>

        {/* Slot Grid grouped by Service Bay */}
        {loadingSlots ? (
          <div className="p-8 bg-slate-900 rounded-xl text-center text-xs text-slate-400 flex items-center justify-center gap-2">
            <FaSpinner className="animate-spin text-accent" /> Checking bay telemetry for {selectedDate}...
          </div>
        ) : Object.keys(slotsByBay).length === 0 ? (
          <div className="p-8 bg-slate-900 rounded-xl text-center text-xs text-slate-500">
            No service bays available on this date.
          </div>
        ) : (
          <div className="space-y-4">
            {Object.entries(slotsByBay).map(([bayName, baySlots]) => (
              <div key={bayName} className="bg-slate-900 p-4 rounded-xl border border-surface-border space-y-3">
                <div className="flex items-center justify-between pb-2 border-b border-surface-border/50">
                  <div className="flex items-center gap-2">
                    <FaTools className="text-accent text-xs" />
                    <span className="font-bold text-xs text-white">{bayName}</span>
                    <span className="text-[10px] text-slate-400">({baySlots[0]?.bayType || 'General'})</span>
                  </div>
                  <span className="text-[10px] text-slate-400">
                    {baySlots.filter((s) => s.status === 'AVAILABLE').length} open slots
                  </span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-2">
                  {baySlots.map((slot) => {
                    const isAvailable = slot.status === 'AVAILABLE';
                    const isSelected = selectedSlotId === slot.slotId;

                    return (
                      <button
                        key={slot.slotId}
                        type="button"
                        disabled={!isAvailable}
                        onClick={() => setSelectedSlotId(slot.slotId)}
                        title={
                          isAvailable
                            ? `Click to select slot ${slot.startTime} - ${slot.endTime}`
                            : `Slot ${slot.startTime} is ${slot.status}`
                        }
                        className={`p-2.5 rounded-lg border text-xs font-mono transition-all flex flex-col items-center justify-center ${
                          isSelected
                            ? 'bg-amber-500/25 text-amber-300 border-amber-400 ring-2 ring-amber-400/50 shadow-md font-bold'
                            : isAvailable
                            ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/40 hover:bg-emerald-500/25 hover:border-emerald-400 cursor-pointer'
                            : 'bg-slate-800/40 text-slate-500 border-slate-700/40 cursor-not-allowed opacity-60'
                        }`}
                      >
                        <span className="font-semibold">{slot.startTime}</span>
                        <span className="text-[9px] uppercase tracking-wider mt-0.5 opacity-80">
                          {isSelected ? 'SELECTED' : slot.status}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Step 4: Notes and Submit */}
      <div className="space-y-4">
        <div>
          <label className="text-xs font-semibold text-slate-300 block mb-1">
            Special Requests or Service Symptoms (Optional)
          </label>
          <input
            type="text"
            placeholder="e.g. Squeaking noise under deceleration, check tire pressure"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            className="input-field text-xs"
          />
        </div>

        <button
          type="submit"
          disabled={submitting || !selectedSlotId || !selectedVehicleId}
          className="btn-primary w-full py-4 text-base font-bold flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed transition-all"
        >
          {submitting ? (
            <>
              <FaSpinner className="animate-spin" /> Reserving Service Bay...
            </>
          ) : (
            'Confirm Service Booking'
          )}
        </button>
      </div>

      {/* Quick Add Vehicle Modal */}
      {showVehicleModal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-[200] flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-surface-border rounded-xl w-full max-w-md p-6 relative shadow-2xl animate-in fade-in">
            <button
              type="button"
              onClick={() => setShowVehicleModal(false)}
              className="absolute top-4 right-4 text-slate-400 hover:text-white"
            >
              &times;
            </button>
            <h4 className="text-base font-bold text-white mb-2 flex items-center gap-2">
              <FaCar className="text-accent" /> Register Vehicle
            </h4>
            <p className="text-xs text-slate-400 mb-4">
              Add your vehicle details to continue booking immediately.
            </p>

            {vehicleModalError && (
              <div className="bg-red-500/10 border border-red-500/40 text-red-400 p-2.5 rounded text-xs mb-3">
                {vehicleModalError}
              </div>
            )}

            <div className="space-y-3">
              <div>
                <label className="text-xs text-slate-300 block mb-1">Registration Number *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. KA-05-MN-9988"
                  value={newVehicle.registrationNumber}
                  onChange={(e) => setNewVehicle({ ...newVehicle, registrationNumber: e.target.value.toUpperCase() })}
                  className="input-field text-xs font-mono"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-slate-300 block mb-1">Make *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Honda"
                    value={newVehicle.make}
                    onChange={(e) => setNewVehicle({ ...newVehicle, make: e.target.value })}
                    className="input-field text-xs"
                  />
                </div>
                <div>
                  <label className="text-xs text-slate-300 block mb-1">Model *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Civic"
                    value={newVehicle.model}
                    onChange={(e) => setNewVehicle({ ...newVehicle, model: e.target.value })}
                    className="input-field text-xs"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-slate-300 block mb-1">Year</label>
                  <input
                    type="number"
                    min="1900"
                    max={new Date().getFullYear() + 1}
                    value={newVehicle.year}
                    onChange={(e) => setNewVehicle({ ...newVehicle, year: parseInt(e.target.value, 10) })}
                    className="input-field text-xs"
                  />
                </div>
                <div>
                  <label className="text-xs text-slate-300 block mb-1">Fuel</label>
                  <select
                    value={newVehicle.fuelType}
                    onChange={(e) => setNewVehicle({ ...newVehicle, fuelType: e.target.value })}
                    className="input-field text-xs"
                  >
                    <option value="Petrol">Petrol</option>
                    <option value="Diesel">Diesel</option>
                    <option value="Electric">Electric</option>
                    <option value="Hybrid">Hybrid</option>
                  </select>
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-surface-border">
                <button
                  type="button"
                  onClick={() => setShowVehicleModal(false)}
                  className="px-3 py-1.5 rounded text-xs text-slate-400 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleQuickAddVehicle}
                  disabled={vehicleModalLoading}
                  className="btn-primary px-4 py-1.5 text-xs flex items-center gap-1.5 cursor-pointer"
                >
                  {vehicleModalLoading && <FaSpinner className="animate-spin" />}
                  Register & Select
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </form>
  );
};

export default BookingForm;
