import { useEffect, useMemo, useState } from 'react';
import { ChatWindow } from './components/Chat/ChatWindow';
import { AuthScreen } from './components/Auth/AuthScreen';
import { LandingPage } from './components/Landing/LandingPage';
import { LeftSidebar } from './components/Layout/LeftSidebar';
import { RightSidebar } from './components/Layout/RightSidebar';
import { useResizableSidebar } from './hooks/useResizableSidebar';
import {
  AuthResponse,
  ChatMessage,
  Citation,
  ConversationView,
  IngestResultData,
  RepositoryDocsView,
  RepositorySummaryView,
  RepositoryView,
  UserView,
} from './types';
import { createConversation, deleteConversation, listConversations, listMessages } from './api/conversations';
import { listRepositories } from './api/repositories';
import { me, logout } from './api/auth';
import { getAccessToken } from './api/http';
import { getRepositoryDocs, getRepositorySummary } from './api/insights';
import { normalizeRepoUrl, getRepoShortName } from './utils/url';
import { mapMessages } from './utils/formatters';

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

  // Layout states
  const [leftSidebarCollapsed, setLeftSidebarCollapsed] = useState(false);
  const [rightSidebarCollapsed, setRightSidebarCollapsed] = useState(true);
  const [isSmallScreen, setIsSmallScreen] = useState(window.innerWidth < 1024);

  // Resizable sidebars
  const leftSidebar = useResizableSidebar({
    initialWidth: 280,
    minWidth: 200,
    maxWidth: 450,
    direction: 'left',
  });

  const rightSidebar = useResizableSidebar({
    initialWidth: 420,
    minWidth: 280,
    maxWidth: 650,
    direction: 'right',
  });

  // Insights & Citation tabs
  const [rightTab, setRightTab] = useState<'summary' | 'docs' | 'citation'>('summary');
  const [selectedCitation, setSelectedCitation] = useState<Citation | null>(null);
  const [summary, setSummary] = useState<RepositorySummaryView | null>(null);
  const [docs, setDocs] = useState<RepositoryDocsView | null>(null);
  const [insightsLoading, setInsightsLoading] = useState(false);
  const [insightsError, setInsightsError] = useState('');

  // Bootstrap auth token
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

  // Open left sidebar on login
  useEffect(() => {
    if (user) {
      setLeftSidebarCollapsed(false);
    }
  }, [user]);

  // Global shortcut to toggle Left Sidebar: Ctrl+B / Cmd+B
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

  // Window resize listener
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

  // Load conversation messages when activeConversationId changes
  useEffect(() => {
    if (!activeConversationId) {
      setConversationMessages([]);
      return;
    }
    void loadConversationMessages(activeConversationId);
  }, [activeConversationId]);

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
      setActiveConversationId(prev => (prev && list.some(c => c.id === prev) ? prev : list[0].id));
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

  const handleOpenInsightsTab = (tab: 'summary' | 'docs') => {
    if (rightTab === tab && !rightSidebarCollapsed) {
      setRightSidebarCollapsed(true);
    } else {
      setRightTab(tab);
      setRightSidebarCollapsed(false);
      if (isSmallScreen) {
        setLeftSidebarCollapsed(true);
      }
    }
  };

  const canChat = Boolean(activeRepoUrl && activeConversationId);
  const repoShortName = getRepoShortName(activeRepoUrl);
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
          <h1 className="text-sm font-bold gradient-text tracking-tight uppercase shrink-0 hidden min-[480px]:block">
            Code-Xray
          </h1>
        </div>

        <div className="h-4 w-px bg-dark-500 hidden sm:block" />
        <span className="text-xs text-dark-400 hidden sm:inline-block">
          Signed in as <b className="text-dark-200">{user.displayName}</b>
        </span>

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
          className={`text-xs text-red-400 hover:text-red-300 border border-red-500/30 px-2 py-1 sm:px-3 sm:py-1.5 rounded-lg bg-red-950/10 hover:bg-red-950/20 transition-all duration-150 active:scale-95 ${
            !activeRepoUrl ? 'ml-auto' : ''
          }`}
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
        <LeftSidebar
          collapsed={leftSidebarCollapsed}
          isSmallScreen={isSmallScreen}
          width={leftSidebar.width}
          isDragging={leftSidebar.isDragging}
          ingestResult={ingestResult}
          onIngestSuccess={handleIngestSuccess}
          activeRepository={activeRepository}
          conversations={conversations}
          activeConversationId={activeConversationId}
          onSelectConversation={setActiveConversationId}
          onNewConversation={() => void handleNewConversation()}
          onDeleteConversation={id => void handleDeleteConversation(id)}
          rightSidebarCollapsed={rightSidebarCollapsed}
          rightTab={rightTab}
          onOpenInsightsTab={handleOpenInsightsTab}
          canChat={canChat}
          onSuggestedQuestion={handleSuggestedQuestion}
        />

        {/* Left Resize Divider */}
        {!leftSidebarCollapsed && !isSmallScreen && (
          <div
            onMouseDown={leftSidebar.startResizing}
            onDoubleClick={leftSidebar.resetWidth}
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
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.5}
                    d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
                  />
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
            onMouseDown={rightSidebar.startResizing}
            onDoubleClick={rightSidebar.resetWidth}
            className="w-1 cursor-col-resize hover:bg-violet-500/40 bg-transparent transition-all duration-150 shrink-0 z-30"
          />
        )}

        {/* Right Sidebar (Insights and Citation preview) */}
        <RightSidebar
          collapsed={rightSidebarCollapsed}
          isSmallScreen={isSmallScreen}
          width={rightSidebar.width}
          isDragging={rightSidebar.isDragging}
          onClose={() => setRightSidebarCollapsed(true)}
          rightTab={rightTab}
          summary={summary}
          docs={docs}
          selectedCitation={selectedCitation}
          insightsLoading={insightsLoading}
          insightsError={insightsError}
        />
      </div>
    </div>
  );
}
