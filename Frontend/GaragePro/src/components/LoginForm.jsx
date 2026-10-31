import { useState } from 'react';
import { FaEnvelope, FaLock, FaArrowRight, FaSpinner, FaUserCheck, FaTools, FaShieldAlt } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { API_BASE_URL } from '../utils/api';

const LoginForm = () => {
  const [selectedRole, setSelectedRole] = useState('CUSTOMER');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleRoleChange = (role) => {
    setSelectedRole(role);
    setEmail('');
    setPassword('');
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsSubmitting(true);

    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email,
          password,
          role: selectedRole,
        }),
      });

      let data = null;
      try {
        data = await response.json();
      } catch (parseErr) {
        // response might not be JSON
      }

      if (!response.ok) {
        throw new Error(data?.message || data?.error || `Authentication failed (Status: ${response.status})`);
      }

      login({ id: data.userId, name: data.name, email: data.email, role: data.role, status: data.status }, data.token);
      setEmail('');
      setPassword('');
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Login failed. Please check your credentials.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="glass-panel p-8 md:p-10 w-full max-w-md mx-auto relative overflow-hidden">
      {/* Decorative Glow */}
      <div className="absolute -top-20 -right-20 w-48 h-48 bg-primary/20 blur-[60px] rounded-full pointer-events-none"></div>

      <div className="relative z-10">
        <div className="text-center mb-6">
          <h3 className="text-2xl font-bold text-white mb-2">Welcome Back</h3>
          <p className="text-slate-400 text-sm">Sign in to manage your precision vehicle services</p>
        </div>

        {/* Role Category Selector */}
        <div className="mb-6">
          <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
            Select Login Category
          </label>
          <div className="grid grid-cols-3 gap-2 bg-slate-950 p-1.5 rounded-lg border border-surface-border">
            <button
              type="button"
              onClick={() => handleRoleChange('CUSTOMER')}
              className={`flex flex-col items-center justify-center py-2 px-1 rounded-md text-xs font-medium transition-all cursor-pointer ${
                selectedRole === 'CUSTOMER'
                  ? 'bg-primary text-white shadow-sm'
                  : 'text-slate-400 hover:text-white hover:bg-slate-800'
              }`}
            >
              <FaUserCheck className="mb-1 text-sm" />
              Customer
            </button>
            <button
              type="button"
              onClick={() => handleRoleChange('TECHNICIAN')}
              className={`flex flex-col items-center justify-center py-2 px-1 rounded-md text-xs font-medium transition-all cursor-pointer ${
                selectedRole === 'TECHNICIAN'
                  ? 'bg-primary text-white shadow-sm'
                  : 'text-slate-400 hover:text-white hover:bg-slate-800'
              }`}
            >
              <FaTools className="mb-1 text-sm" />
              Technician
            </button>
            <button
              type="button"
              onClick={() => handleRoleChange('ADMIN')}
              className={`flex flex-col items-center justify-center py-2 px-1 rounded-md text-xs font-medium transition-all cursor-pointer ${
                selectedRole === 'ADMIN'
                  ? 'bg-primary text-white shadow-sm'
                  : 'text-slate-400 hover:text-white hover:bg-slate-800'
              }`}
            >
              <FaShieldAlt className="mb-1 text-sm" />
              Admin
            </button>
          </div>
        </div>

        {error && (
          <div className="bg-red-500/10 border border-red-500/50 text-red-400 px-4 py-2.5 rounded-lg mb-4 text-xs leading-relaxed animate-in fade-in">
            {error}
          </div>
        )}

        <form key={selectedRole} onSubmit={handleSubmit} autoComplete="off" className="space-y-4">
          <div>
            <label className="form-label">Email Address</label>
            <div className="relative">
              <FaEnvelope className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="off"
                className="input-field pl-11"
                placeholder={selectedRole === 'ADMIN' ? 'admin@precisionauto.com' : 'you@example.com'}
                required
              />
            </div>
          </div>

          <div>
            <label className="form-label">Password</label>
            <div className="relative">
              <FaLock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="new-password"
                className="input-field pl-11"
                placeholder="••••••••"
                required
              />
            </div>
          </div>

          <button
            type="submit"
            className="btn-primary w-full flex items-center justify-center gap-2 py-3.5 mt-2 cursor-pointer disabled:opacity-60"
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <FaSpinner className="animate-spin" />
            ) : (
              <>
                Sign In as {selectedRole.charAt(0) + selectedRole.slice(1).toLowerCase()} <FaArrowRight className="text-sm" />
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
};

export default LoginForm;
