import { Link } from 'react-router-dom';
import { FaArrowRight, FaClock, FaTag } from 'react-icons/fa';

const ServiceCard = ({ icon, title, description, price, duration, delay = 0 }) => {
  return (
    <div 
      className="motion-stat-card h-full"
      style={{ animationDelay: `${delay}ms` }}
    >
      <div className="glass-card motion-card-interactive p-8 group flex flex-col h-full relative overflow-hidden">
        {/* Subtle ambient corner glow */}
        <div className="absolute top-0 right-0 w-32 h-32 bg-primary/10 rounded-full blur-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-300 pointer-events-none" />

        <div className="w-14 h-14 rounded-xl bg-surface border border-surface-border flex items-center justify-center mb-6 text-2xl text-primary group-hover:text-accent group-hover:scale-105 transition-all duration-300 shadow-inner">
          {icon}
        </div>
        <h4 className="text-xl font-bold text-white mb-2 tracking-tight">{title}</h4>

        {(price || duration) && (
          <div className="flex items-center gap-3 text-xs mb-4">
            {price && (
              <span className="text-accent font-bold font-mono flex items-center gap-1">
                <FaTag className="text-[10px]" /> {price}
              </span>
            )}
            {price && duration && <span className="text-slate-600">•</span>}
            {duration && (
              <span className="text-slate-400 flex items-center gap-1">
                <FaClock className="text-[10px]" /> ~{duration}
              </span>
            )}
          </div>
        )}

        <p className="text-slate-400 leading-relaxed text-sm flex-grow mb-6">
          {description}
        </p>
        <Link 
          to="/book-slot" 
          state={{ selectedService: title }}
          className="mt-auto inline-flex items-center gap-2 text-sm font-medium text-accent hover:text-white transition-colors w-max group/link"
        >
          <span>Book this service</span>
          <FaArrowRight className="text-xs transition-transform duration-200 group-hover/link:translate-x-1" />
        </Link>
      </div>
    </div>
  );
};

export default ServiceCard;
