import { useState } from 'react';
import { FaUser, FaEnvelope, FaPaperPlane, FaSpinner } from 'react-icons/fa';

const ContactForm = () => {
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    
    // UI-ONLY Simulation
    setTimeout(() => {
      setIsSubmitting(false);
      alert("Message sent successfully! (Frontend only)");
    }, 1500);
  };

  return (
    <form onSubmit={handleSubmit} className="glass-panel p-8 relative overflow-hidden">
      <div className="absolute -bottom-20 -left-20 w-48 h-48 bg-primary/20 blur-[60px] rounded-full pointer-events-none"></div>
      
      <div className="relative z-10 space-y-6">
        <div>
          <label className="form-label">Your Name</label>
          <div className="relative">
            <FaUser className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" />
            <input type="text" className="input-field pl-11" placeholder="Jane Doe" required />
          </div>
        </div>
        
        <div>
          <label className="form-label">Email Address</label>
          <div className="relative">
            <FaEnvelope className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" />
            <input type="email" className="input-field pl-11" placeholder="jane@example.com" required />
          </div>
        </div>

        <div>
          <label className="form-label">Subject</label>
          <select className="input-field appearance-none bg-[url('data:image/svg+xml;charset=US-ASCII,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22292.4%22%20height%3D%22292.4%22%3E%3Cpath%20fill%3D%22%2394a3b8%22%20d%3D%22M287%2069.4a17.6%2017.6%200%200%200-13-5.4H18.4c-5%200-9.3%201.8-12.9%205.4A17.6%2017.6%200%200%200%200%2082.2c0%205%201.8%209.3%205.4%2012.9l128%20127.9c3.6%203.6%207.8%205.4%2012.8%205.4s9.2-1.8%2012.8-5.4L287%2095c3.5-3.5%205.4-7.8%205.4-12.8%200-5-1.9-9.2-5.5-12.8z%22%2F%3E%3C%2Fsvg%3E')] bg-no-repeat bg-[position:right_1rem_center] bg-[length:0.75rem_auto]" required>
            <option value="">Select a subject</option>
            <option value="support">General Support</option>
            <option value="billing">Billing Inquiry</option>
            <option value="partnership">Partnership / Fleet Deal</option>
            <option value="other">Other</option>
          </select>
        </div>

        <div>
          <label className="form-label">Message</label>
          <textarea 
            className="input-field min-h-[120px] resize-y" 
            placeholder="How can we help you?"
            required
          ></textarea>
        </div>

        <button 
          type="submit" 
          className="btn-primary motion-btn-interactive w-full flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
          disabled={isSubmitting}
        >
          {isSubmitting ? (
            <>
              <FaSpinner className="animate-spin" /> Sending...
            </>
          ) : (
            <>
              <span>Send Message</span>
              <FaPaperPlane className="motion-arrow text-sm" />
            </>
          )}
        </button>
      </div>
    </form>
  );
};

export default ContactForm;
