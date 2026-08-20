import { ReactNode } from 'react';
import { Citation, RepositoryDocsView, RepositorySummaryView } from '../../types';

interface RightSidebarProps {
  collapsed: boolean;
  isSmallScreen: boolean;
  width: number;
  isDragging: boolean;
  onClose: () => void;
  rightTab: 'summary' | 'docs' | 'citation';
  summary: RepositorySummaryView | null;
  docs: RepositoryDocsView | null;
  selectedCitation: Citation | null;
  insightsLoading: boolean;
  insightsError: string;
}

export function RightSidebar({
  collapsed,
  isSmallScreen,
  width,
  isDragging,
  onClose,
  rightTab,
  summary,
  docs,
  selectedCitation,
  insightsLoading,
  insightsError,
}: RightSidebarProps) {
  return (
    <aside
      style={{ width: collapsed ? 0 : isSmallScreen ? 'min(420px, 90vw)' : width }}
      className={`flex flex-col bg-dark-900 overflow-hidden select-none z-40
                 ${isDragging ? '' : 'transition-all duration-200 ease-in-out'}
                 ${collapsed ? 'border-l-0 shadow-none' : isSmallScreen ? 'border-l border-dark-500/80 shadow-2xl' : 'border-l border-dark-500/60'}
                 ${isSmallScreen ? 'absolute right-0 top-0 bottom-0 h-full w-full max-w-[420px]' : 'relative shrink-0'}`}
    >
      <div className="h-full flex flex-col" style={{ width: isSmallScreen ? '100%' : width }}>
        <header className="flex items-center justify-between px-4 py-3 bg-dark-900 border-b border-dark-500/50 shrink-0 shadow-sm">
          <span className="text-[10px] font-bold text-dark-300 uppercase tracking-widest">
            {rightTab === 'summary' && 'Repository Summary'}
            {rightTab === 'docs' && 'Repository Docs'}
            {rightTab === 'citation' && 'Citation Code Source'}
          </span>
          <button
            onClick={onClose}
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
              ) : (
                <p className="text-dark-400 text-center py-10 text-xs italic">Select Summary to generate repository overview.</p>
              )}
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
              ) : (
                <p className="text-dark-400 text-center py-10 text-xs italic">Select Docs to generate documentation sections.</p>
              )}
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
                  <code>{selectedCitation.content || 'No content available in citation.'}</code>
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
