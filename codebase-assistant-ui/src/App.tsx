import { ReactNode, useEffect, useMemo, useState } from 'react';
import { IngestForm } from './components/Ingestion/IngestForm';
import { ChatWindow } from './components/Chat/ChatWindow';
import { AuthScreen } from './components/Auth/AuthScreen';
import { LandingPage } from './components/Landing/LandingPage';
import {
  AuthResponse,
  ChatMessage,
  Citation,
  ConversationView,
  IngestResultData,
  MessageView,
  RepositoryDocsView,
  RepositorySummaryView,
  RepositoryView,
  UserView
} from './types';
import { createConversation, deleteConversation, listConversations, listMessages } from './api/conversations';
import { listRepositories } from './api/repositories';
import { me, logout } from './api/auth';
import { getAccessToken } from './api/http';
import { getRepositoryDocs, getRepositorySummary } from './api/insights';

const SUGGESTED_QUESTIONS = [
  'How does authentication work?',
  'Explain the main entry point',
  'How are errors handled?',
  'What does the service layer do?',
  'Where is the database configured?',
];

export default function App() {
  const [activeRepoUrl, setActiveRepoUrl] = useState('');
  const [ingestResult, setIngestResult] = useState<IngestResultData | null>(null);
  const [suggestedQuestion, setSuggestedQuestion] = useState('');
  const [activeRepository, setActiveRepository] = useState<RepositoryView | null>(null);
  const [conversations, setConversations] = useState<ConversationView[]>([]);
  const [activeConversationId, setActiveConversationId] = useState<string>('');
  const [conversationMessages, setConversationMessages] = useState<ChatMessage[]>([]);
  const [user, setUser] = useState<UserView | null>(null);
  const [authBootstrapped, setAuthBootstrapped] = useState(false);
  const [showAuth, setShowAuth] = useState(false);

  // Layout states for collapsing and resizing sidebars
  const [leftSidebarWidth, setLeftSidebarWidth] = useState(280);
  const [leftSidebarCollapsed, setLeftSidebarCollapsed] = useState(false);
  const [rightSidebarWidth, setRightSidebarWidth] = useState(420);
  const [rightSidebarCollapsed, setRightSidebarCollapsed] = useState(true);
  const [isDraggingLeft, setIsDraggingLeft] = useState(false);
  const [isDraggingRight, setIsDraggingRight] = useState(false);
  const [isSmallScreen, setIsSmallScreen] = useState(window.innerWidth < 1024);

  // Tabs for the right panel insights & citations
  const [rightTab, setRightTab] = useState<'summary' | 'docs' | 'citation'>('summary');
  const [selectedCitation, setSelectedCitation] = useState<Citation | null>(null);

  const [summary, setSummary] = useState<RepositorySummaryView | null>(null);
  const [docs, setDocs] = useState<RepositoryDocsView | null>(null);
  const [insightsLoading, setInsightsLoading] = useState(false);
  const [insightsError, setInsightsError] = useState('');

  useEffect(() => {
    const token = getAccessToken();
    if (!token) {
      setAuthBootstrapped(true);
      return;
    }
    void me()
      .then(setUser)
      .catch(() => logout())
      .finally(() => setAuthBootstrapped(true));
  }, []);

  // Fetch insights when right panel opens or tab changes
  useEffect(() => {
    if (!activeRepository || rightSidebarCollapsed || (rightTab !== 'summary' && rightTab !== 'docs')) return;
    void loadInsights(activeRepository.id, rightTab);
  }, [activeRepository, rightTab, rightSidebarCollapsed]);

  // Global shortcut to toggle Left Sidebar: Ctrl+B
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'b') {
        e.preventDefault();
        setLeftSidebarCollapsed(prev => {
          const next = !prev;
          if (!next && isSmallScreen) {
            setRightSidebarCollapsed(true);
          }
          return next;
        });
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isSmallScreen]);

  // Monitor window size to adapt layout responsively
  useEffect(() => {
    const handleResize = () => {
      const small = window.innerWidth < 1024;
      setIsSmallScreen(small);
      if (small) {
        setLeftSidebarCollapsed(true);
        setRightSidebarCollapsed(true);
      }
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const handleAuthenticated = (auth: AuthResponse) => {
    setUser(auth.user);
  };

  const handleLogout = () => {
    logout();
    setUser(null);
    setActiveRepoUrl('');
    setIngestResult(null);
    setActiveRepository(null);
    setConversations([]);
    setActiveConversationId('');
    setConversationMessages([]);
    setSummary(null);
    setDocs(null);
    setRightSidebarCollapsed(true);
    setShowAuth(false);
  };

  const handleIngestSuccess = async (repoUrl: string, result: IngestResultData) => {
    setActiveRepoUrl(repoUrl);
    setIngestResult(result);
    await syncRepositoryAndConversations(repoUrl);
    setSummary(null);
    setDocs(null);
  };

  const handleSuggestedQuestion = (q: string) => {
    if (!activeRepoUrl) return;
    setSuggestedQuestion(q);
    setTimeout(() => setSuggestedQuestion(''), 100);
  };

  const repoShortName = activeRepoUrl.replace('https://github.com/', '');

  useEffect(() => {
    if (!activeConversationId) {
      setConversationMessages([]);
      return;
    }
    void loadConversationMessages(activeConversationId);
  }, [activeConversationId]);

  const canChat = Boolean(activeRepoUrl && activeConversationId);

  async function syncRepositoryAndConversations(repoUrl: string) {
    const repositories = await listRepositories();
    const matched = repositories.find(r => normalizeRepoUrl(r.repoUrl) === normalizeRepoUrl(repoUrl)) || null;
    setActiveRepository(matched);
    if (!matched) {
      setConversations([]);
      setActiveConversationId('');
      return;
    }

    const list = await listConversations(matched.id);
    if (list.length === 0) {
      const created = await createConversation(matched.id, 'New Chat');
      setConversations([created]);
      setActiveConversationId(created.id);
    } else {
      setConversations(list);
      setActiveConversationId(prev => prev && list.some(c => c.id === prev) ? prev : list[0].id);
    }
  }

  async function loadConversationMessages(conversationId: string) {
    const messages = await listMessages(conversationId);
    setConversationMessages(mapMessages(messages));
  }

  async function loadInsights(repositoryId: string, tab: 'summary' | 'docs') {
    setInsightsLoading(true);
    setInsightsError('');
    try {
      if (tab === 'summary') {
        const data = await getRepositorySummary(repositoryId);
        setSummary(data);
      } else {
        const data = await getRepositoryDocs(repositoryId);
        setDocs(data);
      }
    } catch (err) {
      setInsightsError(err instanceof Error ? err.message : 'Failed to load insights');
    } finally {
      setInsightsLoading(false);
    }
  }

  async function handleNewConversation() {
    if (!activeRepository) return;
    const created = await createConversation(activeRepository.id, `Chat ${conversations.length + 1}`);
    setConversations(prev => [created, ...prev]);
    setActiveConversationId(created.id);
  }

  async function handleDeleteConversation(conversationId: string) {
    await deleteConversation(conversationId);
    const next = conversations.filter(c => c.id !== conversationId);
    setConversations(next);
    if (activeConversationId === conversationId) {
      setActiveConversationId(next[0]?.id || '');
    }
  }

  const handleSelectCitation = (citation: Citation) => {
    setSelectedCitation(citation);
    setRightTab('citation');
    setRightSidebarCollapsed(false);
    if (isSmallScreen) {
      setLeftSidebarCollapsed(true);
    }
  };

  // Left sidebar resizing logic
  const handleLeftResizeMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsDraggingLeft(true);
    const startWidth = leftSidebarWidth;
    const startX = e.clientX;

    const doDrag = (moveEvent: MouseEvent) => {
      const newWidth = startWidth + (moveEvent.clientX - startX);
      if (newWidth >= 200 && newWidth <= 450) {
        setLeftSidebarWidth(newWidth);
      }
    };

    const stopDrag = () => {
      setIsDraggingLeft(false);
      document.removeEventListener('mousemove', doDrag);
      document.removeEventListener('mouseup', stopDrag);
    };

    document.addEventListener('mousemove', doDrag);
    document.addEventListener('mouseup', stopDrag);
  };

  // Right sidebar resizing logic
  const handleRightResizeMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsDraggingRight(true);
    const startWidth = rightSidebarWidth;
    const startX = e.clientX;

    const doDrag = (moveEvent: MouseEvent) => {
      const newWidth = startWidth - (moveEvent.clientX - startX);
      if (newWidth >= 280 && newWidth <= 650) {
        setRightSidebarWidth(newWidth);
      }
    };

    const stopDrag = () => {
      setIsDraggingRight(false);
      document.removeEventListener('mousemove', doDrag);
      document.removeEventListener('mouseup', stopDrag);
    };

    document.addEventListener('mousemove', doDrag);
    document.addEventListener('mouseup', stopDrag);
  };

  const activeConversationTitle = useMemo(
    () => conversations.find(c => c.id === activeConversationId)?.title || 'Chat',
    [conversations, activeConversationId]
  );

  if (!authBootstrapped) {
    return (
      <div className="min-h-screen bg-dark-950 text-dark-300 flex flex-col items-center justify-center gap-4">
        <div className="w-10 h-10 border-4 border-violet-500 border-t-transparent rounded-full animate-spin" />
        <span className="text-sm font-medium tracking-wide text-dark-400">Bootstrapping Auth...</span>
      </div>
    );
  }

  if (!user) {
    if (!showAuth) {
      return <LandingPage onStartAuth={() => setShowAuth(true)} />;
    }
    return <AuthScreen onAuthenticated={handleAuthenticated} onBack={() => setShowAuth(false)} />;
  }

  return (
    <div className="flex flex-col h-screen bg-dark-950 overflow-hidden select-none">
      
      {/* Header bar */}
      <header className="flex items-center gap-4 px-5 py-3 bg-dark-900 border-b border-dark-500/50 shrink-0 shadow-lg shadow-black/20 z-10">
        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              setLeftSidebarCollapsed(prev => {
                const next = !prev;
                if (!next && isSmallScreen) {
                  setRightSidebarCollapsed(true);
                }
                return next;
              });
            }}
            className="p-1.5 rounded-lg bg-dark-800 hover:bg-dark-700 border border-dark-500 text-dark-300 hover:text-white transition-all duration-150 active:scale-95"
            title="Toggle Sidebar (Ctrl+B)"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              {leftSidebarCollapsed ? (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h8m-8 6h16" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </button>
          
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-violet-600 to-cyan-500 flex items-center justify-center shadow-lg shadow-violet-500/20 shrink-0">
            <span className="text-white text-xs font-bold font-mono">CX</span>
          </div>
          <h1 className="text-sm font-bold gradient-text tracking-tight uppercase shrink-0 hidden min-[480px]:block">Code-Xray</h1>
        </div>

        <div className="h-4 w-px bg-dark-500 hidden sm:block" />
        <span className="text-xs text-dark-400 hidden sm:inline-block">Signed in as <b className="text-dark-200">{user.displayName}</b></span>
        
        {activeRepoUrl && (
          <div className="ml-auto flex items-center gap-1.5 max-w-[110px] min-[400px]:max-w-[180px] sm:max-w-none">
            <div className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse-slow shrink-0" />
            <span className="text-xs font-mono text-cyan-400 bg-dark-800 border border-dark-500 px-2 py-0.5 sm:px-3 sm:py-1 rounded-full shadow-inner truncate">
              {repoShortName}
            </span>
          </div>
        )}
        
        <button
          onClick={handleLogout}
          className={`text-xs text-red-400 hover:text-red-300 border border-red-500/30 px-2 py-1 sm:px-3 sm:py-1.5 rounded-lg bg-red-950/10 hover:bg-red-950/20 transition-all duration-150 active:scale-95 ${!activeRepoUrl ? 'ml-auto' : ''}`}
        >
          Logout
        </button>
      </header>

      {/* Main layout frame */}
      <div className="flex flex-1 overflow-hidden relative">
        
        {/* Backdrop overlay for mobile drawer states */}
        {isSmallScreen && (!leftSidebarCollapsed || !rightSidebarCollapsed) && (
          <div
            onClick={() => {
              setLeftSidebarCollapsed(true);
              setRightSidebarCollapsed(true);
            }}
            className="absolute inset-0 bg-black/60 backdrop-blur-sm z-30 transition-all duration-300 pointer-events-auto"
          />
        )}

        {/* Left Sidebar */}
        <aside
          style={{ width: leftSidebarCollapsed ? 0 : (isSmallScreen ? 280 : leftSidebarWidth) }}
          className={`flex flex-col bg-dark-900 overflow-y-auto overflow-x-hidden select-none z-40
                     ${isDraggingLeft ? '' : 'transition-all duration-200 ease-in-out'}
                     ${leftSidebarCollapsed ? 'border-r-0 shadow-none' : (isSmallScreen ? 'border-r border-dark-500/80 shadow-2xl' : 'border-r border-dark-500/60')}
                     ${isSmallScreen ? 'absolute left-0 top-0 bottom-0 h-full' : 'relative shrink-0'}`}
        >
          <div className="w-[280px]"> {/* Keep inner controls static size */}
            <IngestForm onSuccess={handleIngestSuccess} />

            {ingestResult && (
              <div className="mx-3 mb-3 p-3 rounded-xl bg-dark-800/80 border border-dark-500/60 shadow-md">
                <p className="text-[10px] font-bold text-dark-400 uppercase tracking-widest mb-2.5">Index Stats</p>
                <div className="grid grid-cols-2 gap-2">
                  {[
                    { label: 'Files', value: ingestResult.totalFiles },
                    { label: 'Chunks', value: ingestResult.totalChunks },
                    { label: 'Duration', value: `${(ingestResult.durationMs / 1000).toFixed(1)}s` },
                    { label: 'Status', value: 'Ready' },
                  ].map(({ label, value }) => (
                    <div key={label} className="bg-dark-900 border border-dark-500/40 rounded-lg p-2 text-center">
                      <p className="text-[10px] text-dark-400">{label}</p>
                      <p className="text-xs font-semibold text-emerald-400 mt-0.5">{value}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Chats section */}
            <div className="px-3 pb-3">
              <div className="flex items-center justify-between mb-2.5 px-1">
                <p className="text-[10px] font-bold text-dark-400 uppercase tracking-widest">Chats</p>
                <button
                  onClick={() => void handleNewConversation()}
                  disabled={!activeRepository}
                  className="text-[10px] font-semibold text-cyan-400 hover:text-cyan-300 disabled:opacity-40 flex items-center gap-1"
                >
                  ＋ New
                </button>
              </div>
              <div className="space-y-1 max-h-48 overflow-y-auto pr-0.5">
                {conversations.length === 0 ? (
                  <p className="text-[11px] text-dark-400 italic px-1">No active conversations</p>
                ) : (
                  conversations.map(c => {
                    const isActive = c.id === activeConversationId;
                    return (
                      <div key={c.id} className="flex items-center gap-1 group">
                        <button
                          onClick={() => setActiveConversationId(c.id)}
                          className={`flex-1 text-left text-xs rounded-lg px-3 py-2 border transition-all duration-150 truncate ${
                            isActive
                              ? 'bg-gradient-to-r from-violet-950/40 to-dark-800 border-violet-500/40 text-violet-300 shadow-sm'
                              : 'bg-dark-800 border-dark-500/60 text-dark-300 hover:text-dark-100 hover:bg-dark-700/60'
                          }`}
                        >
                          💬 {c.title}
                        </button>
                        <button
                          onClick={() => void handleDeleteConversation(c.id)}
                          className="text-xs text-dark-400 hover:text-red-400 p-1 opacity-0 group-hover:opacity-100 transition-opacity duration-150"
                          title="Delete Chat"
                        >
                          ✕
                        </button>
                      </div>
                    );
                  })
                )}
              </div>
            </div>

            {/* Insights panel triggers */}
            <div className="px-3 pb-3">
              <p className="text-[10px] font-bold text-dark-400 uppercase tracking-widest mb-2.5 px-1">Repository Insights</p>
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => {
                    if (rightTab === 'summary' && !rightSidebarCollapsed) {
                      setRightSidebarCollapsed(true);
                    } else {
                      setRightTab('summary');
                      setRightSidebarCollapsed(false);
                      if (isSmallScreen) {
                        setLeftSidebarCollapsed(true);
                      }
                    }
                  }}
                  disabled={!activeRepository}
                  className={`text-xs font-semibold rounded-lg px-2.5 py-2 border transition-all duration-150 ${
                    !rightSidebarCollapsed && rightTab === 'summary'
                      ? 'border-cyan-500 text-cyan-300 bg-cyan-950/20 shadow-lg shadow-cyan-500/5'
                      : 'border-dark-500 text-dark-300 bg-dark-800 hover:bg-dark-700'
                  } disabled:opacity-30 disabled:cursor-not-allowed`}
                >
                  Summary
                </button>
                <button
                  onClick={() => {
                    if (rightTab === 'docs' && !rightSidebarCollapsed) {
                      setRightSidebarCollapsed(true);
                    } else {
                      setRightTab('docs');
                      setRightSidebarCollapsed(false);
                      if (isSmallScreen) {
                        setLeftSidebarCollapsed(true);
                      }
                    }
                  }}
                  disabled={!activeRepository}
                  className={`text-xs font-semibold rounded-lg px-2.5 py-2 border transition-all duration-150 ${
                    !rightSidebarCollapsed && rightTab === 'docs'
                      ? 'border-cyan-500 text-cyan-300 bg-cyan-950/20 shadow-lg shadow-cyan-500/5'
                      : 'border-dark-500 text-dark-300 bg-dark-800 hover:bg-dark-700'
                  } disabled:opacity-30 disabled:cursor-not-allowed`}
                >
                  Docs
                </button>
              </div>
            </div>

            {/* Suggested questions */}
            <div className="px-3 pb-4">
              <p className="text-[10px] font-bold text-dark-400 uppercase tracking-widest mb-2.5 px-1">Suggested Queries</p>
              <div className="space-y-1.5">
                {SUGGESTED_QUESTIONS.map(q => (
                  <button
                    key={q}
                    onClick={() => handleSuggestedQuestion(q)}
                    disabled={!canChat}
                    className="w-full text-left text-xs text-dark-300 bg-dark-800 hover:bg-dark-700/80
                               hover:text-white border border-dark-500/60 hover:border-violet-500/40
                               rounded-lg px-3 py-2 transition-all duration-150
                               disabled:opacity-30 disabled:cursor-not-allowed"
                  >
                    💡 {q}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </aside>

        {/* Left Resize Divider */}
        {!leftSidebarCollapsed && !isSmallScreen && (
          <div
            onMouseDown={handleLeftResizeMouseDown}
            onDoubleClick={() => setLeftSidebarWidth(280)}
            className="w-1 cursor-col-resize hover:bg-violet-500/40 bg-transparent transition-all duration-150 shrink-0 z-30"
          />
        )}

        {/* Center Panel (Active Chat View) */}
        <main className="flex-1 flex flex-col overflow-hidden bg-grid-pattern relative select-text">
          {canChat ? (
            <ChatWindow
              repoUrl={activeRepoUrl}
              suggestedQuestion={suggestedQuestion}
              conversationId={activeConversationId}
              initialMessages={conversationMessages}
              onSelectCitation={handleSelectCitation}
              key={activeConversationId}
            />
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-center p-8 bg-dark-950/60 backdrop-blur-sm">
              <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-violet-600/10 to-cyan-500/10 border border-violet-500/20 flex items-center justify-center mb-4 shadow-lg shadow-violet-500/5 animate-pulse-slow">
                <svg className="w-8 h-8 text-violet-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                </svg>
              </div>
              <h2 className="text-base font-semibold text-[#e6edf3] mb-1">Welcome to Code-Xray</h2>
              <p className="text-xs text-dark-400 max-w-sm">
                {activeRepoUrl
                  ? 'Select or create a chat session in the sidebar to start asking questions.'
                  : 'Start by indexing a repository using the form on the left.'}
              </p>
            </div>
          )}

          {canChat && (
            <div className="px-4 py-1.5 text-[10px] text-dark-400 border-t border-dark-500 bg-dark-900/80 shrink-0 font-mono">
              Active conversation: {activeConversationTitle}
            </div>
          )}
        </main>

        {/* Right Resize Divider */}
        {!rightSidebarCollapsed && !isSmallScreen && (
          <div
            onMouseDown={handleRightResizeMouseDown}
            onDoubleClick={() => setRightSidebarWidth(420)}
            className="w-1 cursor-col-resize hover:bg-violet-500/40 bg-transparent transition-all duration-150 shrink-0 z-30"
          />
        )}

        {/* Right Sidebar (Insights, Split Views, and Citation pre/code view) */}
        <aside
          style={{ width: rightSidebarCollapsed ? 0 : (isSmallScreen ? 'min(420px, 90vw)' : rightSidebarWidth) }}
          className={`flex flex-col bg-dark-900 overflow-hidden select-none z-40
                     ${isDraggingRight ? '' : 'transition-all duration-200 ease-in-out'}
                     ${rightSidebarCollapsed ? 'border-l-0 shadow-none' : (isSmallScreen ? 'border-l border-dark-500/80 shadow-2xl' : 'border-l border-dark-500/60')}
                     ${isSmallScreen ? 'absolute right-0 top-0 bottom-0 h-full w-full max-w-[420px]' : 'relative shrink-0'}`}
        >
          <div className="h-full flex flex-col" style={{ width: isSmallScreen ? '100%' : rightSidebarWidth }}>
            <header className="flex items-center justify-between px-4 py-3 bg-dark-900 border-b border-dark-500/50 shrink-0 shadow-sm">
              <span className="text-[10px] font-bold text-dark-300 uppercase tracking-widest">
                {rightTab === 'summary' && 'Repository Summary'}
                {rightTab === 'docs' && 'Repository Docs'}
                {rightTab === 'citation' && 'Citation Code Source'}
              </span>
              <button
                onClick={() => setRightSidebarCollapsed(true)}
                className="text-dark-400 hover:text-white text-xs px-2 py-1 rounded transition"
              >
                ✕ Close
              </button>
            </header>

            <div className="flex-1 overflow-y-auto p-5 select-text">
              {rightTab === 'summary' && (
                <InsightsPanel loading={insightsLoading} error={insightsError}>
                  {summary ? (
                    <div className="space-y-4 text-sm text-dark-200">
                      <div className="p-4 rounded-xl bg-dark-800 border border-dark-500/60 shadow-md">
                        <h2 className="text-sm font-bold text-[#e6edf3] mb-2 truncate">{summary.repositoryName}</h2>
                        <div className="space-y-2.5">
                          <p className="text-xs"><b>Architecture:</b> <span className="text-violet-300">{summary.architectureType}</span></p>
                          <p className="text-xs"><b>Frameworks:</b> <span className="text-cyan-300">{summary.detectedFrameworks.join(', ') || 'n/a'}</span></p>
                          <p className="text-xs"><b>Modules:</b> <span className="text-emerald-300">{summary.moduleStructure.join(', ') || 'n/a'}</span></p>
                        </div>
                      </div>

                      <div className="space-y-3">
                        <InsightsSection title="API Layers" list={summary.apiLayers} />
                        <InsightsSection title="Database Layers" list={summary.databaseLayers} />
                        <InsightsSection title="External Integrations" list={summary.externalIntegrations} />
                      </div>
                    </div>
                  ) : <p className="text-dark-400 text-center py-10 text-xs italic">Select Summary to generate repository overview.</p>}
                </InsightsPanel>
              )}

              {rightTab === 'docs' && (
                <InsightsPanel loading={insightsLoading} error={insightsError}>
                  {docs ? (
                    <div className="space-y-6 text-sm text-dark-200 chat-prose">
                      <Section title="README Summary" body={docs.readmeSummary} />
                      <Section title="Onboarding Guide" body={docs.onboardingGuide} />
                      <Section title="Architecture Summary" body={docs.architectureSummary} />
                      <Section title="API Summary" body={docs.apiSummary} />
                    </div>
                  ) : <p className="text-dark-400 text-center py-10 text-xs italic">Select Docs to generate documentation sections.</p>}
                </InsightsPanel>
              )}

              {rightTab === 'citation' && selectedCitation && (
                <div className="space-y-4">
                  <div className="p-3 bg-dark-800 border border-dark-500/60 rounded-xl shadow-md">
                    <div className="flex items-center justify-between mb-2">
                      <h4 className="text-xs font-bold text-[#e6edf3] truncate max-w-[220px]">
                        {selectedCitation.file_name || selectedCitation.file_path?.split('/').pop()}
                      </h4>
                      <span className="text-[9px] px-2 py-0.5 rounded border border-violet-500/30 bg-violet-950/20 text-violet-300 uppercase font-semibold tracking-wider shrink-0">
                        {selectedCitation.retrieval_source}
                      </span>
                    </div>
                    <p className="text-[10px] text-dark-400 font-mono truncate">{selectedCitation.file_path}</p>
                    {selectedCitation.start_line && (
                      <p className="text-[9px] text-cyan-400 mt-1.5 font-semibold">
                        Lines: {selectedCitation.start_line} - {selectedCitation.end_line || selectedCitation.start_line}
                      </p>
                    )}
                  </div>

                  <div className="border border-dark-500/60 rounded-xl overflow-hidden shadow-inner bg-dark-950">
                    <pre className="p-4 text-xs font-mono overflow-x-auto text-[#e6edf3] leading-relaxed max-h-[480px]">
                      <code>{selectedCitation.content || "No content available in citation."}</code>
                    </pre>
                  </div>

                  {selectedCitation.match_reason && (
                    <div className="p-3 bg-dark-800/50 border border-dark-500/40 rounded-xl text-[11px] text-dark-300 leading-relaxed shadow-sm">
                      <span className="font-bold text-dark-400 uppercase text-[9px] tracking-wider block mb-1">Retrieval Reason</span>
                      {selectedCitation.match_reason}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </aside>

      </div>
    </div>
  );
}

function Section({ title, body }: { title: string; body: string }) {
  return (
    <div className="p-4 bg-dark-800 border border-dark-500/40 rounded-xl shadow-sm">
      <h3 className="text-xs font-bold text-violet-300 uppercase tracking-wide mb-2.5">{title}</h3>
      <p className="text-xs text-dark-200 whitespace-pre-wrap leading-relaxed">{body}</p>
    </div>
  );
}

function InsightsSection({ title, list }: { title: string; list: string[] }) {
  return (
    <div className="p-4 bg-dark-800 border border-dark-500/40 rounded-xl shadow-sm">
      <h3 className="text-xs font-bold text-[#e6edf3] uppercase tracking-wide mb-2.5">{title}</h3>
      {list.length > 0 ? (
        <ul className="space-y-1.5">
          {list.map((item, idx) => (
            <li key={idx} className="text-xs text-dark-200 flex items-start gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-violet-400 mt-1.5 shrink-0" />
              <span>{item}</span>
            </li>
          ))}
        </ul>
      ) : (
        <p className="text-[11px] text-dark-400 italic">None detected</p>
      )}
    </div>
  );
}

function InsightsPanel({ children, loading, error }: { children: ReactNode; loading: boolean; error: string }) {
  return (
    <div className="h-full flex flex-col justify-start">
      {loading && (
        <div className="flex flex-col items-center justify-center py-20 gap-3">
          <div className="w-6 h-6 border-2 border-cyan-400 border-t-transparent rounded-full animate-spin" />
          <p className="text-xs text-dark-400">Generating insights...</p>
        </div>
      )}
      {error && (
        <div className="p-3 bg-red-950/20 border border-red-500/30 rounded-xl text-xs text-red-300 mb-4">
          ⚠️ {error}
        </div>
      )}
      {!loading && children}
    </div>
  );
}

function normalizeRepoUrl(url: string): string {
  return url.replace(/\/$/, '').replace(/\.git$/, '');
}

function mapMessages(messages: MessageView[]): ChatMessage[] {
  return messages.map((m, idx) => ({
    id: idx + 1,
    role: m.role,
    content: m.content,
    citations: parseCitations(m.citationsJson),
  }));
}

function parseCitations(citationsJson?: string) {
  if (!citationsJson) return [];
  try {
    const parsed = JSON.parse(citationsJson);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}
