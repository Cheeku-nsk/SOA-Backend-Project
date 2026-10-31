const SectionTitle = ({ subtitle, title, description, centered = true }) => {
  return (
    <div className={`mb-12 max-w-3xl ${centered ? 'text-center mx-auto' : ''}`}>
      {subtitle && (
        <h2 className="text-sm font-bold text-accent uppercase tracking-wider mb-3">
          {subtitle}
        </h2>
      )}
      <h3 className="text-3xl md:text-4xl font-bold text-white mb-6">
        {title}
      </h3>
      {description && (
        <p className="text-slate-400 text-lg leading-relaxed">
          {description}
        </p>
      )}
    </div>
  );
};

export default SectionTitle;
