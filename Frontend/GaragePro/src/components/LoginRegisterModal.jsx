import { useState, useEffect } from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import { FaTimes, FaSpinner, FaUserCheck, FaTools, FaShieldAlt } from 'react-icons/fa';
import { useAuth } from '../context/AuthContext';
import { API_BASE_URL } from '../utils/api';

const LoginRegisterModal = ({ open, onOpenChange, onSuccess }) => {
  const [isLogin, setIsLogin] = useState(true);
  const [selectedRole, setSelectedRole] = useState('CUSTOMER'); // 'CUSTOMER' | 'TECHNICIAN' | 'ADMIN'
  const [formData, setFormData] = useState({ name: '', email: '', password: '', confirmPassword: '' });
  const [error, setError] = useState('');
  const [infoMessage, setInfoMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();

  const resetForm = () => {
    setFormData({ name: '', email: '', password: '', confirmPassword: '' });
    setError('');
    setInfoMessage('');
  };

  // Reset all input fields whenever modal opens or closes
  useEffect(() => {
    resetForm();
  }, [open]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleRoleChange = (role) => {
    setSelectedRole(role);
    resetForm();
    if (role === 'ADMIN' && !isLogin) {
      setInfoMessage('Administrator accounts must be initialized by the system configuration and cannot be created publicly.');
    }
  };

  const handleModeSwitch = (toLogin) => {
    setIsLogin(toLogin);
    resetForm();
    if (!toLogin && selectedRole === 'ADMIN') {
      setSelectedRole('CUSTOMER');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setInfoMessage('');
    setLoading(true);

    if (!isLogin && selectedRole === 'ADMIN') {
      setError('Public registration as Administrator is not permitted.');
      setLoading(false);
      return;
    }

    if (!isLogin && formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      setLoading(false);
      return;
    }

    try {
      if (isLogin) {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            email: formData.email,
            password: formData.password,
            role: selectedRole,
          }),
        });

        let data = null;
        try {
          data = await response.json();
        } catch {
          data = null;
        }

        if (!response.ok) {
          throw new Error(data?.message || data?.error || 'Authentication failed');
        }

        login({ id: data.userId, name: data.name, email: data.email, role: data.role, status: data.status }, data.token);
        resetForm();
        onOpenChange(false);
        if (onSuccess) onSuccess();
      } else {
        // Registration
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: formData.name,
            email: formData.email,
            password: formData.password,
            role: selectedRole,
          }),
        });

        let data = null;
        try {
          data = await response.json();
        } catch {
          data = null;
        }

        if (!response.ok) {
          throw new Error(data?.message || data?.error || 'Registration failed');
        }

        if (selectedRole === 'TECHNICIAN') {
          setInfoMessage(data.message || 'Your technician registration request has been submitted. Please wait for administrator approval.');
          resetForm();
          setIsLogin(true);
        } else {
          // Customer auto-login
          login({ id: data.userId, name: data.name, email: data.email, role: data.role, status: data.status }, data.token);
          resetForm();
          onOpenChange(false);
          if (onSuccess) onSuccess();
        }
      }
    } catch (err) {
      setError(err.message || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/60 backdrop-blur-sm z-[100] radix-overlay-motion" />
        <Dialog.Content className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-slate-900 border border-surface-border rounded-xl p-6 md:p-8 w-[92vw] max-w-md z-[101] shadow-2xl radix-content-motion focus:outline-none max-h-[90vh] overflow-y-auto">
          <Dialog.Title className="text-2xl font-bold text-white mb-1">
            {isLogin ? 'Sign In to PrecisionAuto Care' : 'Create an Account'}
          </Dialog.Title>
          <Dialog.Description className="text-slate-400 text-sm mb-4">
            {isLogin
              ? 'Enter your credentials to access your console.'
              : 'Sign up to register vehicles and schedule service bays.'}
          </Dialog.Description>

          {/* Role Category Selector */}
          <div className="mb-5">
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

          {infoMessage && (
            <div className="bg-cyan-500/10 border border-cyan-500/50 text-cyan-300 px-4 py-2.5 rounded-lg mb-4 text-xs leading-relaxed animate-in fade-in">
              {infoMessage}
            </div>
          )}

          {/* Form keyed by role and mode to prevent stale browser autocomplete across categories */}
          <form key={`${selectedRole}-${isLogin ? 'login' : 'register'}`} onSubmit={handleSubmit} autoComplete="off" className="space-y-4">
            {!isLogin && (
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Full Name</label>
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleChange}
                  required={!isLogin}
                  disabled={!isLogin && selectedRole === 'ADMIN'}
                  autoComplete="off"
                  className="w-full bg-slate-800 border border-surface-border rounded-lg px-4 py-2.5 text-white text-sm focus:outline-none focus:border-accent transition-colors disabled:opacity-50"
                  placeholder="e.g. Alex Morgan"
                />
              </div>
            )}
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Email Address</label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                required
                disabled={!isLogin && selectedRole === 'ADMIN'}
                autoComplete="off"
                className="w-full bg-slate-800 border border-surface-border rounded-lg px-4 py-2.5 text-white text-sm focus:outline-none focus:border-accent transition-colors disabled:opacity-50"
                placeholder={selectedRole === 'ADMIN' ? 'admin@precisionauto.com' : 'you@example.com'}
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1">Password</label>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                required
                disabled={!isLogin && selectedRole === 'ADMIN'}
                autoComplete="new-password"
                className="w-full bg-slate-800 border border-surface-border rounded-lg px-4 py-2.5 text-white text-sm focus:outline-none focus:border-accent transition-colors disabled:opacity-50"
                placeholder="••••••••"
              />
            </div>
            {!isLogin && (
              <div>
                <label className="block text-xs font-medium text-slate-300 mb-1">Confirm Password</label>
                <input
                  type="password"
                  name="confirmPassword"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  required={!isLogin}
                  disabled={!isLogin && selectedRole === 'ADMIN'}
                  autoComplete="new-password"
                  className="w-full bg-slate-800 border border-surface-border rounded-lg px-4 py-2.5 text-white text-sm focus:outline-none focus:border-accent transition-colors disabled:opacity-50"
                  placeholder="••••••••"
                />
              </div>
            )}

            <button
              type="submit"
              disabled={loading || (!isLogin && selectedRole === 'ADMIN')}
              className="w-full btn-primary motion-btn-interactive py-3 rounded-lg mt-2 text-white font-medium flex items-center justify-center gap-2 cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed text-sm"
            >
              {loading ? (
                <>
                  <FaSpinner className="animate-spin" /> Processing...
                </>
              ) : isLogin ? (
                `Sign In as ${selectedRole.charAt(0) + selectedRole.slice(1).toLowerCase()}`
              ) : (
                `Register as ${selectedRole.charAt(0) + selectedRole.slice(1).toLowerCase()}`
              )}
            </button>
          </form>

          <div className="mt-5 text-center text-xs text-slate-400">
            {isLogin ? (
              <p>
                Don't have an account?{' '}
                <button
                  type="button"
                  onClick={() => handleModeSwitch(false)}
                  className="text-accent hover:underline cursor-pointer font-medium"
                >
                  Register here
                </button>
              </p>
            ) : (
              <p>
                Already have an account?{' '}
                <button
                  type="button"
                  onClick={() => handleModeSwitch(true)}
                  className="text-accent hover:underline cursor-pointer font-medium"
                >
                  Sign in here
                </button>
              </p>
            )}
          </div>

          <Dialog.Close asChild>
            <button
              className="absolute top-4 right-4 text-slate-400 hover:text-white transition-colors cursor-pointer"
              aria-label="Close"
            >
              <FaTimes />
            </button>
          </Dialog.Close>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
};

export default LoginRegisterModal;
