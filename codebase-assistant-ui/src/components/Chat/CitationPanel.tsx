import { useState } from 'react';
import { Citation } from '../../types';

interface Props {
  citations: Citation[];
  repoUrl: string;
  onSelectCitation?: (citation: Citation) => void;
}

export function CitationPanel({ citations, repoUrl, onSelectCitation }: Props) {
  const [expanded, setExpanded] = useState<string | null>(null);
  const [showAll, setShowAll] = useState(false);

  if (!citations.length) return null;

  const unique = citations.filter(
    (c, i, arr) => i === arr.findIndex(x => x.file_path === c.file_path && x.start_line === c.start_line)
  );
  const visible = showAll ? unique : unique.slice(0, 3);

  return (
    <div className="mt-3 pt-3 border-t border-dark-500/60 animate-fade-in-up">
      <div className="flex items-center justify-between mb-2">
        <span className="text-[10px] font-semibold text-dark-400 uppercase tracking-widest">
          Sources - {unique.length}
        </span>
        {unique.length > 3 && (
          <button
            onClick={() => setShowAll(v => !v)}
            className="text-[10px] text-cyan-400 hover:text-cyan-300"
          >
            {showAll ? 'Show less' : `Show all (${unique.length})`}
          </button>
        )}
      </div>

      <div className="space-y-2">
        {visible.map((c, i) => {
          const id = `${c.file_path}:${c.start_line ?? i}`;
          const fileName = c.file_name || c.file_path?.split('/').pop() || 'file';
          const lineInfo = c.start_line ? `:${c.start_line}${c.end_line ? `-${c.end_line}` : ''}` : '';
          const href = buildUrl(repoUrl, c);
          const source = c.retrieval_source || 'vector';
          const confidence = c.retrieval_confidence || deriveConfidence(c.hybrid_score);
          const hybridScore = normalizeScore(c.hybrid_score);
          const keywordScore = normalizeScore(c.keyword_score);
          const isOpen = expanded === id;

          return (
            <div key={id} className="border border-dark-500 rounded-lg bg-dark-700/60">
              <button
                onClick={() => setExpanded(prev => (prev === id ? null : id))}
                className="w-full px-3 py-2 text-left flex items-center justify-between gap-2"
              >
                <div className="min-w-0 flex-1">
                  <p className="text-xs text-[#e6edf3] truncate">{fileName}{lineInfo}</p>
                  <p className="text-[10px] text-dark-400 truncate">{c.file_path}</p>
                </div>
                <div className="flex items-center gap-1 shrink-0">
                  <Badge label={source.toUpperCase()} />
                  <Badge label={`CONF ${String(confidence).toUpperCase()}`} />
                </div>
              </button>

              {isOpen && (
                <div className="px-3 pb-3 pt-1 text-[11px] text-dark-300 border-t border-dark-500">
                  <p><span className="text-dark-400">Reason:</span> {c.match_reason || defaultReason(source)}</p>
                  <p><span className="text-dark-400">Hybrid score:</span> {hybridScore ?? 'n/a'}</p>
                  <p><span className="text-dark-400">Keyword score:</span> {keywordScore ?? 'n/a'}</p>
                  <div className="flex items-center gap-3 mt-2">
                    {c.content && onSelectCitation && (
                      <button
                        onClick={() => onSelectCitation(c)}
                        className="text-violet-400 hover:text-violet-300 font-medium"
                      >
                        Inspect source code
                      </button>
                    )}
                    {href && (
                      <a
                        href={href}
                        target="_blank"
                        rel="noreferrer"
                        className="text-cyan-400 hover:text-cyan-300"
                      >
                        Open on GitHub
                      </a>
                    )}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

function buildUrl(repoUrl: string, citation: Citation): string | null {
  if (!repoUrl || !citation.file_path) return null;
  const base = repoUrl.replace(/\.git$/, '');
  const lines = citation.start_line
    ? `#L${citation.start_line}${citation.end_line ? `-L${citation.end_line}` : ''}`
    : '';
  return `${base}/blob/main/${citation.file_path}${lines}`;
}

function normalizeScore(score?: number): string | null {
  if (typeof score !== 'number' || Number.isNaN(score)) return null;
  return score.toFixed(3);
}

function deriveConfidence(score?: number): 'high' | 'medium' | 'low' {
  if (typeof score !== 'number') return 'low';
  if (score >= 0.9) return 'high';
  if (score >= 0.7) return 'medium';
  return 'low';
}

function defaultReason(source: string): string {
  if (source === 'hybrid') return 'Matched by semantic and keyword retrieval.';
  if (source === 'keyword') return 'Matched by keyword full-text retrieval.';
  return 'Matched by semantic similarity retrieval.';
}

function Badge({ label }: { label: string }) {
  return (
    <span className="text-[9px] px-1.5 py-0.5 rounded border border-dark-400 text-dark-300">
      {label}
    </span>
  );
}

