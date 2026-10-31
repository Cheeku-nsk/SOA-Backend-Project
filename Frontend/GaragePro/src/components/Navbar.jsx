import { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { FaBars, FaTimes, FaCar, FaUserCircle } from 'react-icons/fa';
import { useAuth } from '../context/AuthContext';
import LoginRegisterModal from './LoginRegisterModal';
import ProfileDropdown from './ProfileDropdown';

const Navbar = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [pendingBookNow, setPendingBookNow] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const { isAuthenticated, user, logout, setIsBookingOpen } = useAuth();

  const isStaff = isAuthenticated && (user?.role === 'ADMIN' || user?.role === 'TECHNICIAN');

  // Close mobile menu on route change without triggering cascading render warnings
  const [prevPath, setPrevPath] = useState(location.pathname);
  if (prevPath !== location.pathname) {
    setPrevPath(location.pathname);
    setIsOpen(false);
  }

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const publicLinks = [
    { name: 'Home', path: '/' },
    { name: 'Services', path: '/services' },
    { name: 'Contact', path: '/contact' },
  ];

  const isActive = (path) => location.pathname === path;

  const handleBookNow = (e) => {
    e.preventDefault();
    if (isStaff) {
      navigate('/dashboard');
      return;
    }
    if (isAuthenticated) {
      setIsBookingOpen(true);
    } else {
      setPendingBookNow(true);
      setShowAuthModal(true);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <>
      <header 
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ease-in-out border-b border-surface-border ${
          scrolled ? 'bg-background/90 backdrop-blur-md shadow-lg py-3' : 'bg-slate-950 py-4'
        }`}
      >
        <div className="container mx-auto px-4 md:px-6">
          <div className="flex items-center justify-between">
            
            {/* Logo: Routes to /dashboard for staff, / for customers/guests */}
            <Link to={isStaff ? "/dashboard" : "/"} className="flex items-center gap-2 group">
              <div className="bg-gradient-to-br from-primary to-accent p-2 rounded-lg group-hover:shadow-lg group-hover:shadow-primary/30 group-hover:scale-105 transition-all duration-200">
                <FaCar className="text-white text-xl" />
              </div>
              <span className="text-xl font-bold tracking-tight text-white">
                Precision<span className="text-accent">Auto</span> Care
              </span>
            </Link>

            {/* Desktop Navigation */}
            <nav className="hidden md:flex items-center gap-8">
              <ul className="flex items-center gap-6">
                {!isStaff && !isAuthenticated && publicLinks.map((link) => (
                  <li key={link.name}>
                    <Link 
                      to={link.path}
                      className={`text-sm font-medium transition-colors hover:text-accent relative py-2 ${
                        isActive(link.path) ? 'text-accent' : 'text-slate-300'
                      }`}
                    >
                      {link.name}
                      {isActive(link.path) && (
                        <span className="absolute bottom-0 left-0 w-full h-0.5 bg-accent rounded-full transition-all" />
                      )}
                    </Link>
                  </li>
                ))}
              </ul>
              
              <div className="flex items-center gap-4 border-l border-surface-border pl-6">
                {isAuthenticated ? (
                  <>
                    <Link to="/dashboard" className="text-sm font-medium text-slate-300 hover:text-white transition-colors">
                      Dashboard
                    </Link>
                    <ProfileDropdown />
                  </>
                ) : (
                  <button 
                    onClick={() => {
                      setPendingBookNow(false);
                      setShowAuthModal(true);
                    }} 
                    className="text-sm font-medium text-slate-300 hover:text-white transition-colors cursor-pointer"
                  >
                    Log In
                  </button>
                )}

                {/* Only Customers or unauthenticated visitors see "Book Now" */}
                {!isStaff && (
                  <button 
                    onClick={handleBookNow} 
                    className="btn-primary motion-btn-interactive py-2 px-4 text-sm"
                  >
                    Book Now
                  </button>
                )}
              </div>
            </nav>

            {/* Mobile Menu Button */}
            <button 
              className="md:hidden text-slate-300 hover:text-white p-2 cursor-pointer"
              onClick={() => setIsOpen(!isOpen)}
              aria-label="Toggle menu"
            >
              {isOpen ? <FaTimes size={24} /> : <FaBars size={24} />}
            </button>
          </div>
        </div>

        {/* Mobile Navigation */}
        <div 
          className={`md:hidden absolute top-full left-0 w-full bg-slate-900 border-b border-surface-border shadow-2xl transition-all duration-300 overflow-hidden ${
            isOpen ? 'max-h-screen opacity-100 py-4' : 'max-h-0 opacity-0 py-0 border-transparent'
          }`}
        >
          <div className="container mx-auto px-4 flex flex-col gap-4">
            {!isStaff && !isAuthenticated && publicLinks.map((link) => (
              <Link 
                key={link.name}
                to={link.path}
                className={`block px-4 py-3 rounded-lg text-base font-medium transition-colors ${
                  isActive(link.path) ? 'bg-primary/20 text-accent' : 'text-slate-300 hover:bg-white/5'
                }`}
              >
                {link.name}
              </Link>
            ))}
            <div className="h-px bg-surface-border my-2 mx-4" />
            {isAuthenticated ? (
              <>
                <div className="px-4 py-2 flex items-center gap-3">
                  <FaUserCircle className="text-accent text-2xl" />
                  <div>
                    <p className="text-white font-medium">{user?.name}</p>
                    <p className="text-slate-400 text-sm">{user?.email}</p>
                  </div>
                </div>
                <Link to="/dashboard" className="block px-4 py-3 text-base font-medium text-slate-300 hover:text-white">
                  Dashboard
                </Link>
                <button onClick={handleLogout} className="block w-full text-left px-4 py-3 text-base font-medium text-red-400 hover:text-red-300 cursor-pointer">
                  Logout
                </button>
              </>
            ) : (
              <button 
                onClick={() => { setIsOpen(false); setPendingBookNow(false); setShowAuthModal(true); }}
                className="block w-full text-left px-4 py-3 text-base font-medium text-slate-300 hover:text-white cursor-pointer"
              >
                Log In
              </button>
            )}

            {/* Mobile Book Now button only for customers and guests */}
            {!isStaff && (
              <button 
                onClick={(e) => { setIsOpen(false); handleBookNow(e); }}
                className="block mx-4 my-2 text-center btn-primary motion-btn-interactive w-[calc(100%-2rem)]"
              >
                Book Now
              </button>
            )}
          </div>
        </div>
      </header>

      <LoginRegisterModal 
        open={showAuthModal} 
        onOpenChange={(isOpen) => {
          setShowAuthModal(isOpen);
          if (!isOpen) setPendingBookNow(false);
        }} 
        onSuccess={() => {
          setShowAuthModal(false);
          if (pendingBookNow) {
            setPendingBookNow(false);
            const storedUser = JSON.parse(localStorage.getItem('user') || '{}');
            if (storedUser?.role === 'CUSTOMER') {
              setIsBookingOpen(true);
            }
          }
        }}
      />
    </>
  );
};

export default Navbar;
