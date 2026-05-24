import { useState } from 'react';
import { login, signup } from '../../api/auth';
import { AuthResponse } from '../../types';

interface Props {
  onAuthenticated: (auth: AuthResponse) => void;
  onBack?: () => void;
}

export function AuthScreen({ onAuthenticated, onBack }: Props) {
  const [mode, setMode] = useState<'login' | 'signup'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Generate deterministic drifting particle coordinates in React state
  const [particles] = useState(() =>
    Array.from({ length: 18 }, (_, i) => ({
      id: i,
      left: `${Math.random() * 100}%`,
      bottom: `${Math.random() * 25}%`,
      size: `${Math.random() * 4 + 2}px`,
      delay: `${Math.random() * 5}s`,
      duration: `${Math.random() * 6 + 6}s`,
    }))
  );

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const auth = mode === 'login'
        ? await login({ email, password })
        : await signup({ email, password, displayName: displayName || email.split('@')[0] });
      onAuthenticated(auth);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex bg-dark-950 overflow-hidden relative">
      
      {/* Background patterns */}
      <div className="absolute inset-0 bg-grid-pattern opacity-15 pointer-events-none" />

      {/* Left Column (Isometric showcase & Particle effect) - Hidden on mobile */}
      <div className="hidden md:flex md:w-[45%] lg:w-[50%] bg-dark-900 border-r border-dark-500/50 flex-col items-center justify-center p-10 relative overflow-hidden">
        
        {/* Glow gradients */}
        <div className="absolute w-[60%] h-[60%] bg-violet-600/10 rounded-full blur-[100px] top-1/4 left-1/4 animate-pulse-slow pointer-events-none" />
        
        {/* Floating particles wrapper */}
        <div className="absolute inset-0 overflow-hidden pointer-events-none">
          {particles.map(p => (
            <div
              key={p.id}
              className="particle-dot bg-violet-500/30 shadow shadow-cyan-400/20"
              style={{
                left: p.left,
                bottom: p.bottom,
                width: p.size,
                height: p.size,
                animationDelay: p.delay,
                animationDuration: p.duration,
              }}
            />
          ))}
        </div>

        {/* 3D stacked code cards illustration */}
        <div className="relative w-80 h-80 perspective-1000 preserve-3d animate-float select-none">
          
          {/* Base card */}
          <div className="absolute top-12 left-6 w-64 h-40 bg-dark-850/80 border border-dark-500/80 rounded-xl p-4 shadow-xl transform rotate-x-[30deg] -rotate-y-[20deg] translate-z-0 transition duration-300">
            <div className="w-12 h-2 rounded bg-dark-600 mb-3" />
            <div className="space-y-2">
              <div className="w-full h-1.5 rounded bg-dark-700" />
              <div className="w-4/5 h-1.5 rounded bg-dark-700" />
              <div className="w-5/6 h-1.5 rounded bg-dark-700" />
            </div>
          </div>

          {/* Mid card */}
          <div className="absolute top-6 left-12 w-64 h-40 bg-dark-800/90 border border-violet-500/30 rounded-xl p-4 shadow-2xl transform rotate-x-[30deg] -rotate-y-[20deg] translate-z-[12px] transition duration-300">
            <div className="flex items-center gap-1.5 mb-3">
              <div className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse-slow" />
              <div className="w-16 h-2 rounded bg-violet-500/50" />
            </div>
            <div className="space-y-2">
              <div className="w-11/12 h-1.5 rounded bg-dark-700" />
              <div className="w-3/4 h-1.5 rounded bg-dark-700" />
            </div>
          </div>

          {/* Top card */}
          <div className="absolute top-0 left-18 w-64 h-40 bg-dark-700 border border-cyan-500/40 rounded-xl p-4 shadow-2xl transform rotate-x-[30deg] -rotate-y-[20deg] translate-z-[24px] transition duration-300 hover:scale-105">
            <div className="flex items-center justify-between mb-3 border-b border-dark-500 pb-1.5">
              <span className="text-[9px] font-mono text-cyan-400 font-bold uppercase tracking-wider">Semantic Index</span>
              <div className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
            </div>
            <div className="space-y-2">
              <div className="w-full h-1.5 rounded bg-dark-600" />
              <div className="w-2/3 h-1.5 rounded bg-dark-600" />
              <div className="w-4/5 h-1.5 rounded bg-dark-600" />
            </div>
          </div>
        </div>

        <div className="text-center space-y-2.5 mt-8 max-w-sm relative z-10 select-none">
          <h3 className="text-base font-bold text-[#e6edf3]">Semantic codebase exploration</h3>
          <p className="text-xs text-dark-350 leading-relaxed">
            Ingest repositories in minutes. Speak to your functions, modules, and configurations naturally.
          </p>
        </div>
      </div>

      {/* Right Column (Form section) */}
      <div className="flex-1 flex flex-col items-center justify-center p-6 relative z-10">
        
        {/* Back home arrow button */}
        {onBack && (
          <button
            onClick={onBack}
            className="absolute top-6 left-6 text-xs text-dark-350 hover:text-white flex items-center gap-1.5 transition active:scale-95"
          >
            ← Back to Home
          </button>
        )}

        <div className="w-full max-w-md bg-dark-850/60 backdrop-blur-xl border border-dark-500/60 rounded-2xl p-6 sm:p-8 shadow-2xl space-y-6">
          
          <div className="space-y-2 select-none text-center md:text-left">
            <h1 className="text-xl font-black text-[#e6edf3] tracking-tight uppercase">Code-Xray</h1>
            <p className="text-xs text-dark-450">Sign in to unlock interactive repo indexing & memory-backed conversations.</p>
          </div>

          {/* Mode Switcher */}
          <div className="flex bg-dark-900 border border-dark-500/50 rounded-xl p-1 relative">
            <button
              onClick={() => setMode('login')}
              className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-all duration-200 ${mode === 'login' ? 'bg-dark-700 border border-dark-500/50 text-[#e6edf3]' : 'text-dark-350 hover:text-white'}`}
            >
              Sign In
            </button>
            <button
              onClick={() => setMode('signup')}
              className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-all duration-200 ${mode === 'signup' ? 'bg-dark-700 border border-dark-500/50 text-[#e6edf3]' : 'text-dark-350 hover:text-white'}`}
            >
              Sign Up
            </button>
          </div>

          {/* Submit form */}
          <form onSubmit={submit} className="space-y-4">
            {mode === 'signup' && (
              <div className="space-y-1.5 animate-fade-in-up">
                <label className="text-[10px] font-bold text-dark-350 uppercase tracking-wide block px-0.5">Display Name</label>
                <input
                  className="input-field"
                  placeholder="John Doe"
                  value={displayName}
                  onChange={e => setDisplayName(e.target.value)}
                  required
                />
              </div>
            )}
            
            <div className="space-y-1.5">
              <label className="text-[10px] font-bold text-dark-350 uppercase tracking-wide block px-0.5">Email address</label>
              <input
                className="input-field"
                type="email"
                placeholder="john@example.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-[10px] font-bold text-dark-350 uppercase tracking-wide block px-0.5">Password</label>
              <input
                className="input-field"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={e => setPassword(e.target.value)}
                required
                minLength={8}
              />
            </div>

            {error && (
              <div className="p-3 bg-red-950/20 border border-red-500/30 rounded-xl text-xs text-red-400">
                ⚠️ {error}
              </div>
            )}

            <button
              type="submit"
              className="btn-primary w-full py-3 text-xs font-bold tracking-wide mt-2"
              disabled={loading}
            >
              {loading ? (
                <div className="flex items-center justify-center gap-2">
                  <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  <span>Processing...</span>
                </div>
              ) : mode === 'login' ? (
                'Sign In'
              ) : (
                'Create Account'
              )}
            </button>
          </form>
        </div>
      </div>
      
    </div>
  );
}
