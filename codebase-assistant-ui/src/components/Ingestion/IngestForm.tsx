import { useEffect, useState } from 'react';
import { getIngestionJob, startIngestionJob } from '../../api/ingest';
import { IngestResultData, IngestStatus, IngestionProgressEvent } from '../../types';
import { useIngestionJobProgress } from '../../hooks/useIngestionJobProgress';

interface Props {
  onSuccess: (repoUrl: string, result: IngestResultData) => void;
}

export function IngestForm({ onSuccess }: Props) {
  const [repoUrl, setRepoUrl] = useState('');
  const [branch, setBranch] = useState('main');
  const [status, setStatus] = useState<IngestStatus>('idle');
  const [error, setError] = useState('');
  const [progress, setProgress] = useState(0);
  const [progressMessage, setProgressMessage] = useState('Waiting to start...');
  const [currentFile, setCurrentFile] = useState('');
  const { subscribe, unsubscribe } = useIngestionJobProgress();

  const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

  const awaitTerminalJobState = async (jobId: string) => {
    for (let i = 0; i < 8; i++) {
      const job = await getIngestionJob(jobId);
      if (job.status === 'COMPLETED' || job.status === 'FAILED') {
        return job;
      }
      await sleep(350);
    }
    return getIngestionJob(jobId);
  };

  useEffect(() => {
    return () => unsubscribe();
  }, [unsubscribe]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!repoUrl.trim()) return;

    setStatus('loading');
    setError('');
    setProgress(0);
    setCurrentFile('');
    setProgressMessage('Submitting ingestion job...');

    try {
      const job = await startIngestionJob(repoUrl.trim(), branch.trim() || 'main');

      subscribe(job.jobId, async (event: IngestionProgressEvent) => {
        setProgress(event.percentage ?? 0);
        setProgressMessage(event.message || 'Indexing...');
        setCurrentFile(event.currentFile || '');

        if (event.state === 'COMPLETED' || event.state === 'FAILED') {
          unsubscribe();
          const finalJob = await awaitTerminalJobState(job.jobId);
          if (finalJob.status === 'COMPLETED' && finalJob.result) {
            setStatus('success');
            setProgress(100);
            setProgressMessage('Ingestion completed');
            onSuccess(repoUrl.trim(), finalJob.result);
          } else {
            setStatus('error');
            setProgress(0);
            setError(finalJob.errorMessage || event.error || 'Ingestion failed');
          }
        }
      });
    } catch (err) {
      setProgress(0);
      setError(err instanceof Error ? err.message : 'Ingestion failed');
      setStatus('error');
    }
  };

  return (
    <div className="p-4 border-b border-dark-500">
      <div className="flex items-center gap-2 mb-3">
        <div className="w-1 h-4 rounded-full bg-gradient-to-b from-violet-500 to-cyan-500" />
        <h2 className="text-sm font-semibold text-[#e6edf3]">Index a Repository</h2>
      </div>

      <form onSubmit={handleSubmit} className="space-y-2">
        <input
          className="input-field"
          type="url"
          placeholder="https://github.com/owner/repo"
          value={repoUrl}
          onChange={e => setRepoUrl(e.target.value)}
          disabled={status === 'loading'}
          required
        />
        <div className="flex gap-2">
          <input
            className="input-field w-24"
            type="text"
            placeholder="Branch"
            value={branch}
            onChange={e => setBranch(e.target.value)}
            disabled={status === 'loading'}
          />
          <button
            type="submit"
            disabled={status === 'loading' || !repoUrl.trim()}
            className="btn-primary flex-1 py-2 px-4 text-sm flex items-center justify-center gap-2"
          >
            {status === 'loading' ? 'Indexing...' : 'Index'}
          </button>
        </div>
      </form>

      {status === 'loading' && (
        <div className="mt-3 space-y-1.5">
          <div className="h-1 w-full bg-dark-600 rounded-full overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-violet-500 to-cyan-500 rounded-full transition-all duration-500"
              style={{ width: `${progress}%` }}
            />
          </div>
          <p className="text-xs text-dark-400">{progressMessage}</p>
          {currentFile && (
            <p className="text-[10px] text-dark-400 truncate" title={currentFile}>
              {currentFile}
            </p>
          )}
        </div>
      )}

      {status === 'success' && (
        <div className="mt-3 text-xs text-emerald-400 animate-fade-in-up">
          Repository indexed successfully
        </div>
      )}

      {status === 'error' && (
        <div className="mt-3 text-xs text-red-400 animate-fade-in-up">
          {error}
        </div>
      )}
    </div>
  );
}
