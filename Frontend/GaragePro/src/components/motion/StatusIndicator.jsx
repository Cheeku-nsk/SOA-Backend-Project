/**
 * StatusIndicator Component
 * Features a subtle 2.6s breathing pulse on the status dot.
 * Preserves actual status text and color without fabricating state.
 */
const StatusIndicator = ({ 
  status = "Active", 
  variant = "emerald", // "emerald" | "accent" | "amber" | "red"
  className = "" 
}) => {
  const variantStyles = {
    emerald: {
      dot: "bg-emerald-400 status-indicator-breathe",
      text: "text-emerald-400",
      bg: "bg-emerald-500/10 border-emerald-500/20",
    },
    accent: {
      dot: "bg-accent status-indicator-accent",
      text: "text-accent",
      bg: "bg-accent/10 border-accent/20",
    },
    amber: {
      dot: "bg-amber-400",
      text: "text-amber-400",
      bg: "bg-amber-500/10 border-amber-500/20",
    },
    red: {
      dot: "bg-rose-500",
      text: "text-rose-400",
      bg: "bg-rose-500/10 border-rose-500/20",
    }
  };

  const style = variantStyles[variant] || variantStyles.emerald;

  return (
    <span className={`inline-flex items-center gap-2 ${className}`}>
      <span className="relative flex h-2.5 w-2.5 items-center justify-center">
        <span className={`h-2 w-2 rounded-full ${style.dot}`} />
      </span>
      <span className={`text-xs font-semibold uppercase tracking-wider ${style.text}`}>
        {status}
      </span>
    </span>
  );
};

export default StatusIndicator;
