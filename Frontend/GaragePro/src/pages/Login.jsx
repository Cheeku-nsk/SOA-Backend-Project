import LoginForm from '../components/LoginForm';

const Login = () => {
  return (
    <div className="min-h-[80vh] bg-slate-950 flex items-center justify-center py-12 px-4 relative overflow-hidden">
      {/* Background Decor */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-primary/10 blur-[150px] rounded-full pointer-events-none"></div>
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-accent/10 blur-[150px] rounded-full pointer-events-none"></div>
      
      <div className="w-full relative z-10">
        <LoginForm />
      </div>
    </div>
  );
};

export default Login;
