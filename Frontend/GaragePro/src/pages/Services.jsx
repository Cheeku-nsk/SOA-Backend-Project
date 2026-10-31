import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import SectionTitle from '../components/SectionTitle';
import ServiceCard from '../components/ServiceCard';
import AutomotiveBackground from '../components/motion/AutomotiveBackground';
import { FaWrench, FaOilCan, FaCarBattery, FaSnowflake, FaCogs, FaCarCrash } from 'react-icons/fa';

const Services = () => {
  const { isAuthenticated, user } = useAuth();

  // Admin and Technician cannot book customer services
  if (isAuthenticated && (user?.role === 'ADMIN' || user?.role === 'TECHNICIAN')) {
    return <Navigate to="/dashboard" replace />;
  }

  const services = [
    {
      icon: <FaWrench />,
      title: 'General Maintenance',
      price: '$89.99',
      duration: '60 mins',
      description: 'Comprehensive multi-point inspections and regular maintenance to keep your fleet running smoothly and efficiently.'
    },
    {
      icon: <FaOilCan />,
      title: 'Oil & Fluid Service',
      price: '$49.99',
      duration: '30 mins',
      description: 'Complete fluid checks, flushes, and replacements including engine oil, transmission fluid, and brake fluid.'
    },
    {
      icon: <FaCarCrash />,
      title: 'Brake Services',
      price: '$129.99',
      duration: '90 mins',
      description: 'Expert brake pad replacement, rotor resurfacing, and complete hydraulic system diagnostics.'
    },
    {
      icon: <FaCarBattery />,
      title: 'Electrical Diagnostics',
      price: '$79.99',
      duration: '45 mins',
      description: 'Advanced computer diagnostics to identify and resolve complex electrical and sensor issues.'
    },
    {
      icon: <FaSnowflake />,
      title: 'A/C & Heating',
      price: '$99.99',
      duration: '60 mins',
      description: 'Climate control system repair, refrigerant recharge, and compressor replacement.'
    },
    {
      icon: <FaCogs />,
      title: 'Engine Repair',
      price: '$199.99',
      duration: '120 mins',
      description: 'From minor tune-ups to major overhauls, our certified mechanics handle all engine-related repairs.'
    }
  ];

  return (
    <div className="min-h-screen bg-slate-950 pt-12 pb-24 relative overflow-hidden">
      {/* Automotive Subtle Background Decor */}
      <AutomotiveBackground />
      
      <div className="container mx-auto px-4 md:px-6 relative z-10">
        <SectionTitle 
          subtitle="Our Expertise"
          title="Professional Automotive Services"
          description="Explore our comprehensive range of vehicle maintenance and repair services. All operations are logged and tracked within the PrecisionAuto Care platform."
        />

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 max-w-7xl mx-auto">
          {services.map((service, index) => (
            <ServiceCard 
              key={index}
              icon={service.icon}
              title={service.title}
              price={service.price}
              duration={service.duration}
              description={service.description}
              delay={index * 90 + 100}
            />
          ))}
        </div>
      </div>
    </div>
  );
};

export default Services;
