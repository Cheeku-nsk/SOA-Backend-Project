import { useLocation } from 'react-router-dom';

/**
 * PageTransition wrapper
 * Smoothly fades and lifts new page content upward by 12px over 350ms.
 * Re-triggers on route change using location.pathname without delaying navigation.
 */
const PageTransition = ({ children }) => {
  const location = useLocation();

  return (
    <div 
      key={location.pathname} 
      className="motion-page-enter w-full h-full"
    >
      {children}
    </div>
  );
};

export default PageTransition;
