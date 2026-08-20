import { IngestForm } from '../Ingestion/IngestForm';
import { ConversationView, IngestResultData, RepositoryView } from '../../types';

const SUGGESTED_QUESTIONS = [
  'How does authentication work?',
  'Explain the main entry point',
  'How are errors handled?',
  'What does the service layer do?',
  'Where is the database configured?',
];

interface LeftSidebarProps {
  collapsed: boolean;
  isSmallScreen: boolean;
  width: number;
  isDragging: boolean;
  ingestResult: IngestResultData | null;
  onIngestSuccess: (repoUrl: string, result: IngestResultData) => void;
  activeRepository: RepositoryView | null;
  conversations: ConversationView[];
  activeConversationId: string;
  onSelectConversation: (id: string) => void;
  onNewConversation: () => void;
  onDeleteConversation: (id: string) => void;
  rightSidebarCollapsed: boolean;
  rightTab: 'summary' | 'docs' | 'citation';
  onOpenInsightsTab: (tab: 'summary' | 'docs') => void;
  canChat: boolean;
  onSuggestedQuestion: (q: string) => void;
}

export function LeftSidebar({
  collapsed,
  isSmallScreen,
  width,
  isDragging,
  ingestResult,
  onIngestSuccess,
  activeRepository,
  conversations,
  activeConversationId,
  onSelectConversation,
  onNewConversation,
  onDeleteConversation,
  rightSidebarCollapsed,
  rightTab,
  onOpenInsightsTab,
  canChat,
  onSuggestedQuestion,
}: LeftSidebarProps) {
  return (
    <aside
      style={{ width: collapsed ? 0 : isSmallScreen ? 280 : width }}
      className={`flex flex-col bg-dark-900 overflow-y-auto overflow-x-hidden select-none z-40
                 ${isDragging ? '' : 'transition-all duration-200 ease-in-out'}
                 ${collapsed ? 'border-r-0 shadow-none' : isSmallScreen ? 'border-r border-dark-500/80 shadow-2xl' : 'border-r border-dark-500/60'}
                 ${isSmallScreen ? 'absolute left-0 top-0 bottom-0 h-full' : 'relative shrink-0'}`}
    >
      <div className="w-[280px]">
        <IngestForm onSuccess={onIngestSuccess} />

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
              onClick={onNewConversation}
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
                      onClick={() => onSelectConversation(c.id)}
                      className={`flex-1 text-left text-xs rounded-lg px-3 py-2 border transition-all duration-150 truncate ${
                        isActive
                          ? 'bg-gradient-to-r from-violet-950/40 to-dark-800 border-violet-500/40 text-violet-300 shadow-sm'
                          : 'bg-dark-800 border-dark-500/60 text-dark-300 hover:text-dark-100 hover:bg-dark-700/60'
                      }`}
                    >
                      💬 {c.title}
                    </button>
                    <button
                      onClick={() => onDeleteConversation(c.id)}
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

        {/* Insights triggers */}
        <div className="px-3 pb-3">
          <p className="text-[10px] font-bold text-dark-400 uppercase tracking-widest mb-2.5 px-1">Repository Insights</p>
          <div className="grid grid-cols-2 gap-2">
            <button
              onClick={() => onOpenInsightsTab('summary')}
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
              onClick={() => onOpenInsightsTab('docs')}
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

        {/* Suggested queries */}
        <div className="px-3 pb-4">
          <p className="text-[10px] font-bold text-dark-400 uppercase tracking-widest mb-2.5 px-1">Suggested Queries</p>
          <div className="space-y-1.5">
            {SUGGESTED_QUESTIONS.map(q => (
              <button
                key={q}
                onClick={() => onSuggestedQuestion(q)}
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
  );
}
