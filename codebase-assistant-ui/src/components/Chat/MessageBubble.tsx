import { Suspense, lazy, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { ChatMessage, Citation } from '../../types';
import { CitationPanel } from './CitationPanel';
const LazyCodeBlock = lazy(() => import('./CodeBlock').then(m => ({ default: m.CodeBlock })));

interface Props {
  message: ChatMessage;
  repoUrl: string;
  isStreaming?: boolean;
  onSelectCitation?: (citation: Citation) => void;
}

export function MessageBubble({ message, repoUrl, isStreaming, onSelectCitation }: Props) {
  const isUser = message.role === 'user';
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    if (!message.content) return;
    navigator.clipboard.writeText(message.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className={`flex gap-3 mb-5 animate-fade-in-up ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>

      {/* Avatar */}
      <div className={`w-8 h-8 rounded-lg shrink-0 flex items-center justify-center text-xs font-bold
                        ${isUser
                          ? 'bg-gradient-to-br from-violet-600 to-blue-600 text-white'
                          : 'bg-gradient-to-br from-cyan-600/30 to-violet-600/30 border border-violet-500/30 text-violet-300'
                        }`}>
        {isUser ? 'U' : 'AI'}
      </div>

      {/* Bubble */}
      <div className={`max-w-[85%] sm:max-w-[75%] min-w-0 ${isUser ? 'items-end' : 'items-start'} flex flex-col select-text`}>
        <div className={`rounded-2xl px-4 py-3 text-sm leading-relaxed relative select-text
                          ${isUser
                            ? 'bg-gradient-to-br from-violet-600 to-blue-600 text-white rounded-tr-sm shadow-lg shadow-violet-500/20'
                            : 'bg-dark-700 border border-dark-500 text-[#e6edf3] rounded-tl-sm pr-10'
                          }`}>
          {isUser ? (
            <p className="whitespace-pre-wrap">{message.content}</p>
          ) : (
            <>
              <div className={`chat-prose ${isStreaming && !message.content ? '' : ''}`}>
                {message.content ? (
                  <>
                    <ReactMarkdown
                      remarkPlugins={[remarkGfm]}
                      components={{
                        code({ className, children }) {
                          const match = /language-(\w+)/.exec(className || '');
                          return match ? (
                            <Suspense fallback={<pre className="bg-dark-800 p-3 rounded text-xs overflow-auto">{String(children)}</pre>}>
                              <LazyCodeBlock language={match[1]} code={String(children).replace(/\n$/, '')} />
                            </Suspense>
                          ) : (
                            <code className="bg-dark-600 text-violet-300 px-1.5 py-0.5 rounded text-xs font-mono">
                              {children}
                            </code>
                          );
                        },
                      }}
                    >
                      {message.content}
                    </ReactMarkdown>
                    {isStreaming && (
                      <span className="inline-block w-0.5 h-4 bg-violet-400 animate-blink ml-0.5 align-text-bottom" />
                    )}
                  </>
                ) : (
                  <TypingIndicator />
                )}
              </div>

              {message.content && (
                <button
                  onClick={handleCopy}
                  className="absolute top-3 right-3 p-1.5 rounded-lg bg-dark-800 hover:bg-dark-600 text-dark-400 hover:text-white transition duration-150 active:scale-90 border border-dark-500/50"
                  title="Copy Response"
                >
                  {copied ? (
                    <svg className="w-3.5 h-3.5 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                  ) : (
                    <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                    </svg>
                  )}
                </button>
              )}
            </>
          )}
        </div>

        {/* Citations */}
        {!isUser && message.citations.length > 0 && (
          <div className="w-full mt-1 px-1">
            <CitationPanel citations={message.citations} repoUrl={repoUrl} onSelectCitation={onSelectCitation} />
          </div>
        )}
      </div>
    </div>
  );
}

function TypingIndicator() {
  return (
    <div className="flex items-center gap-1 py-1">
      {[0, 1, 2].map(i => (
        <span
          key={i}
          className="w-1.5 h-1.5 rounded-full bg-violet-400 animate-bounce"
          style={{ animationDelay: `${i * 150}ms` }}
        />
      ))}
    </div>
  );
}
