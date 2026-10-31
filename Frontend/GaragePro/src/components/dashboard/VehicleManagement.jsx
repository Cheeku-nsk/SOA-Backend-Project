import { useState, useEffect } from 'react';
import { FaCar, FaPlus, FaTrash, FaEdit, FaSpinner, FaGasPump, FaCalendarAlt, FaExclamationTriangle } from 'react-icons/fa';
import { api } from '../../utils/api';

const VehicleManagement = () => {
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [selectedVehicleId, setSelectedVehicleId] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [deleteConfirmId, setDeleteConfirmId] = useState(null);

  const [formData, setFormData] = useState({
    registrationNumber: '',
    make: '',
    model: '',
    year: new Date().getFullYear(),
    fuelType: 'Petrol',
    vehicleType: 'Sedan',
  });

  const fetchVehicles = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await api.get('/vehicles');
      setVehicles(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Failed to load vehicles');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchVehicles();
  }, []);

  const openAddModal = () => {
    setIsEditing(false);
    setSelectedVehicleId(null);
    setFormData({
      registrationNumber: '',
      make: '',
      model: '',
      year: new Date().getFullYear(),
      fuelType: 'Petrol',
      vehicleType: 'Sedan',
    });
    setError('');
    setShowModal(true);
  };

  const openEditModal = (vehicle) => {
    setIsEditing(true);
    setSelectedVehicleId(vehicle.vehicleId);
    setFormData({
      registrationNumber: vehicle.registrationNumber,
      make: vehicle.make,
      model: vehicle.model,
      year: vehicle.year,
      fuelType: vehicle.fuelType || 'Petrol',
      vehicleType: vehicle.vehicleType || 'Sedan',
    });
    setError('');
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError('');

    // Client-side validations
    const currentYear = new Date().getFullYear();
    if (formData.year < 1900 || formData.year > currentYear + 1) {
      setError(`Year must be between 1900 and ${currentYear + 1}`);
      setSubmitting(false);
      return;
    }

    const regRegex = /^[A-Za-z0-9 -]{4,15}$/;
    if (!regRegex.test(formData.registrationNumber.trim())) {
      setError('Registration number must be 4 to 15 alphanumeric characters');
      setSubmitting(false);
      return;
    }

    try {
      if (isEditing) {
        await api.put(`/vehicles/${selectedVehicleId}`, formData);
      } else {
        await api.post('/vehicles', formData);
      }
      setShowModal(false);
      fetchVehicles();
    } catch (err) {
      setError(err.message || 'Failed to save vehicle');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (vehicleId) => {
    setError('');
    try {
      await api.delete(`/vehicles/${vehicleId}`);
      setDeleteConfirmId(null);
      fetchVehicles();
    } catch (err) {
      setError(err.message || 'Failed to delete vehicle. Active bookings may exist.');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header with Title and Add Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-surface-border">
        <div>
          <h2 className="text-2xl font-bold text-white tracking-tight flex items-center gap-2">
            <FaCar className="text-accent" /> My Vehicles
          </h2>
          <p className="text-slate-400 text-sm mt-1">
            Manage your registered automobiles for effortless service scheduling.
          </p>
        </div>
        <button
          onClick={openAddModal}
          className="btn-primary flex items-center gap-2 px-4 py-2.5 text-sm cursor-pointer self-start sm:self-auto"
        >
          <FaPlus /> Add New Vehicle
        </button>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/40 text-red-400 p-4 rounded-xl text-sm flex items-start gap-3">
          <FaExclamationTriangle className="text-lg flex-shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {/* Loading State */}
      {loading ? (
        <div className="py-20 flex flex-col items-center justify-center text-slate-400 gap-3">
          <FaSpinner className="animate-spin text-3xl text-accent" />
          <p className="text-sm">Loading your registered vehicles...</p>
        </div>
      ) : vehicles.length === 0 ? (
        /* Empty State */
        <div className="glass-card p-12 text-center rounded-2xl border border-surface-border flex flex-col items-center">
          <div className="w-20 h-20 bg-slate-800/60 rounded-full flex items-center justify-center text-slate-500 text-4xl mb-4 border border-surface-border">
            <FaCar />
          </div>
          <h3 className="text-xl font-bold text-white mb-2">No Vehicles Registered</h3>
          <p className="text-slate-400 max-w-md text-sm mb-6">
            You haven't added any vehicles yet. Register your vehicle now to quickly select it when booking precision maintenance slots.
          </p>
          <button
            onClick={openAddModal}
            className="btn-primary flex items-center gap-2 px-6 py-3 cursor-pointer"
          >
            <FaPlus /> Register Your First Vehicle
          </button>
        </div>
      ) : (
        /* Vehicle Cards Grid */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {vehicles.map((v) => (
            <div
              key={v.vehicleId}
              className="glass-card p-6 rounded-xl border border-surface-border hover:border-accent/40 transition-all flex flex-col justify-between group"
            >
              <div>
                <div className="flex items-start justify-between mb-4">
                  <div className="w-12 h-12 rounded-lg bg-primary/20 text-accent flex items-center justify-center text-2xl border border-accent/20">
                    <FaCar />
                  </div>
                  <span className="text-xs font-mono font-bold px-2.5 py-1 bg-slate-800 text-slate-200 rounded border border-surface-border">
                    {v.registrationNumber}
                  </span>
                </div>

                <h3 className="text-lg font-bold text-white tracking-tight">
                  {v.make} {v.model}
                </h3>
                <p className="text-xs text-slate-400 mb-4">{v.vehicleType || 'Sedan'}</p>

                <div className="space-y-2 pt-3 border-t border-surface-border/50 text-xs text-slate-300">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-500 flex items-center gap-1.5">
                      <FaCalendarAlt /> Year
                    </span>
                    <span className="font-semibold text-white">{v.year}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-slate-500 flex items-center gap-1.5">
                      <FaGasPump /> Fuel Type
                    </span>
                    <span className="font-semibold text-accent">{v.fuelType || 'Petrol'}</span>
                  </div>
                </div>
              </div>

              {/* Actions */}
              <div className="flex items-center justify-end gap-2 mt-6 pt-4 border-t border-surface-border/50">
                <button
                  onClick={() => openEditModal(v)}
                  className="px-3 py-1.5 rounded-lg text-xs font-medium text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 flex items-center gap-1.5 cursor-pointer transition-colors"
                >
                  <FaEdit className="text-accent" /> Edit
                </button>
                <button
                  onClick={() => setDeleteConfirmId(v.vehicleId)}
                  className="px-3 py-1.5 rounded-lg text-xs font-medium text-red-400 hover:text-red-300 bg-red-500/10 hover:bg-red-500/20 flex items-center gap-1.5 cursor-pointer transition-colors"
                >
                  <FaTrash /> Remove
                </button>
              </div>

              {/* Inline Delete Confirmation */}
              {deleteConfirmId === v.vehicleId && (
                <div className="mt-3 p-3 bg-red-950/60 border border-red-500/40 rounded-lg text-xs">
                  <p className="text-red-200 mb-2">Are you sure you want to remove this vehicle?</p>
                  <div className="flex items-center justify-end gap-2">
                    <button
                      onClick={() => setDeleteConfirmId(null)}
                      className="px-2.5 py-1 text-slate-300 hover:text-white bg-slate-800 rounded cursor-pointer"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={() => handleDelete(v.vehicleId)}
                      className="px-2.5 py-1 text-white bg-red-600 hover:bg-red-700 rounded cursor-pointer font-medium"
                    >
                      Confirm Delete
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Add / Edit Vehicle Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-[150] flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-surface-border rounded-2xl w-full max-w-lg p-6 md:p-8 relative shadow-2xl animate-in fade-in zoom-in-95">
            <button
              onClick={() => setShowModal(false)}
              className="absolute top-4 right-4 text-slate-400 hover:text-white p-2"
              aria-label="Close"
            >
              &times;
            </button>

            <h3 className="text-xl font-bold text-white mb-2 flex items-center gap-2">
              <FaCar className="text-accent" />
              {isEditing ? 'Edit Vehicle Details' : 'Register New Vehicle'}
            </h3>
            <p className="text-xs text-slate-400 mb-6">
              Enter the required registration and specification details for your vehicle.
            </p>

            {error && (
              <div className="bg-red-500/10 border border-red-500/40 text-red-400 p-3 rounded-lg text-xs mb-4">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Registration Number *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. KA-01-AB-1234"
                  value={formData.registrationNumber}
                  onChange={(e) => setFormData({ ...formData, registrationNumber: e.target.value.toUpperCase() })}
                  className="input-field font-mono"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Make *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Toyota"
                    value={formData.make}
                    onChange={(e) => setFormData({ ...formData, make: e.target.value })}
                    className="input-field"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Model *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Camry"
                    value={formData.model}
                    onChange={(e) => setFormData({ ...formData, model: e.target.value })}
                    className="input-field"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Year *</label>
                  <input
                    type="number"
                    required
                    min="1900"
                    max={new Date().getFullYear() + 1}
                    value={formData.year}
                    onChange={(e) => setFormData({ ...formData, year: parseInt(e.target.value, 10) })}
                    className="input-field"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Fuel Type</label>
                  <select
                    value={formData.fuelType}
                    onChange={(e) => setFormData({ ...formData, fuelType: e.target.value })}
                    className="input-field"
                  >
                    <option value="Petrol">Petrol</option>
                    <option value="Diesel">Diesel</option>
                    <option value="Electric">Electric</option>
                    <option value="Hybrid">Hybrid</option>
                    <option value="CNG">CNG</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Body Type</label>
                  <select
                    value={formData.vehicleType}
                    onChange={(e) => setFormData({ ...formData, vehicleType: e.target.value })}
                    className="input-field"
                  >
                    <option value="Sedan">Sedan</option>
                    <option value="SUV">SUV</option>
                    <option value="Hatchback">Hatchback</option>
                    <option value="Coupe">Coupe</option>
                    <option value="Truck">Truck</option>
                    <option value="Van">Van</option>
                  </select>
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-4 border-t border-surface-border">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2.5 rounded-lg text-sm text-slate-300 hover:text-white bg-slate-800 cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="btn-primary px-6 py-2.5 text-sm flex items-center gap-2 cursor-pointer disabled:opacity-60"
                >
                  {submitting && <FaSpinner className="animate-spin" />}
                  {isEditing ? 'Save Changes' : 'Register Vehicle'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default VehicleManagement;
