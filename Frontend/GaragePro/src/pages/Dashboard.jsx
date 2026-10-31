import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Navigate, NavLink, Routes, Route, useLocation } from 'react-router-dom';
import {
  FaUserCircle,
  FaCalendarAlt,
  FaCar,
  FaHistory,
  FaFileInvoiceDollar,
  FaTools,
  FaUsers,
  FaBars,
  FaTimes,
  FaCheckCircle,
  FaPlus,
  FaChartBar,
} from 'react-icons/fa';
import StatusIndicator from '../components/motion/StatusIndicator';
import AutomotiveBackground from '../components/motion/AutomotiveBackground';
import useCountUp from '../hooks/useCountUp';
import VehicleManagement from '../components/dashboard/VehicleManagement';
import CustomerBookings from '../components/dashboard/CustomerBookings';
import CustomerServiceHistory from '../components/dashboard/CustomerServiceHistory';
import CustomerBillings from '../components/dashboard/CustomerBillings';
import AdminDashboard from '../components/dashboard/AdminDashboard';
import TechnicianDashboard from '../components/dashboard/TechnicianDashboard';
import { api } from '../utils/api';

const Dashboard = () => {
  const { isAuthenticated, user, setIsBookingOpen } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const location = useLocation();

  // Sync menu state with pathname change without cascading render warning
  const [prevPath, setPrevPath] = useState(location.pathname);
  if (prevPath !== location.pathname) {
    setPrevPath(location.pathname);
    setMobileMenuOpen(false);
  }

  if (!isAuthenticated) {
    return <Navigate to="/" />;
  }

  const renderSidebarLinks = () => {
    switch (user?.role) {
      case 'ADMIN':
        return (
          <>
            <SidebarLink to="" icon={<FaUserCircle />} label="Overview" exact />
            <SidebarLink to="approvals" icon={<FaUsers />} label="Technician Approvals" />
            <SidebarLink to="bookings" icon={<FaCalendarAlt />} label="Manage Bookings" />
            <SidebarLink to="bays" icon={<FaTools />} label="Service Bays" />
            <SidebarLink to="invoices" icon={<FaFileInvoiceDollar />} label="Billing Management" />
          </>
        );
      case 'TECHNICIAN':
        return (
          <>
            <SidebarLink to="" icon={<FaUserCircle />} label="Overview" exact />
            <SidebarLink to="assigned" icon={<FaTools />} label="Active Work Orders" />
            <SidebarLink to="records" icon={<FaHistory />} label="Service Records" />
          </>
        );
      case 'CUSTOMER':
      default:
        return (
          <>
            <SidebarLink to="" icon={<FaUserCircle />} label="Overview" exact />
            <SidebarLink
              as="button"
              onClick={() => setIsBookingOpen(true)}
              icon={<FaPlus />}
              label="Book a Service"
            />
            <SidebarLink to="my-bookings" icon={<FaCalendarAlt />} label="My Bookings" />
            <SidebarLink to="my-vehicles" icon={<FaCar />} label="My Vehicles" />
            <SidebarLink to="history" icon={<FaHistory />} label="Service History" />
            <SidebarLink to="my-billings" icon={<FaFileInvoiceDollar />} label="My Billings" />
          </>
        );
    }
  };

  return (
    <div className="flex flex-col md:flex-row min-h-[calc(100vh-5rem)] bg-slate-950">
      {/* Mobile Sidebar Toggle */}
      <div className="md:hidden flex items-center justify-between bg-slate-900 p-4 border-b border-surface-border">
        <div className="flex items-center gap-3">
          <FaUserCircle className="text-2xl text-accent" />
          <span className="font-semibold text-white">{user?.name}</span>
        </div>
        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          className="text-slate-300 hover:text-white p-2"
          aria-label="Toggle menu"
        >
          {mobileMenuOpen ? <FaTimes size={20} /> : <FaBars size={20} />}
        </button>
      </div>

      {/* Sidebar Workspace */}
      <aside
        className={`
          w-full md:w-64 lg:w-72 flex-shrink-0 bg-slate-900 border-r border-surface-border z-10
          ${mobileMenuOpen ? 'block' : 'hidden md:block'}
        `}
      >
        <div className="p-6 md:sticky md:top-20">
          <div className="hidden md:flex items-center gap-4 mb-8 pb-6 border-b border-surface-border">
            <FaUserCircle className="text-4xl text-accent" />
            <div className="overflow-hidden">
              <h2 className="font-bold text-white truncate">{user?.name}</h2>
              <span className="inline-block mt-1 text-xs text-accent font-medium px-2 py-0.5 bg-accent/10 rounded-full border border-accent/20">
                {user?.role}
              </span>
            </div>
          </div>

          <nav className="flex flex-col gap-1">{renderSidebarLinks()}</nav>

          {/* Profile Info for Mobile */}
          <div className="md:hidden mt-6 pt-6 border-t border-surface-border">
            <p className="text-sm text-slate-400">Logged in as</p>
            <p className="font-medium text-white truncate">{user?.email}</p>
            <span className="inline-block mt-2 text-xs text-accent font-medium px-2 py-0.5 bg-accent/10 rounded-full border border-accent/20">
              {user?.role}
            </span>
          </div>
        </div>
      </aside>

      {/* Main Content Workspace */}
      <main className="flex-grow min-w-0 bg-slate-950 relative overflow-hidden">
        <AutomotiveBackground />

        <div className="p-6 md:p-8 lg:p-10 relative z-10">
          <div className="max-w-5xl mx-auto">
            <Routes>
              <Route path="" element={<DashboardOverview user={user} />} />

              {/* Customer Routes */}
              <Route path="my-bookings" element={<CustomerBookings />} />
              <Route path="my-vehicles" element={<VehicleManagement />} />
              <Route path="history" element={<CustomerServiceHistory />} />
              <Route path="my-billings" element={<CustomerBillings />} />
              <Route path="billing" element={<CustomerBillings />} />

              {/* Admin Routes */}
              <Route path="approvals" element={<AdminDashboard activeTab="approvals" />} />
              <Route path="users" element={<AdminDashboard activeTab="approvals" />} />
              <Route path="bookings" element={<AdminDashboard activeTab="bookings" />} />
              <Route path="bays" element={<AdminDashboard activeTab="bays" />} />
              <Route path="slots" element={<AdminDashboard activeTab="bays" />} />
              <Route path="invoices" element={<AdminDashboard activeTab="invoices" />} />

              {/* Technician Routes */}
              <Route path="assigned" element={<TechnicianDashboard activeTab="active" />} />
              <Route path="records" element={<TechnicianDashboard activeTab="completed" />} />

              <Route
                path="*"
                element={
                  <EmptyState
                    title="Section Not Found"
                    message="Please choose a section from the dashboard navigation menu."
                    icon={<FaTools />}
                  />
                }
              />
            </Routes>
          </div>
        </div>
      </main>
    </div>
  );
};

const SidebarLink = ({ to, icon, label, exact, as, onClick }) => {
  const baseClasses =
    'flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200';

  if (as === 'button') {
    return (
      <button
        onClick={onClick}
        className={`${baseClasses} text-slate-300 hover:text-white hover:bg-white/5 hover:translate-x-1 w-full text-left cursor-pointer`}
      >
        <span className="text-xl">{icon}</span>
        <span className="font-medium text-sm">{label}</span>
      </button>
    );
  }

  return (
    <NavLink
      to={to}
      end={exact}
      className={({ isActive }) => `
        ${baseClasses}
        ${
          isActive
            ? 'bg-primary/20 text-accent border border-accent/20 shadow-[0_0_15px_rgba(56,189,248,0.1)] translate-x-1'
            : 'text-slate-300 hover:text-white hover:bg-white/5 hover:translate-x-1'
        }
      `}
    >
      <span className="text-xl">{icon}</span>
      <span className="font-medium text-sm">{label}</span>
    </NavLink>
  );
};

const EmptyState = ({ title, message, icon }) => (
  <div className="motion-page-enter flex flex-col items-center justify-center text-center py-20 h-full">
    <div className="w-20 h-20 bg-slate-800/50 rounded-full flex items-center justify-center text-slate-500 mb-6 text-4xl border border-surface-border/50">
      {icon}
    </div>
    <h2 className="text-2xl font-bold text-white mb-2">{title}</h2>
    <p className="text-slate-400 max-w-md">{message}</p>
  </div>
);

const DashboardOverview = ({ user }) => {
  const currentDate = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
  });

  const [stats, setStats] = useState({
    upcomingBookings: '0',
    vehiclesCount: '0',
    historyCount: '0',
    pendingApprovals: '0',
    totalUsers: '0',
    activeBays: '4 / 4',
    techTasks: '0',
    completedTasks: '0',
  });

  const [bays, setBays] = useState([]);
  const [bookings, setBookings] = useState([]);

  useEffect(() => {
    const fetchTelemetry = async () => {
      try {
        if (user?.role === 'CUSTOMER') {
          const [vList, bList, rList, baysList] = await Promise.all([
            api.get('/vehicles').catch(() => []),
            api.get('/bookings/my').catch(() => []),
            api.get('/records/customer').catch(() => []),
            api.get('/bookings/bays').catch(() => []),
          ]);

          const validBays = Array.isArray(baysList) && baysList.length > 0 ? baysList : [];
          const validBookings = Array.isArray(bList) ? bList : [];
          setBays(validBays);
          setBookings(validBookings);

          const activeCount = validBookings.filter(
            (b) => b.status === 'CONFIRMED' || b.status === 'PENDING' || b.status === 'IN_PROGRESS'
          ).length;

          setStats((prev) => ({
            ...prev,
            vehiclesCount: String(Array.isArray(vList) ? vList.length : 0),
            upcomingBookings: String(activeCount),
            historyCount: String(Array.isArray(rList) ? rList.length : 0),
          }));
        } else if (user?.role === 'ADMIN') {
          const [pList, bList, baysList] = await Promise.all([
            api.get('/auth/technicians/pending').catch(() => []),
            api.get('/bookings').catch(() => []),
            api.get('/bookings/bays').catch(() => []),
          ]);

          const validBays = Array.isArray(baysList) && baysList.length > 0 ? baysList : [];
          const validBookings = Array.isArray(bList) ? bList : [];
          setBays(validBays);
          setBookings(validBookings);

          const totalBays = validBays.length > 0 ? validBays.length : 4;
          const availableBays = validBays.length > 0
            ? validBays.filter((b) => b.status === 'AVAILABLE').length
            : 4;

          setStats((prev) => ({
            ...prev,
            pendingApprovals: String(Array.isArray(pList) ? pList.length : 0),
            upcomingBookings: String(validBookings.length),
            activeBays: `${availableBays} / ${totalBays}`,
          }));
        } else if (user?.role === 'TECHNICIAN') {
          const [rList, baysList] = await Promise.all([
            api.get('/records/technician').catch(() => []),
            api.get('/bookings/bays').catch(() => []),
          ]);

          const list = Array.isArray(rList) ? rList : [];
          const validBays = Array.isArray(baysList) && baysList.length > 0 ? baysList : [];
          setBays(validBays);
          setBookings(list);

          const activeJobs = list.filter((r) => r.status === 'PENDING' || r.status === 'IN_PROGRESS').length;
          const completedJobs = list.filter((r) => r.status === 'COMPLETED').length;
          const assignedBays = validBays.length > 0 ? validBays.length : 4;

          setStats((prev) => ({
            ...prev,
            techTasks: String(activeJobs),
            completedTasks: String(completedJobs),
            activeBays: `${assignedBays} Bays`,
          }));
        }
      } catch {
        // Fallback to defaults
      }
    };
    fetchTelemetry();
  }, [user]);

  const defaultBays = [
    { bayId: 1, bayNumber: 'Bay 1', bayType: 'Express Maintenance', status: 'AVAILABLE' },
    { bayId: 2, bayNumber: 'Bay 2', bayType: 'Diagnostics & Electrical', status: 'AVAILABLE' },
    { bayId: 3, bayNumber: 'Bay 3', bayType: 'Engine & Heavy Overhaul', status: 'AVAILABLE' },
    { bayId: 4, bayNumber: 'Bay 4', bayType: 'Brake & Hydraulic Alignment', status: 'AVAILABLE' },
  ];

  const operationalBays = bays.length > 0 ? bays : defaultBays;

  const computedBayTelemetry = operationalBays.map((bay, idx) => {
    const bayBookings = bookings.filter((b) =>
      b &&
      (b.bayNumber === bay.bayNumber || b.bayNumber?.includes(`Bay ${idx + 1}`)) &&
      b.status !== 'CANCELLED'
    );
    const activeCount = bayBookings.filter(
      (b) => b.status === 'CONFIRMED' || b.status === 'IN_PROGRESS' || b.status === 'PENDING'
    ).length;
    // Calculate realistic capacity percentage against 5 daily session slots
    const capacityPercent = Math.min(Math.round((activeCount / 5) * 100), 100);

    let statusLabel = 'Available';
    let colorGradient = 'from-emerald-500 to-teal-400';
    let statusColor = 'text-emerald-400';

    if (bay.status === 'MAINTENANCE' || bay.status === 'INACTIVE') {
      statusLabel = 'Maintenance';
      colorGradient = 'from-slate-600 to-slate-500';
      statusColor = 'text-slate-400';
    } else if (activeCount >= 4) {
      statusLabel = 'High Load';
      colorGradient = 'from-amber-500 to-orange-500';
      statusColor = 'text-amber-400';
    } else if (activeCount > 0) {
      statusLabel = 'Active';
      colorGradient = 'from-blue-600 to-cyan-500';
      statusColor = 'text-cyan-400';
    }

    return {
      bay: `${bay.bayNumber}: ${bay.bayType}`,
      capacity: capacityPercent,
      status: statusLabel,
      color: colorGradient,
      statusColor,
      activeCount,
      delay: 180 + idx * 100,
    };
  });

  return (
    <div>
      <div
        className="motion-hero-item flex flex-col md:flex-row md:items-center justify-between mb-8 gap-4 border-b border-surface-border/50 pb-6"
        style={{ animationDelay: '80ms' }}
      >
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-white tracking-tight">
            Welcome back, {user?.name}
          </h1>
          <p className="text-slate-400 mt-1">
            PrecisionAuto Care operational telemetry and account console.
          </p>
        </div>

        <div className="bg-slate-900/80 backdrop-blur-sm px-4 py-2.5 rounded-lg border border-surface-border flex items-center gap-3 w-max shadow-sm">
          <FaCalendarAlt className="text-accent text-lg" />
          <div>
            <p className="text-sm font-medium text-white leading-none">{currentDate}</p>
            <div className="mt-1">
              <StatusIndicator status="System Online" variant="emerald" />
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
        {user?.role === 'CUSTOMER' && (
          <>
            <StatCard
              title="Active Bookings"
              value={stats.upcomingBookings}
              icon={<FaCalendarAlt />}
              color="text-blue-400"
              bg="bg-blue-500/10"
              delay={160}
            />
            <StatCard
              title="My Vehicles"
              value={stats.vehiclesCount}
              icon={<FaCar />}
              color="text-emerald-400"
              bg="bg-emerald-500/10"
              delay={280}
            />
            <StatCard
              title="Service Records"
              value={stats.historyCount}
              icon={<FaHistory />}
              color="text-amber-400"
              bg="bg-amber-500/10"
              delay={400}
            />
          </>
        )}
        {user?.role === 'ADMIN' && (
          <>
            <StatCard
              title="Total Reservations"
              value={stats.upcomingBookings}
              icon={<FaCalendarAlt />}
              color="text-blue-400"
              bg="bg-blue-500/10"
              delay={160}
            />
            <StatCard
              title="Pending Approvals"
              value={stats.pendingApprovals}
              icon={<FaUsers />}
              color="text-amber-400"
              bg="bg-amber-500/10"
              delay={280}
            />
            <StatCard
              title="Active Service Bays"
              value={stats.activeBays}
              icon={<FaTools />}
              color="text-emerald-400"
              bg="bg-emerald-500/10"
              delay={400}
            />
          </>
        )}
        {user?.role === 'TECHNICIAN' && (
          <>
            <StatCard
              title="Active Repair Jobs"
              value={stats.techTasks}
              icon={<FaTools />}
              color="text-accent"
              bg="bg-blue-500/10"
              delay={160}
            />
            <StatCard
              title="Completed Repairs"
              value={stats.completedTasks}
              icon={<FaCheckCircle />}
              color="text-emerald-400"
              bg="bg-emerald-500/10"
              delay={280}
            />
            <StatCard
              title="Assigned Bays"
              value={stats.activeBays || '4 Bays'}
              icon={<FaTools />}
              color="text-amber-400"
              bg="bg-amber-500/10"
              delay={400}
            />
          </>
        )}
      </div>

      <div
        className="glass-card motion-stat-card p-6 border border-surface-border"
        style={{ animationDelay: '520ms' }}
      >
        <div className="flex items-center justify-between mb-6 pb-4 border-b border-surface-border/50">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-lg bg-primary/20 text-accent flex items-center justify-center text-lg">
              <FaChartBar />
            </div>
            <div>
              <h3 className="font-semibold text-white text-base">Service Bay Operational Capacity</h3>
              <p className="text-xs text-slate-400">Real-time workshop telemetry and live bay readiness</p>
            </div>
          </div>
          <span className="text-xs text-slate-400 bg-slate-800/80 px-2.5 py-1 rounded-md border border-surface-border">
            Live Telemetry
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 pt-2">
          {computedBayTelemetry.map((item, idx) => (
            <div key={idx} className="bg-slate-950/60 p-4 rounded-xl border border-surface-border/60 flex flex-col justify-between">
              <div className="flex justify-between items-start mb-4">
                <span className="text-xs text-slate-300 font-medium leading-tight">{item.bay}</span>
                <span className="text-xs font-semibold text-slate-200">{item.capacity}%</span>
              </div>
              <div className="w-full bg-slate-800/80 rounded-full h-2.5 overflow-hidden p-0.5">
                <div
                  className={`h-full rounded-full bg-gradient-to-r ${item.color} motion-bar-grow`}
                  style={{
                    width: `${Math.max(item.capacity, 5)}%`,
                    animationDelay: `${item.delay}ms`,
                  }}
                />
              </div>
              <div className="flex justify-between items-center mt-3 pt-2 border-t border-surface-border/30">
                <span className="text-[10px] text-slate-400">Bay Status</span>
                <span className={`text-[10px] font-semibold ${item.statusColor}`}>
                  {item.status} ({item.activeCount} booked)
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

const StatCard = ({ title, value, icon, color, bg, delay = 0 }) => {
  const animatedValue = useCountUp(value, 900);

  return (
    <div className="motion-stat-card" style={{ animationDelay: `${delay}ms` }}>
      <div className="motion-card-interactive relative bg-slate-900/60 backdrop-blur-sm p-6 rounded-xl border border-surface-border overflow-hidden cursor-default group h-full">
        <div className="absolute inset-0 bg-gradient-to-br from-white/[0.03] to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 pointer-events-none"></div>

        <div className="relative z-10 flex items-center gap-4 mb-4">
          <div
            className={`w-12 h-12 rounded-lg ${bg} ${color} flex items-center justify-center text-2xl border border-white/5 transition-transform duration-300 group-hover:scale-110`}
          >
            {icon}
          </div>
          <h3 className="text-slate-300 font-medium">{title}</h3>
        </div>
        <p className="relative z-10 text-3xl font-bold text-white tracking-tight">{animatedValue}</p>
      </div>
    </div>
  );
};

export default Dashboard;
