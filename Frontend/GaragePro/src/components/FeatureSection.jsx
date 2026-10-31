import { FaCalendarAlt, FaClipboardList, FaFileInvoiceDollar, FaCarCrash } from 'react-icons/fa';

const features = [
  {
    icon: <FaCalendarAlt className="text-3xl text-primary" />,
    title: 'Book Service Slots',
    description: 'Effortlessly request and schedule vehicle servicing based on real-time availability of garage bays and technicians.',
    gradient: 'from-blue-500/10 to-indigo-500/10',
    delay: 120
  },
  {
    icon: <FaClipboardList className="text-3xl text-accent" />,
    title: 'Manage Vehicle Records',
    description: 'Maintain organized profiles for vehicles, linking them directly to customer data and service histories.',
    gradient: 'from-cyan-500/10 to-sky-500/10',
    delay: 240
  },
  {
    icon: <FaCarCrash className="text-3xl text-emerald-400" />,
    title: 'Track Maintenance & Repairs',
    description: 'Keep a complete, immutable history of all vehicle servicing, parts replaced, and mechanic notes.',
    gradient: 'from-emerald-500/10 to-teal-500/10',
    delay: 360
  },
  {
    icon: <FaFileInvoiceDollar className="text-3xl text-purple-400" />,
    title: 'Manage Billing',
    description: 'Automatically generate professional invoices based on completed service records and labor costs.',
    gradient: 'from-purple-500/10 to-fuchsia-500/10',
    delay: 480
  }
];

const FeatureSection = () => {
  return (
    <section className="py-24 bg-slate-950 relative">
      <div className="container mx-auto px-4 md:px-6 relative z-10">
        
        <div className="text-center max-w-3xl mx-auto mb-16">
          <h2 className="text-sm font-bold text-accent uppercase tracking-wider mb-3">Platform Capabilities</h2>
          <h3 className="text-3xl md:text-5xl font-bold text-white mb-6">Centralized Service Operations</h3>
          <p className="text-slate-400 text-lg leading-relaxed">
            PrecisionAuto Care eliminates booking collisions, messy paper records, and billing confusion by unifying your entire garage workflow into one seamless digital experience.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {features.map((feature, index) => (
            <div 
              key={index} 
              className="motion-stat-card h-full"
              style={{ animationDelay: `${feature.delay}ms` }}
            >
              <div className="glass-card motion-card-interactive p-8 group relative overflow-hidden h-full">
                <div className={`absolute inset-0 bg-gradient-to-br ${feature.gradient} opacity-0 group-hover:opacity-100 transition-opacity duration-300 pointer-events-none`}></div>
                <div className="relative z-10">
                  <div className="w-16 h-16 rounded-2xl bg-surface-border flex items-center justify-center mb-6 shadow-inner group-hover:scale-105 transition-transform duration-300">
                    {feature.icon}
                  </div>
                  <h4 className="text-xl font-bold text-white mb-3">{feature.title}</h4>
                  <p className="text-slate-400 leading-relaxed text-sm">
                    {feature.description}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default FeatureSection;
