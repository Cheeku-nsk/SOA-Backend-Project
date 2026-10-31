import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import SectionTitle from '../components/SectionTitle';
import BookingForm from '../components/BookingForm';
import AutomotiveBackground from '../components/motion/AutomotiveBackground';

const BookSlot = () => {
  const { isAuthenticated, user } = useAuth();

  // Admin and Technician cannot book customer slots
  if (isAuthenticated && (user?.role === 'ADMIN' || user?.role === 'TECHNICIAN')) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <div className="min-h-screen bg-slate-950 pt-12 pb-24 relative overflow-hidden">
      {/* Automotive Subtle Background Decor */}
      <AutomotiveBackground />
      
      <div className="container mx-auto px-4 md:px-6 relative z-10 max-w-4xl">
        <SectionTitle 
          subtitle="Schedule Service"
          title="Book a Service Slot"
          description="Reserve a bay for your vehicle. Fill out the details below to request a service appointment."
        />

        <div className="glass-panel p-6 md:p-8">
          <BookingForm />
        </div>
      </div>
    </div>
  );
};

export default BookSlot;
