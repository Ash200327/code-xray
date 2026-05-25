import { useEffect, useRef, useState } from 'react';
import { useStreamingChat } from '../../hooks/useStreamingChat';
import { MessageBubble } from './MessageBubble';
import { ChatMessage, Citation } from '../../types';

interface Props {
  repoUrl: string;
  suggestedQuestion?: string;
  conversationId?: string;
  initialMessages?: ChatMessage[];
  onSelectCitation?: (citation: Citation) => void;
}

export function ChatWindow({ repoUrl, suggestedQuestion, conversationId, initialMessages, onSelectCitation }: Props) {
  const { messages, isStreaming, sendMessage, clearMessages, setMessageHistory } = useStreamingChat();
  const [input, setInput] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    setMessageHistory(initialMessages || []);
  }, [initialMessages, setMessageHistory]);

  // Pre-fill input when a suggested question is clicked
  useEffect(() => {
    if (suggestedQuestion) {
      setInput(suggestedQuestion);
      textareaRef.current?.focus();
    }
  }, [suggestedQuestion]);

  const handleSend = () => {
    const q = input.trim();
    if (!q || isStreaming) return;
    setInput('');
    sendMessage(q, repoUrl, conversationId);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const lastMessageId = messages[messages.length - 1]?.id;

  return (
    <div className="flex flex-col flex-1 overflow-hidden">

      {/* Chat header */}
      <div className="flex items-center justify-between px-3 py-2.5 sm:px-5 sm:py-3 bg-dark-800/80 backdrop-blur border-b border-dark-500 shrink-0">
        <div className="flex items-center gap-2">
          <div className={`w-2 h-2 rounded-full ${isStreaming ? 'bg-violet-400 animate-pulse' : 'bg-emerald-400'}`} />
          <span className="text-sm font-medium text-[#e6edf3]">
            {isStreaming ? 'Generating…' : 'Chat'}
          </span>
          {messages.length > 0 && (
            <span className="text-xs text-dark-400 ml-1">
              {messages.filter(m => m.role === 'user').length} question{messages.filter(m => m.role === 'user').length !== 1 ? 's' : ''}
            </span>
          )}
        </div>
        {messages.length > 0 && (
          <button
            onClick={clearMessages}
            className="text-xs text-dark-400 hover:text-[#e6edf3] border border-dark-500 hover:border-dark-400
                       px-2.5 py-1 rounded-lg transition-all duration-150"
          >
            Clear chat
          </button>
        )}
      </div>

      {/* Message list */}
      <div className="flex-1 overflow-y-auto px-3 py-4 sm:px-5 sm:py-6 space-y-1">
        {messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full gap-3 text-center">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-violet-600/20 to-cyan-500/20
                            border border-violet-500/20 flex items-center justify-center">
              <svg className="w-6 h-6 text-violet-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                  d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
              </svg>
            </div>
            <p className="text-sm text-[#8b949e]">Ask anything about the indexed codebase</p>
            <p className="text-xs text-dark-400">Press <kbd className="bg-dark-600 border border-dark-500 rounded px-1.5 py-0.5 font-mono text-[10px]">Enter</kbd> to send · <kbd className="bg-dark-600 border border-dark-500 rounded px-1.5 py-0.5 font-mono text-[10px]">Shift+Enter</kbd> for new line</p>
          </div>
        ) : (
          messages.map(msg => (
            <MessageBubble
              key={msg.id}
              message={msg}
              repoUrl={repoUrl}
              isStreaming={isStreaming && msg.id === lastMessageId && msg.role === 'assistant'}
              onSelectCitation={onSelectCitation}
            />
          ))
        )}
        <div ref={bottomRef} />
      </div>

      {/* Input bar */}
      <div className="shrink-0 px-3 py-3 sm:px-5 sm:py-4 bg-dark-800/80 backdrop-blur border-t border-dark-500">
        <div className={`flex items-end gap-3 bg-dark-700 border rounded-xl px-3 py-2 transition-all duration-200
                          ${isStreaming ? 'border-violet-500/30' : 'border-dark-500 focus-within:border-violet-500/60 focus-within:ring-1 focus-within:ring-violet-500/20'}`}>
          <textarea
            ref={textareaRef}
            className="flex-1 bg-transparent text-base sm:text-sm text-[#e6edf3] placeholder-dark-400
                       outline-none resize-none leading-relaxed min-h-[24px] sm:min-h-[20px] max-h-32"
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask about the codebase…"
            rows={1}
            disabled={isStreaming}
            style={{ height: 'auto' }}
            onInput={e => {
              const t = e.target as HTMLTextAreaElement;
              t.style.height = 'auto';
              t.style.height = `${Math.min(t.scrollHeight, 128)}px`;
            }}
          />
          <button
            onClick={handleSend}
            disabled={isStreaming || !input.trim()}
            className="shrink-0 w-8 h-8 rounded-lg bg-gradient-to-br from-violet-600 to-cyan-600
                       hover:from-violet-500 hover:to-cyan-500 flex items-center justify-center
                       transition-all duration-200 active:scale-95
                       disabled:opacity-40 disabled:cursor-not-allowed disabled:active:scale-100"
          >
            {isStreaming ? (
              <svg className="w-3.5 h-3.5 text-white animate-spin" viewBox="0 0 24 24" fill="none">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
              </svg>
            ) : (
              <svg className="w-3.5 h-3.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 12h14M12 5l7 7-7 7" />
              </svg>
            )}
          </button>
        </div>
        <p className="text-[10px] text-dark-400 mt-1.5 text-center">
          Answers are grounded in the indexed codebase · citations link to exact lines on GitHub
        </p>
      </div>
    </div>
  );
}
