import { ChatMessage, Citation, MessageView } from '../types';

export function parseCitations(citationsJson?: string): Citation[] {
  if (!citationsJson) return [];
  try {
    const parsed = JSON.parse(citationsJson);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function mapMessages(messages: MessageView[]): ChatMessage[] {
  return messages.map((m, idx) => ({
    id: idx + 1,
    role: m.role,
    content: m.content,
    citations: parseCitations(m.citationsJson),
  }));
}
