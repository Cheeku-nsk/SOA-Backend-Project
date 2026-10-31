import { Link } from 'react-router-dom';
import { FaArrowRight, FaShieldAlt, FaCalendarCheck, FaWrench } from 'react-icons/fa';
import AutomotiveBackground from './motion/AutomotiveBackground';
import StatusIndicator from './motion/StatusIndicator';

const HeroSection = () => {
  return (
    <section className="relative min-h-[90vh] flex items-center pt-20 overflow-hidden">
      {/* Background Image with Overlay */}
      <div className="absolute inset-0 z-0">
        <div className="absolute inset-0 bg-gradient-to-r from-slate-950 via-slate-900/90 to-slate-900/40 z-10"></div>
        <img 
          src="https://images.unsplash.com/photo-1619642751034-765dfdf7c58e?auto=format&fit=crop&q=80&w=2000" 
          alt="Automotive Service Garage" 
          className="w-full h-full object-cover object-center"
        />
      </div>

      {/* Automotive-themed ambient visual motion */}
      <AutomotiveBackground />

      <div className="container mx-auto px-4 md:px-6 relative z-20">
        <div className="max-w-3xl">
          {/* 1. Brand Badge / Section Label */}
          <div className="motion-hero-item motion-delay-1 inline-flex items-center gap-2.5 px-3.5 py-1.5 rounded-full bg-surface/80 border border-surface-border text-accent text-sm font-medium mb-6 backdrop-blur-md shadow-sm">
            <StatusIndicator status="Online" variant="accent" />
            <span className="text-slate-400">|</span>
            <span>Next-Gen Fleet Management</span>
          </div>
          
          {/* 2. Main Heading */}
          <h1 className="motion-hero-item motion-delay-2 text-4xl md:text-6xl lg:text-7xl font-bold text-white leading-tight mb-6 tracking-tight">
            Intelligent <span className="text-transparent bg-clip-text bg-gradient-to-r from-primary via-sky-400 to-accent">Automotive</span> Service Platform
          </h1>
          
          {/* 3. Description Text */}
          <p className="motion-hero-item motion-delay-3 text-lg md:text-xl text-slate-300 mb-10 leading-relaxed max-w-2xl">
            Streamline your garage operations. PrecisionAuto Care provides centralized booking, maintenance tracking, service histories, and automated billing in one modern platform.
          </p>
          
          {/* 4. Action Buttons (Primary CTA & Secondary Action) */}
          <div className="motion-hero-item motion-delay-4 flex flex-col sm:flex-row gap-4 mb-16">
            <Link 
              to="/book-slot" 
              className="btn-primary motion-btn-interactive flex items-center justify-center gap-3 group text-lg"
            >
              <span>Book a Service Slot</span>
              <FaArrowRight className="motion-arrow text-sm" />
            </Link>
            <Link 
              to="/services" 
              className="btn-secondary motion-btn-interactive flex items-center justify-center gap-2 text-lg"
            >
              Explore Services
            </Link>
          </div>

          {/* 5. Quick Stats/Features with Interactive Card Hover */}
          <div className="motion-hero-item motion-delay-5 grid grid-cols-1 sm:grid-cols-3 gap-6 pt-8 border-t border-surface-border/50">
            <div className="glass-card motion-card-interactive p-4 flex items-center gap-4">
              <div className="w-12 h-12 rounded-xl bg-primary/20 text-primary flex items-center justify-center text-xl shrink-0 border border-primary/20">
                <FaCalendarCheck />
              </div>
              <div>
                <h4 className="text-white font-semibold text-sm">Smart Scheduling</h4>
                <p className="text-slate-400 text-xs">Prevent collisions</p>
              </div>
            </div>
            <div className="glass-card motion-card-interactive p-4 flex items-center gap-4">
              <div className="w-12 h-12 rounded-xl bg-accent/20 text-accent flex items-center justify-center text-xl shrink-0 border border-accent/20">
                <FaShieldAlt />
              </div>
              <div>
                <h4 className="text-white font-semibold text-sm">Secure Records</h4>
                <p className="text-slate-400 text-xs">Complete vehicle history</p>
              </div>
            </div>
            <div className="glass-card motion-card-interactive p-4 flex items-center gap-4">
              <div className="w-12 h-12 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center text-xl shrink-0 border border-emerald-500/20">
                <FaWrench />
              </div>
              <div>
                <h4 className="text-white font-semibold text-sm">Expert Diagnostics</h4>
                <p className="text-slate-400 text-xs">Track every repair</p>
              </div>
            </div>
          </div>
          
        </div>
      </div>
    </section>
  );
};

export default HeroSection;
