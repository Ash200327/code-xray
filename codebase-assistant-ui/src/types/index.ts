export interface Citation {
  repo_url?: string;
  file_path?: string;
  file_name?: string;
  language?: string;
  start_line?: number;
  end_line?: number;
  chunk_type?: string;
  retrieval_source?: 'vector' | 'keyword' | 'hybrid' | string;
  keyword_score?: number;
  hybrid_score?: number;
  retrieval_confidence?: 'high' | 'medium' | 'low' | string;
  match_reason?: string;
  content?: string;
}

export interface ChatMessage {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  citations: Citation[];
}

export type IngestStatus = 'idle' | 'loading' | 'success' | 'error';

export interface IngestResultData {
  repoUrl: string;
  totalFiles: number;
  totalChunks: number;
  durationMs: number;
  status: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: {
    code: string;
    message: string;
  };
}

export type IngestionJobStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface IngestionJobData {
  jobId: string;
  repoUrl: string;
  branch: string;
  status: IngestionJobStatus;
  attempt: number;
  maxAttempts: number;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
  errorMessage?: string;
  result?: IngestResultData;
}

export interface IngestionProgressEvent {
  jobId: string;
  state: IngestionJobStatus;
  repoUrl: string;
  branch: string;
  currentFile?: string;
  totalFilesDiscovered: number;
  filesProcessed: number;
  totalChunks: number;
  batchesStored: number;
  percentage: number;
  message?: string;
  error?: string;
}

export interface RepositoryView {
  id: string;
  workspaceId?: string;
  name: string;
  repoUrl: string;
  branch: string;
  createdAt: string;
  updatedAt: string;
  lastIngestedAt?: string;
}

export interface UserView {
  id: string;
  email: string;
  displayName: string;
}

export interface ConversationView {
  id: string;
  repositoryId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface MessageView {
  id: string;
  conversationId: string;
  role: 'user' | 'assistant';
  content: string;
  citationsJson?: string;
  createdAt: string;
}

export interface SignUpRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserView;
}

export interface RepositorySummaryView {
  repositoryName: string;
  repoUrl: string;
  architectureType: string;
  detectedFrameworks: string[];
  moduleStructure: string[];
  apiLayers: string[];
  databaseLayers: string[];
  externalIntegrations: string[];
}

export interface RepositoryDocsView {
  readmeSummary: string;
  onboardingGuide: string;
  architectureSummary: string;
  apiSummary: string;
}
