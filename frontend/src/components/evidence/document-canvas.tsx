'use client';

import { useEffect, useRef, useState } from 'react';
import * as pdfjsLib from 'pdfjs-dist';
import { useAuthStore } from '@/stores/auth-store';
import { evidenceApi } from '@/lib/api/evidence';
import { Loader2, AlertCircle, ChevronLeft, ChevronRight, Download } from 'lucide-react';
import { clsx } from 'clsx';

pdfjsLib.GlobalWorkerOptions.workerSrc = `//cdnjs.cloudflare.com/ajax/libs/pdf.js/${pdfjsLib.version}/pdf.worker.min.mjs`;

interface DocumentCanvasProps {
  evidenceId: string;
  fileName: string;
  onDownloadFallback?: () => void;
}

export function DocumentCanvas({ evidenceId, fileName, onDownloadFallback }: DocumentCanvasProps) {
  const { user } = useAuthStore();
  
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const watermarkRef = useRef<HTMLCanvasElement>(null);
  
  const [pdfDoc, setPdfDoc] = useState<pdfjsLib.PDFDocumentProxy | null>(null);
  const [pageNum, setPageNum] = useState<number>(1);
  const [numPages, setNumPages] = useState<number>(0);
  
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    
    const loadPdf = async () => {
      try {
        setIsLoading(true);
        setError(null);
        
        const blob = await evidenceApi.getBlob(evidenceId);
        
        if (blob.type !== 'application/pdf' && !fileName.toLowerCase().endsWith('.pdf')) {
          throw new Error('This file format cannot be rendered in the sandboxed canvas. It must be downloaded for secure local viewing.');
        }

        const arrayBuffer = await blob.arrayBuffer();
        
        const loadingTask = pdfjsLib.getDocument({ data: arrayBuffer });
        const pdf = await loadingTask.promise;
        
        if (isMounted) {
          setPdfDoc(pdf);
          setNumPages(pdf.numPages);
          setPageNum(1);
        }
      } catch (err: any) {
        if (isMounted) {
          setError(err.message || 'Failed to load document.');
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };
    
    loadPdf();
    
    return () => {
      isMounted = false;
    };
  }, [evidenceId, fileName]);

  useEffect(() => {
    let renderTask: pdfjsLib.RenderTask | null = null;
    
    const renderPage = async () => {
      if (!pdfDoc || !canvasRef.current) return;
      
      try {
        const page = await pdfDoc.getPage(pageNum);
        const canvas = canvasRef.current;
        const ctx = canvas.getContext('2d');
        if (!ctx) return;
        
        const viewport = page.getViewport({ scale: 1.5 });
        canvas.height = viewport.height;
        canvas.width = viewport.width;
        
        const renderContext = {
          canvasContext: ctx,
          viewport: viewport,
        };
        
        renderTask = page.render(renderContext as any);
        await renderTask.promise;
        
        drawWatermark(viewport.width, viewport.height);
        
      } catch (err: any) {
        if (err.name !== 'RenderingCancelledException') {
          console.error('Error rendering page:', err);
        }
      }
    };
    
    renderPage();
    
    return () => {
      if (renderTask) {
        renderTask.cancel();
      }
    };
  }, [pdfDoc, pageNum]);

  const drawWatermark = (width: number, height: number) => {
    if (!watermarkRef.current || !user) return;
    
    const canvas = watermarkRef.current;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    
    canvas.width = width;
    canvas.height = height;
    
    ctx.clearRect(0, 0, width, height);
    
    const text = `${user.fullName?.toUpperCase()} - ID: ${user.badgeNumber || user.id.substring(0, 8).toUpperCase()} - ${new Date().toISOString()}`;
    
    ctx.font = 'bold 24px monospace';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.15)'; 
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    
    const stepX = 400;
    const stepY = 250;
    
    ctx.translate(width / 2, height / 2);
    ctx.rotate((-45 * Math.PI) / 180);
    ctx.translate(-width / 2, -height / 2);
    
    for (let x = -width; x < width * 2; x += stepX) {
      for (let y = -height; y < height * 2; y += stepY) {
        ctx.fillText(text, x, y);
      }
    }
    
    ctx.setTransform(1, 0, 0, 1, 0, 0);
  };

  const handleContextMenu = (e: React.MouseEvent) => {
    e.preventDefault();
  };

  if (isLoading) {
    return (
      <div className="w-full h-96 flex flex-col items-center justify-center bg-zinc-950 border border-zinc-800 rounded-xl">
        <Loader2 className="w-8 h-8 text-zinc-500 animate-spin mb-4" />
        <span className="text-zinc-400 font-mono text-xs">Securely fetching binary blob...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="w-full h-96 flex flex-col items-center justify-center bg-zinc-950 border border-zinc-800 rounded-xl p-6 text-center">
        <AlertCircle className="w-10 h-10 text-red-500 mb-4" />
        <h3 className="text-zinc-100 font-bold mb-2">Unsupported Preview</h3>
        <p className="text-zinc-400 text-xs mb-6 max-w-sm">
          {error}
        </p>
        <button onClick={onDownloadFallback} className="btn-primary">
          <Download className="w-4 h-4" />
          <span>Download Safely</span>
        </button>
      </div>
    );
  }

  return (
    <div className="flex flex-col space-y-4">
      <div className="flex items-center justify-between bg-zinc-900 border border-zinc-800 rounded-lg p-2 px-4">
        <div className="text-xs font-mono text-zinc-400 truncate max-w-[200px] sm:max-w-md">
          {fileName}
        </div>
        
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-1">
            <button 
              onClick={() => setPageNum(p => Math.max(1, p - 1))}
              disabled={pageNum <= 1}
              className="p-1 rounded bg-zinc-800 text-zinc-300 disabled:opacity-30 hover:bg-zinc-700 transition-colors cursor-pointer"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="text-xs font-mono text-zinc-300 w-16 text-center">
              {pageNum} / {numPages}
            </span>
            <button 
              onClick={() => setPageNum(p => Math.min(numPages, p + 1))}
              disabled={pageNum >= numPages}
              className="p-1 rounded bg-zinc-800 text-zinc-300 disabled:opacity-30 hover:bg-zinc-700 transition-colors cursor-pointer"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      <div 
        className="relative w-full max-h-[800px] overflow-auto bg-zinc-950 border border-zinc-800 rounded-xl flex justify-center custom-scrollbar"
        onContextMenu={handleContextMenu}
        style={{ userSelect: 'none', WebkitUserSelect: 'none' }}
      >
        <div className="relative">
          <canvas 
            ref={canvasRef} 
            className="block"
          />
          <canvas 
            ref={watermarkRef} 
            className="absolute top-0 left-0 pointer-events-none opacity-80 mix-blend-screen"
          />
        </div>
      </div>
      <div className="text-center text-[10px] text-zinc-600 font-mono">
        Sandboxed UI • Digital Watermarking Active • No Copy/Paste
      </div>
    </div>
  );
}


