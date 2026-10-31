import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import SectionTitle from '../components/SectionTitle';
import ContactForm from '../components/ContactForm';
import AutomotiveBackground from '../components/motion/AutomotiveBackground';
import { FaMapMarkerAlt, FaPhoneAlt, FaEnvelope, FaClock } from 'react-icons/fa';

const Contact = () => {
  const { isAuthenticated, user } = useAuth();

  if (isAuthenticated && (user?.role === 'ADMIN' || user?.role === 'TECHNICIAN')) {
    return <Navigate to="/dashboard" replace />;
  }
  return (
    <div className="min-h-screen bg-slate-950 pt-12 pb-24 relative overflow-hidden">
      {/* Background Decor */}
      <AutomotiveBackground />
      
      <div className="container mx-auto px-4 md:px-6 relative z-10 max-w-6xl">
        <SectionTitle 
          subtitle="Get in Touch"
          title="Contact Support"
          description="Have questions about our platform or want to discuss fleet maintenance solutions? We're here to help."
        />

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 mt-12">
          
          {/* Contact Info */}
          <div className="space-y-8">
            <div className="glass-card motion-card-interactive p-8 group">
              <div className="w-12 h-12 rounded-lg bg-surface border border-surface-border flex items-center justify-center text-primary mb-6 transition-transform group-hover:scale-105">
                <FaMapMarkerAlt className="text-xl" />
              </div>
              <h4 className="text-lg font-bold text-white mb-2">Headquarters</h4>
              <p className="text-slate-400 leading-relaxed">
                123 Automotive Boulevard<br />
                Tech Park, San Francisco<br />
                CA 94107, United States
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="glass-card motion-card-interactive p-6 group">
                <FaPhoneAlt className="text-accent mb-4 text-xl transition-transform group-hover:scale-110" />
                <h4 className="text-white font-bold mb-1">Phone</h4>
                <p className="text-slate-400 text-sm">+1 (555) 123-4567</p>
              </div>
              
              <div className="glass-card motion-card-interactive p-6 group">
                <FaEnvelope className="text-accent mb-4 text-xl transition-transform group-hover:scale-110" />
                <h4 className="text-white font-bold mb-1">Email</h4>
                <p className="text-slate-400 text-sm">support@precisionauto.com</p>
              </div>
            </div>

            <div className="glass-card motion-card-interactive p-8 group">
              <div className="flex items-center gap-4 mb-4">
                <FaClock className="text-emerald-400 text-2xl transition-transform group-hover:rotate-12" />
                <h4 className="text-lg font-bold text-white">Business Hours</h4>
              </div>
              <ul className="space-y-2 text-slate-400">
                <li className="flex justify-between border-b border-surface-border pb-2">
                  <span>Monday - Friday</span>
                  <span className="text-white font-medium">8:00 AM - 6:00 PM</span>
                </li>
                <li className="flex justify-between border-b border-surface-border pb-2">
                  <span>Saturday</span>
                  <span className="text-white font-medium">9:00 AM - 2:00 PM</span>
                </li>
                <li className="flex justify-between">
                  <span>Sunday</span>
                  <span className="text-slate-500 font-medium">Closed</span>
                </li>
              </ul>
            </div>
          </div>

          {/* Contact Form */}
          <div>
            <ContactForm />
          </div>

        </div>
      </div>
    </div>
  );
};

export default Contact;
