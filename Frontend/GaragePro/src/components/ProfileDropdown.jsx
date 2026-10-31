import { useState, useRef, useEffect } from 'react';
import { FaUserCircle, FaSignOutAlt, FaUser } from 'react-icons/fa';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

const ProfileDropdown = () => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  // Close when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    
    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('keydown', handleEscape);
    }
    
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen]);

  const handleLogout = () => {
    setIsOpen(false);
    logout();
    navigate('/');
  };

  if (!user) return null;

  return (
    <div className="relative" ref={dropdownRef}>
      <button 
        onClick={() => setIsOpen(!isOpen)}
        className={`flex items-center gap-2 bg-slate-800 hover:bg-slate-700 px-3 py-1.5 rounded-full border transition-all duration-200 cursor-pointer ${
          isOpen ? 'border-accent shadow-[0_0_12px_rgba(14,165,233,0.25)]' : 'border-surface-border'
        }`}
        aria-expanded={isOpen}
        aria-haspopup="true"
      >
        <FaUserCircle className="text-accent text-lg" />
        <span className="text-sm font-medium text-white select-none">{user.name}</span>
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-64 bg-slate-900 border border-surface-border rounded-xl shadow-2xl z-[100] motion-page-enter">
          
          {/* Header */}
          <div className="p-4 border-b border-surface-border flex items-center gap-3">
            <FaUserCircle className="text-4xl text-accent" />
            <div className="overflow-hidden">
              <p className="text-white font-medium truncate">{user.name}</p>
              <span className="text-xs text-accent font-medium px-2 py-0.5 bg-accent/10 rounded-full border border-accent/20 inline-block mt-1">
                {user.role}
              </span>
            </div>
          </div>

          {/* Details */}
          <div className="p-4 space-y-3 border-b border-surface-border">
            <div>
              <p className="text-xs text-slate-500 font-medium uppercase tracking-wider mb-1">Name</p>
              <p className="text-sm text-slate-300 truncate">{user.name}</p>
            </div>
            <div>
              <p className="text-xs text-slate-500 font-medium uppercase tracking-wider mb-1">Email</p>
              <p className="text-sm text-slate-300 truncate">{user.email}</p>
            </div>
            <div>
              <p className="text-xs text-slate-500 font-medium uppercase tracking-wider mb-1">Role</p>
              <p className="text-sm text-slate-300">{user.role}</p>
            </div>
          </div>

          {/* Actions */}
          <div className="p-2">
            <button 
              onClick={() => { setIsOpen(false); navigate('/dashboard'); }}
              className="w-full text-left flex items-center gap-3 px-3 py-2 text-sm text-slate-300 hover:text-white hover:bg-white/5 rounded-lg transition-colors cursor-pointer"
            >
              <FaUser className="text-slate-400" />
              View Profile
            </button>
            <button 
              onClick={handleLogout}
              className="w-full text-left flex items-center gap-3 px-3 py-2 text-sm text-red-400 hover:text-red-300 hover:bg-red-400/10 rounded-lg transition-colors mt-1 cursor-pointer"
            >
              <FaSignOutAlt className="text-red-400/70" />
              Logout
            </button>
          </div>

        </div>
      )}
    </div>
  );
};

export default ProfileDropdown;
