import * as Dialog from '@radix-ui/react-dialog';
import { FaTimes } from 'react-icons/fa';
import BookingForm from './BookingForm';
import { useAuth } from '../context/AuthContext';

const BookingDialog = ({ open, onOpenChange }) => {
  const { user } = useAuth();

  // Admin and Technician cannot book services
  if (user?.role === 'ADMIN' || user?.role === 'TECHNICIAN') {
    return null;
  }

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/60 backdrop-blur-sm z-[100] radix-overlay-motion" />
        <Dialog.Content className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-slate-900 border border-surface-border rounded-xl p-0 w-[95vw] max-w-3xl max-h-[90vh] overflow-y-auto z-[101] shadow-2xl radix-content-motion focus:outline-none">
          
          <div className="sticky top-0 bg-slate-900/95 backdrop-blur z-20 border-b border-surface-border p-6 flex items-center justify-between">
            <div>
              <Dialog.Title className="text-2xl font-bold text-white">
                Book a Service
              </Dialog.Title>
              <Dialog.Description className="text-slate-400 mt-1">
                Reserve a service bay for your vehicle.
              </Dialog.Description>
            </div>
            <Dialog.Close asChild>
              <button 
                className="text-slate-400 hover:text-white transition-colors p-2 bg-slate-800 rounded-full hover:bg-slate-700 cursor-pointer" 
                aria-label="Close"
              >
                <FaTimes />
              </button>
            </Dialog.Close>
          </div>

          <div className="p-2 md:p-6">
            <BookingForm onSuccess={() => onOpenChange(false)} />
          </div>

        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
};

export default BookingDialog;
