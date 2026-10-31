import { FaLongArrowAltRight, FaLongArrowAltDown } from 'react-icons/fa';

const WorkflowSection = () => {
  const steps = [
    { num: '01', title: 'Book a Slot', desc: 'Customer requests a convenient service date and time.' },
    { num: '02', title: 'Vehicle Serviced', desc: 'Technicians perform the required maintenance or repairs.' },
    { num: '03', title: 'Update Records', desc: 'Service details and mechanic notes are logged in the system.' },
    { num: '04', title: 'Generate Invoice', desc: 'A final bill is generated automatically for the customer.' },
  ];

  return (
    <section className="py-24 bg-slate-900 border-y border-surface-border relative overflow-hidden">
      {/* Decorative Glow */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-3/4 h-64 bg-primary/10 blur-[100px] rounded-full pointer-events-none"></div>

      <div className="container mx-auto px-4 md:px-6 relative z-10">
        <div className="text-center max-w-2xl mx-auto mb-16">
          <h2 className="text-sm font-bold text-accent uppercase tracking-wider mb-3">How It Works</h2>
          <h3 className="text-3xl md:text-4xl font-bold text-white mb-6">Seamless Service Lifecycle</h3>
        </div>

        <div className="flex flex-col lg:flex-row items-center justify-between gap-8 max-w-5xl mx-auto">
          {steps.map((step, index) => (
            <div key={index} className="flex flex-col lg:flex-row items-center w-full lg:w-auto">
              
              {/* Step Card with Motion Interactive Treatment */}
              <div className="flex flex-col items-center text-center relative w-full lg:w-48 group motion-card-interactive p-3 rounded-2xl">
                <div className="w-20 h-20 rounded-full bg-surface border border-surface-border flex items-center justify-center mb-6 relative z-10 group-hover:border-accent group-hover:shadow-[0_0_20px_rgba(14,165,233,0.3)] transition-all duration-300 shadow-lg">
                  <span className="text-2xl font-bold text-slate-300 group-hover:text-accent transition-colors duration-200">{step.num}</span>
                </div>
                <h4 className="text-white font-semibold mb-2 text-lg">{step.title}</h4>
                <p className="text-slate-400 text-sm leading-relaxed">{step.desc}</p>
              </div>

              {/* Arrow Indicator */}
              {index < steps.length - 1 && (
                <div className="text-surface-border py-6 lg:py-0 lg:px-4 flex items-center justify-center">
                  <FaLongArrowAltRight className="text-3xl hidden lg:block transition-colors group-hover:text-accent" />
                  <FaLongArrowAltDown className="text-3xl lg:hidden" />
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default WorkflowSection;
