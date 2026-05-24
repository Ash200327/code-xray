import { useState, useCallback } from 'react';
import { ChatMessage, Citation } from '../types';

export function useStreamingChat() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);

  const sendMessage = useCallback((question: string, repoUrl: string, conversationId?: string) => {
    if (isStreaming) return;

    const userId = createMessageId();
    const assistantId = createMessageId();

    // Add user + assistant placeholder in one state update to avoid ordering/race issues.
    setMessages(prev => [
      ...prev,
      {
        id: userId,
        role: 'user',
        content: question,
        citations: [],
      },
      {
        id: assistantId,
        role: 'assistant',
        content: '',
        citations: [],
      }
    ]);

    setIsStreaming(true);

    const params = new URLSearchParams({ question, repoUrl });
    if (conversationId) {
      params.set('conversationId', conversationId);
    }
    const es = new EventSource(`/api/chat/stream?${params}`);

    es.addEventListener('message', (event) => {
      const chunk = event.data ?? '';
      setMessages(prev => prev.map(m =>
        m.id === assistantId ? { ...m, content: appendChunk(m.content, chunk) } : m
      ));
    });

    es.addEventListener('citations', (event) => {
      try {
        const citations: Citation[] = JSON.parse(event.data);
        setMessages(prev => prev.map(m =>
          m.id === assistantId ? { ...m, citations } : m
        ));
      } catch {
        // ignore parse errors
      }
    });

    es.addEventListener('done', () => {
      es.close();
      setIsStreaming(false);
    });

    es.addEventListener('error', (event) => {
      const data = (event as MessageEvent).data;
      if (data) {
        setMessages(prev => prev.map(m =>
          m.id === assistantId ? { ...m, content: m.content + '\n\n_Error: ' + data + '_' } : m
        ));
      }
      es.close();
      setIsStreaming(false);
    });

    es.onerror = () => {
      es.close();
      setIsStreaming(false);
    };
  }, [isStreaming]);

  const clearMessages = useCallback(() => setMessages([]), []);
  const setMessageHistory = useCallback((history: ChatMessage[]) => setMessages(history), []);

  return { messages, isStreaming, sendMessage, clearMessages, setMessageHistory };
}

function appendChunk(existing: string, chunk: string): string {
  if (!existing) return chunk;
  if (!chunk) return existing;

  const prev = existing[existing.length - 1];
  const next = chunk[0];
  const needsSpace =
    /[A-Za-z0-9]/.test(prev) &&
    /[A-Za-z0-9]/.test(next) &&
    !chunk.startsWith("'") &&
    !chunk.startsWith("’");

  return needsSpace ? `${existing} ${chunk}` : existing + chunk;
}

function createMessageId(): number {
  // Date-based IDs can collide when messages are created in quick succession.
  return Math.floor(Math.random() * 1_000_000_000);
}
