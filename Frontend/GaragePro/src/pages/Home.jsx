import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import HeroSection from '../components/HeroSection';
import FeatureSection from '../components/FeatureSection';
import WorkflowSection from '../components/WorkflowSection';

const Home = () => {
  const { isAuthenticated, user } = useAuth();

  // Admin and Technician roles should directly see their dashboard console
  if (isAuthenticated && (user?.role === 'ADMIN' || user?.role === 'TECHNICIAN')) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <div>
      <HeroSection />
      <FeatureSection />
      <WorkflowSection />
    </div>
  );
};

export default Home;
