/**
 * AutomotiveBackground Component
 * Renders subtle automotive-themed SVG telemetry lines, tech grid, and soft ambient lightrays.
 * Low contrast, zero CPU-heavy loops, lightweight GPU CSS transforms.
 */
const AutomotiveBackground = ({ className = "" }) => {
  return (
    <div className={`pointer-events-none absolute inset-0 overflow-hidden select-none z-0 ${className}`} aria-hidden="true">
      {/* Soft Ambient Radial Lighting */}
      <div className="absolute top-1/4 -left-48 w-[500px] h-[500px] bg-primary/15 rounded-full blur-[140px] mix-blend-screen" />
      <div className="absolute top-1/3 -right-48 w-[500px] h-[500px] bg-accent/10 rounded-full blur-[140px] mix-blend-screen" />

      {/* SVG Automotive Track & Grid Accents */}
      <svg
        className="absolute inset-0 w-full h-full opacity-[0.06] text-slate-400"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          <pattern id="automotive-grid" width="60" height="60" patternUnits="userSpaceOnUse">
            <path d="M 60 0 L 0 0 0 60" fill="none" stroke="currentColor" strokeWidth="0.75" strokeDasharray="2 4" />
          </pattern>
          <linearGradient id="track-fade" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#2563eb" stopOpacity="0" />
            <stop offset="50%" stopColor="#38bdf8" stopOpacity="0.4" />
            <stop offset="100%" stopColor="#2563eb" stopOpacity="0" />
          </linearGradient>
        </defs>
        <rect width="100%" height="100%" fill="url(#automotive-grid)" />
      </svg>

      {/* Subtle Horizontal Telemetry Beam */}
      <div className="absolute top-2/3 left-0 w-full h-[1px] bg-gradient-to-r from-transparent via-accent/20 to-transparent automotive-track-line" />
    </div>
  );
};

export default AutomotiveBackground;
