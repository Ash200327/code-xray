import { useState } from 'react';
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
  repositories: RepositoryView[];
  activeRepository: RepositoryView | null;
  onSelectRepository: (repo: RepositoryView) => void;
  onDeleteRepository: (repoId: string) => void;
  conversations: ConversationView[];
  activeConversationId: string;
  onSelectConversation: (id: string) => void;
  onNewConversation: () => void;
  onRenameConversation: (id: string, newTitle: string) => void;
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
  repositories,
  activeRepository,
  onSelectRepository,
  onDeleteRepository,
  conversations,
  activeConversationId,
  onSelectConversation,
  onNewConversation,
  onRenameConversation,
  onDeleteConversation,
  rightSidebarCollapsed,
  rightTab,
  onOpenInsightsTab,
  canChat,
  onSuggestedQuestion,
}: LeftSidebarProps) {
  const [editingConvId, setEditingConvId] = useState<string | null>(null);
  const [editingTitle, setEditingTitle] = useState<string>('');

  const startRename = (c: ConversationView, e: React.MouseEvent) => {
    e.stopPropagation();
    setEditingConvId(c.id);
    setEditingTitle(c.title);
  };

  const handleSaveRename = (cId: string) => {
    if (editingTitle.trim()) {
      onRenameConversation(cId, editingTitle.trim());
    }
    setEditingConvId(null);
    setEditingTitle('');
  };

  const handleKeyDown = (e: React.KeyboardEvent, cId: string) => {
    if (e.key === 'Enter') {
      handleSaveRename(cId);
    } else if (e.key === 'Escape') {
      setEditingConvId(null);
      setEditingTitle('');
    }
  };

  return (
    <aside
      style={{ width: collapsed ? 0 : isSmallScreen ? 280 : width }}
      className={`flex flex-col bg-dark-900 overflow-y-auto overflow-x-hidden select-none z-40
                 ${isDragging ? '' : 'transition-all duration-200 ease-in-out'}
                 ${collapsed ? 'border-r-0 shadow-none' : isSmallScreen ? 'border-r border-dark-500/80 shadow-2xl' : 'border-r border-dark-500/60'}
                 ${isSmallScreen ? 'absolute left-0 top-0 bottom-0 h-full' : 'relative shrink-0'}`}
    >
      <div className="w-[280px]">
        {/* Ingestion Box */}
        <IngestForm onSuccess={onIngestSuccess} />

        {/* Ingestion Results Card */}
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

        {/* Repositories Section */}
        <div className="px-3 pb-3">
          <div className="flex items-center justify-between mb-2 px-1">
            <p className="text-[10px] font-bold text-dark-400 uppercase tracking-widest">Indexed Repos</p>
            <span className="text-[10px] text-dark-400 font-mono">{repositories.length}</span>
          </div>

          <div className="space-y-1 max-h-36 overflow-y-auto pr-0.5">
            {repositories.length === 0 ? (
              <p className="text-[11px] text-dark-400 italic px-1">No repositories indexed yet</p>
            ) : (
              repositories.map(repo => {
                const isActive = activeRepository?.id === repo.id;
                const repoName = repo.name || repo.repoUrl.split('/').pop() || repo.repoUrl;
                return (
                  <div key={repo.id} className="flex items-center gap-1 group">
                    <button
                      onClick={() => onSelectRepository(repo)}
                      title={repo.repoUrl}
                      className={`flex-1 text-left text-xs rounded-lg px-2.5 py-1.5 border transition-all duration-150 truncate flex items-center gap-1.5 ${
                        isActive
                          ? 'bg-cyan-950/40 border-cyan-500/50 text-cyan-300 shadow-sm font-medium'
                          : 'bg-dark-800 border-dark-500/60 text-dark-300 hover:text-dark-100 hover:bg-dark-700/60'
                      }`}
                    >
                      <span className="text-xs shrink-0">📁</span>
                      <span className="truncate">{repoName}</span>
                    </button>
                    <button
                      onClick={e => {
                        e.stopPropagation();
                        if (confirm(`Delete repository "${repoName}" and all its vector embeddings?`)) {
                          onDeleteRepository(repo.id);
                        }
                      }}
                      className="text-xs text-dark-400 hover:text-red-400 p-1 opacity-0 group-hover:opacity-100 transition-opacity duration-150 shrink-0"
                      title="Delete Repository"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Chats section */}
        <div className="px-3 pb-3">
          <div className="flex items-center justify-between mb-2 px-1">
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
                const isEditing = editingConvId === c.id;

                if (isEditing) {
                  return (
                    <div key={c.id} className="flex items-center gap-1 px-1 py-0.5">
                      <input
                        type="text"
                        autoFocus
                        value={editingTitle}
                        onChange={e => setEditingTitle(e.target.value)}
                        onBlur={() => handleSaveRename(c.id)}
                        onKeyDown={e => handleKeyDown(e, c.id)}
                        className="flex-1 text-xs bg-dark-950 border border-violet-500 rounded px-2 py-1 text-white focus:outline-none"
                      />
                    </div>
                  );
                }

                return (
                  <div key={c.id} className="flex items-center gap-1 group">
                    <button
                      onClick={() => onSelectConversation(c.id)}
                      className={`flex-1 text-left text-xs rounded-lg px-2.5 py-1.5 border transition-all duration-150 truncate flex items-center gap-1.5 ${
                        isActive
                          ? 'bg-gradient-to-r from-violet-950/40 to-dark-800 border-violet-500/40 text-violet-300 shadow-sm font-medium'
                          : 'bg-dark-800 border-dark-500/60 text-dark-300 hover:text-dark-100 hover:bg-dark-700/60'
                      }`}
                    >
                      <span className="shrink-0">💬</span>
                      <span className="truncate">{c.title}</span>
                    </button>
                    {/* Rename button */}
                    <button
                      onClick={e => startRename(c, e)}
                      className="text-xs text-dark-400 hover:text-violet-300 p-1 opacity-0 group-hover:opacity-100 transition-opacity duration-150 shrink-0"
                      title="Rename Chat"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    {/* Delete button */}
                    <button
                      onClick={e => {
                        e.stopPropagation();
                        onDeleteConversation(c.id);
                      }}
                      className="text-xs text-dark-400 hover:text-red-400 p-1 opacity-0 group-hover:opacity-100 transition-opacity duration-150 shrink-0"
                      title="Delete Chat"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Insights triggers */}
        <div className="px-3 pb-3">
          <p className="text-[10px] font-bold text-dark-400 uppercase tracking-widest mb-2 px-1">Repository Insights</p>
          <div className="grid grid-cols-2 gap-2">
            <button
              onClick={() => onOpenInsightsTab('summary')}
              disabled={!activeRepository}
              className={`text-xs font-semibold rounded-lg px-2.5 py-1.5 border transition-all duration-150 ${
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
              className={`text-xs font-semibold rounded-lg px-2.5 py-1.5 border transition-all duration-150 ${
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
          <p className="text-[10px] font-bold text-dark-400 uppercase tracking-widest mb-2 px-1">Suggested Queries</p>
          <div className="space-y-1.5">
            {SUGGESTED_QUESTIONS.map(q => (
              <button
                key={q}
                onClick={() => onSuggestedQuestion(q)}
                disabled={!canChat}
                className="w-full text-left text-xs text-dark-300 bg-dark-800 hover:bg-dark-700/80
                           hover:text-white border border-dark-500/60 hover:border-violet-500/40
                           rounded-lg px-3 py-1.5 transition-all duration-150
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
