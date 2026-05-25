import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';

interface Props {
  language: string;
  code: string;
}

export function CodeBlock({ language, code }: Props) {
  return (
    <div className="my-2 rounded-xl overflow-hidden border border-dark-500 w-full max-w-full">
      <div className="flex items-center justify-between px-3 py-1.5 bg-dark-800 border-b border-dark-500 shrink-0">
        <span className="text-[10px] font-mono text-dark-400 uppercase">{language}</span>
        <span className="text-[10px] text-dark-400">code</span>
      </div>
      <div className="overflow-x-auto w-full">
        <SyntaxHighlighter
          style={vscDarkPlus as never}
          language={language}
          PreTag="div"
          customStyle={{ margin: 0, background: '#0d1117', fontSize: '12px', padding: '12px' }}
        >
          {code}
        </SyntaxHighlighter>
      </div>
    </div>
  );
}

