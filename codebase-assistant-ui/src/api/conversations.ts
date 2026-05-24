import { ApiResponse, ConversationView, MessageView } from '../types';
import { apiFetch } from './http';

export async function listConversations(repositoryId: string): Promise<ConversationView[]> {
  const res = await apiFetch(`/api/conversations?repositoryId=${encodeURIComponent(repositoryId)}`);
  const body: ApiResponse<ConversationView[]> = await res.json();
  if (!res.ok || !body.success) {
    throw new Error(body.error?.message || `Server error: ${res.status}`);
  }
  return body.data;
}

export async function createConversation(repositoryId: string, title: string): Promise<ConversationView> {
  const res = await apiFetch('/api/conversations', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ repositoryId, title }),
  });
  const body: ApiResponse<ConversationView> = await res.json();
  if (!res.ok || !body.success) {
    throw new Error(body.error?.message || `Server error: ${res.status}`);
  }
  return body.data;
}

export async function deleteConversation(conversationId: string): Promise<void> {
  const res = await apiFetch(`/api/conversations/${conversationId}`, { method: 'DELETE' });
  const body: ApiResponse<null> = await res.json();
  if (!res.ok || !body.success) {
    throw new Error(body.error?.message || `Server error: ${res.status}`);
  }
}

export async function listMessages(conversationId: string): Promise<MessageView[]> {
  const res = await apiFetch(`/api/conversations/${conversationId}/messages`);
  const body: ApiResponse<MessageView[]> = await res.json();
  if (!res.ok || !body.success) {
    throw new Error(body.error?.message || `Server error: ${res.status}`);
  }
  return body.data;
}
