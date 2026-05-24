import { useEffect, useState } from 'react';
// @ts-ignore
import heroGraphic from '../../assets/hero_graphic.png';

interface Props {
  onStartAuth: () => void;
}

export function LandingPage({ onStartAuth }: Props) {
  const [typedText, setTypedText] = useState('');
  const fullText = `1. Ingestion: Walk files & parse code chunks.
2. Embeddings: Convert code to 1536-dimensional vectors.
3. RAG: Search semantically to stream answers.`;

  // Simulated AI typing micro-animation in hero mockup
  useEffect(() => {
    let index = 0;
    const interval = setInterval(() => {
      setTypedText(fullText.substring(0, index));
      index++;
      if (index > fullText.length) {
        setTimeout(() => {
          index = 0; // restart typing loop
        }, 3000);
      }
    }, 45);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="h-screen bg-dark-950 text-dark-200 overflow-y-auto select-none font-sans relative scroll-smooth">
      
      {/* Background grids and glowing blobs */}
      <div className="absolute inset-0 bg-grid-pattern opacity-30 pointer-events-none" />
      <div className="absolute top-[-10%] left-[-10%] w-[50%] h-[50%] bg-violet-600/10 rounded-full blur-[120px] pointer-events-none animate-pulse-slow" />
      <div className="absolute bottom-[10%] right-[-10%] w-[50%] h-[50%] bg-cyan-500/10 rounded-full blur-[120px] pointer-events-none" />

      {/* Glass header */}
      <header className="sticky top-0 z-50 backdrop-blur-md bg-dark-950/70 border-b border-dark-500/40 px-6 py-4 flex items-center justify-between shadow-lg shadow-black/10">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-violet-600 to-cyan-500 flex items-center justify-center shadow-lg shadow-violet-500/20 shrink-0">
            <span className="text-white text-xs font-bold font-mono">CX</span>
          </div>
          <span className="text-sm font-extrabold tracking-widest text-[#e6edf3] uppercase font-mono select-none">Code-Xray</span>
        </div>
        <div className="flex items-center gap-4">
          <a href="#features" className="text-xs text-dark-350 hover:text-white transition duration-150">Features</a>
          <a href="#tech" className="text-xs text-dark-350 hover:text-white transition duration-150 mr-4">Stack</a>
          <button
            onClick={onStartAuth}
            className="text-xs font-semibold text-white bg-dark-800 hover:bg-dark-700 border border-dark-500 px-4 py-2 rounded-lg transition duration-200 active:scale-95 shadow-md"
          >
            Sign In
          </button>
        </div>
      </header>

      {/* Hero section */}
      <section className="max-w-6xl mx-auto px-6 pt-16 pb-20 grid grid-cols-1 md:grid-cols-2 gap-12 items-center relative z-10">
        
        {/* Left Column Text */}
        <div className="space-y-6 text-left">
          <div className="inline-flex items-center gap-2 bg-violet-950/30 border border-violet-500/30 px-3 py-1 rounded-full text-violet-400 text-[10px] font-bold uppercase tracking-wider animate-pulse-slow">
            ⚡ Now Powered by true hybrid search
          </div>
          <h2 className="text-4xl md:text-5xl font-black text-[#e6edf3] tracking-tight leading-[1.1]">
            Your Codebase, <br />
            <span className="bg-gradient-to-r from-violet-400 via-fuchsia-400 to-cyan-400 bg-clip-text text-transparent">
              Now Interactive.
            </span>
          </h2>
          <p className="text-sm text-dark-300 leading-relaxed max-w-lg">
            Clone, index, and chat with your repositories in real-time. Code-Xray uses language-aware chunking and hybrid retrieval to trace implementation flows and explain complex systems instantly.
          </p>
          <div className="flex flex-wrap items-center gap-4 pt-2">
            <button
              onClick={onStartAuth}
              className="bg-gradient-to-r from-violet-600 to-cyan-600 hover:from-violet-500 hover:to-cyan-500 text-white font-bold text-xs px-6 py-3.5 rounded-xl transition duration-200 active:scale-95 shadow-lg shadow-violet-500/20"
            >
              Get Started for Free
            </button>
            <a
              href="#features"
              className="text-xs font-bold text-dark-300 hover:text-white border border-dark-500 hover:border-dark-400 px-6 py-3.5 rounded-xl transition duration-200 bg-dark-900/50"
            >
              Explore Features
            </a>
          </div>
        </div>

        {/* Right Column 3D Showcase Panel */}
        <div className="relative animate-float">
          {/* Main Visual Graphic */}
          <div className="rounded-2xl border border-dark-500/50 bg-dark-900/40 backdrop-blur p-1.5 shadow-2xl relative overflow-hidden group">
            <img
              src={heroGraphic}
              alt="Futuristic Coding Graph"
              className="rounded-xl w-full object-cover aspect-[4/3] opacity-80 group-hover:scale-[1.02] transition-transform duration-500"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-dark-950 via-dark-950/20 to-transparent" />
          </div>

          {/* Interactive Mockup Chat Panel Overlay */}
          <div className="relative mt-6 md:absolute md:bottom-[-30px] md:left-[-30px] md:right-8 md:mt-0 bg-dark-800/90 backdrop-blur-xl border border-dark-500 rounded-xl p-4 shadow-2xl space-y-3 max-w-sm mx-auto md:max-w-none">
            <div className="flex items-center gap-2 border-b border-dark-500 pb-2">
              <div className="w-2.5 h-2.5 rounded-full bg-red-500/80" />
              <div className="w-2.5 h-2.5 rounded-full bg-yellow-500/80" />
              <div className="w-2.5 h-2.5 rounded-full bg-green-500/80" />
              <span className="text-[9px] font-mono text-dark-400 ml-2">code-xray-chat.log</span>
            </div>
            
            {/* User message */}
            <div className="flex items-start gap-2.5">
              <div className="w-5 h-5 rounded bg-violet-600 flex items-center justify-center text-white text-[9px] font-bold">U</div>
              <p className="text-[11px] text-dark-250 font-medium">Explain the index process.</p>
            </div>

            {/* AI Response with typing effect */}
            <div className="flex items-start gap-2.5 pt-1">
              <div className="w-5 h-5 rounded bg-gradient-to-br from-violet-600 to-cyan-500 flex items-center justify-center text-white text-[9px] font-bold">AI</div>
              <div className="flex-1 bg-dark-900 border border-dark-500/50 rounded-lg p-2.5 text-[10px] text-emerald-400 font-mono leading-relaxed select-text whitespace-pre-line">
                {typedText}
                <span className="inline-block w-1 h-3.5 bg-violet-400 animate-blink ml-0.5 align-text-bottom" />
              </div>
            </div>
          </div>
        </div>

      </section>

      {/* Key features grid */}
      <section id="features" className="max-w-6xl mx-auto px-6 py-20 relative z-10 border-t border-dark-500/30">
        <div className="text-center space-y-3 mb-16">
          <p className="text-[10px] font-bold text-violet-400 uppercase tracking-widest">Built for developer velocity</p>
          <h3 className="text-2xl md:text-3xl font-black text-[#e6edf3]">Explore Code-Xray's Capabilities</h3>
          <p className="text-xs text-dark-350 max-w-md mx-auto leading-relaxed">
            Everything you need to navigate, document, and analyze large codebases in seconds.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[
            {
              title: '🚀 Smart Code Ingestion',
              description: 'Clones repository branches programmatic JGit, walk directories, applies intelligent file filters, and splits files at method/class syntax boundaries.',
              border: 'group-hover:border-violet-500/40',
              glow: 'group-hover:shadow-violet-500/5',
            },
            {
              title: '🔍 True Hybrid Retrieval',
              description: 'Combines vector semantic calculations (cosine similarity) with PostgreSQL Simple full-text keyword indexing to locate symbols, route matches, and configs.',
              border: 'group-hover:border-cyan-500/40',
              glow: 'group-hover:shadow-cyan-500/5',
            },
            {
              title: '🖥️ Side-by-Side Split Workspace',
              description: 'View repository summary modules, documentation markdown layers, and highlighted code citation snippets side-by-side while maintaining your chat stream.',
              border: 'group-hover:border-fuchsia-500/40',
              glow: 'group-hover:shadow-fuchsia-500/5',
            },
          ].map((feat, idx) => (
            <div
              key={idx}
              className="bg-dark-900 border border-dark-500/60 rounded-2xl p-6 text-left space-y-3 group hover:bg-dark-800 transition-all duration-300 shadow-md hover:-translate-y-1 hover:shadow-lg"
            >
              <h4 className="text-sm font-bold text-[#e6edf3] tracking-wide">{feat.title}</h4>
              <p className="text-xs text-dark-300 leading-relaxed">{feat.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Technology stack section */}
      <section id="tech" className="max-w-6xl mx-auto px-6 py-16 border-t border-dark-500/30 relative z-10">
        <div className="text-center space-y-3 mb-12">
          <p className="text-[10px] font-bold text-cyan-400 uppercase tracking-widest">Architectural Foundation</p>
          <h3 className="text-2xl font-black text-[#e6edf3]">Our Production Stack</h3>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-6 gap-4">
          {[
            { name: 'Java 21', detail: 'Modern JDK features', color: 'text-orange-400' },
            { name: 'Spring Boot', detail: 'WebFlux & Security', color: 'text-emerald-400' },
            { name: 'Spring AI', detail: 'AI framework', color: 'text-green-400' },
            { name: 'PGVector', detail: 'Vector storage', color: 'text-cyan-400' },
            { name: 'OpenAI API', detail: 'embeddings & LLM', color: 'text-purple-400' },
            { name: 'React + TS', detail: 'Sleek component UI', color: 'text-sky-400' },
          ].map((tech, idx) => (
            <div
              key={idx}
              className="bg-dark-900 border border-dark-500/40 p-4 rounded-xl text-center space-y-1 hover:border-dark-400 transition"
            >
              <span className={`text-xs font-bold font-mono ${tech.color}`}>{tech.name}</span>
              <p className="text-[9px] text-dark-400 truncate">{tech.detail}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Footer banner */}
      <footer className="border-t border-dark-500/40 bg-dark-900 py-12 relative z-10 text-center">
        <div className="max-w-xl mx-auto px-6 space-y-6">
          <h3 className="text-lg font-bold text-[#e6edf3]">Ready to talk to your code?</h3>
          <button
            onClick={onStartAuth}
            className="bg-gradient-to-r from-violet-600 to-cyan-600 hover:from-violet-500 hover:to-cyan-500 text-white font-bold text-xs px-8 py-4 rounded-xl transition duration-200 active:scale-95 shadow-lg shadow-violet-500/10"
          >
            Start Ingesting Now
          </button>
          <p className="text-[10px] text-dark-400 font-mono">Code-Xray © 2026 · Powered by Spring AI & React</p>
        </div>
      </footer>

    </div>
  );
}
