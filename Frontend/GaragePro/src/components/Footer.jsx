import { Link } from 'react-router-dom';
import { FaCar, FaFacebook, FaTwitter, FaInstagram, FaLinkedin, FaMapMarkerAlt, FaPhoneAlt, FaEnvelope, FaClock } from 'react-icons/fa';

const Footer = () => {
  return (
    <footer className="bg-slate-950 border-t border-surface-border pt-16 pb-8">
      <div className="container mx-auto px-4 md:px-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12 mb-12">
          
          {/* Brand Info */}
          <div className="flex flex-col gap-4">
            <Link to="/" className="flex items-center gap-2 group w-max">
              <div className="bg-gradient-to-br from-primary to-accent p-2 rounded-lg group-hover:shadow-lg group-hover:shadow-primary/30 transition-all">
                <FaCar className="text-white text-xl" />
              </div>
              <span className="text-xl font-bold tracking-tight text-white">
                Precision<span className="text-accent">Auto</span> Care
              </span>
            </Link>
            <p className="text-slate-400 text-sm leading-relaxed mt-2">
              The premier automotive fleet maintenance & service operations platform. 
              Simplifying bookings, service records, and billing for modern garages.
            </p>
            <div className="flex items-center gap-4 mt-2">
              <a href="#" className="w-10 h-10 rounded-full bg-surface flex items-center justify-center text-slate-400 hover:text-white hover:bg-primary transition-all">
                <FaFacebook />
              </a>
              <a href="#" className="w-10 h-10 rounded-full bg-surface flex items-center justify-center text-slate-400 hover:text-white hover:bg-primary transition-all">
                <FaTwitter />
              </a>
              <a href="#" className="w-10 h-10 rounded-full bg-surface flex items-center justify-center text-slate-400 hover:text-white hover:bg-primary transition-all">
                <FaInstagram />
              </a>
              <a href="#" className="w-10 h-10 rounded-full bg-surface flex items-center justify-center text-slate-400 hover:text-white hover:bg-primary transition-all">
                <FaLinkedin />
              </a>
            </div>
          </div>

          {/* Quick Links */}
          <div>
            <h3 className="text-white font-semibold text-lg mb-6 relative inline-block">
              Quick Links
              <span className="absolute -bottom-2 left-0 w-1/2 h-1 bg-accent rounded-full"></span>
            </h3>
            <ul className="flex flex-col gap-3">
              <li><Link to="/" className="text-slate-400 hover:text-accent transition-colors text-sm">Home</Link></li>
              <li><Link to="/about" className="text-slate-400 hover:text-accent transition-colors text-sm">About Us</Link></li>
              <li><Link to="/services" className="text-slate-400 hover:text-accent transition-colors text-sm">Our Services</Link></li>
              <li><Link to="/book-slot" className="text-slate-400 hover:text-accent transition-colors text-sm">Book an Appointment</Link></li>
              <li><Link to="/contact" className="text-slate-400 hover:text-accent transition-colors text-sm">Contact Support</Link></li>
            </ul>
          </div>

          {/* Services */}
          <div>
            <h3 className="text-white font-semibold text-lg mb-6 relative inline-block">
              Our Services
              <span className="absolute -bottom-2 left-0 w-1/2 h-1 bg-accent rounded-full"></span>
            </h3>
            <ul className="flex flex-col gap-3">
              <li><Link to="/services" className="text-slate-400 hover:text-accent transition-colors text-sm">General Maintenance</Link></li>
              <li><Link to="/services" className="text-slate-400 hover:text-accent transition-colors text-sm">Engine Diagnostics</Link></li>
              <li><Link to="/services" className="text-slate-400 hover:text-accent transition-colors text-sm">Brake Services</Link></li>
              <li><Link to="/services" className="text-slate-400 hover:text-accent transition-colors text-sm">Tire & Wheel Alignments</Link></li>
              <li><Link to="/services" className="text-slate-400 hover:text-accent transition-colors text-sm">Oil & Fluid Changes</Link></li>
            </ul>
          </div>

          {/* Contact Info */}
          <div>
            <h3 className="text-white font-semibold text-lg mb-6 relative inline-block">
              Get in Touch
              <span className="absolute -bottom-2 left-0 w-1/2 h-1 bg-accent rounded-full"></span>
            </h3>
            <ul className="flex flex-col gap-4">
              <li className="flex items-start gap-3">
                <FaMapMarkerAlt className="text-accent mt-1 flex-shrink-0" />
                <span className="text-slate-400 text-sm">123 Automotive Boulevard, Tech Park, San Francisco, CA 94107</span>
              </li>
              <li className="flex items-center gap-3">
                <FaPhoneAlt className="text-accent flex-shrink-0" />
                <span className="text-slate-400 text-sm">+1 (555) 123-4567</span>
              </li>
              <li className="flex items-center gap-3">
                <FaEnvelope className="text-accent flex-shrink-0" />
                <span className="text-slate-400 text-sm">support@precisionautocare.com</span>
              </li>
              <li className="flex items-start gap-3">
                <FaClock className="text-accent mt-1 flex-shrink-0" />
                <div className="flex flex-col">
                  <span className="text-slate-400 text-sm">Mon - Fri: 8:00 AM - 6:00 PM</span>
                  <span className="text-slate-400 text-sm">Sat: 9:00 AM - 2:00 PM</span>
                </div>
              </li>
            </ul>
          </div>

        </div>

        <div className="border-t border-surface-border pt-8 flex flex-col md:flex-row justify-between items-center gap-4">
          <p className="text-slate-500 text-sm">
            &copy; {new Date().getFullYear()} PrecisionAuto Care. All rights reserved.
          </p>
          <div className="flex gap-6">
            <Link to="#" className="text-slate-500 hover:text-white text-sm transition-colors">Privacy Policy</Link>
            <Link to="#" className="text-slate-500 hover:text-white text-sm transition-colors">Terms of Service</Link>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
