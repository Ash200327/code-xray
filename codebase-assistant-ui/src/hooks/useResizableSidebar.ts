import { useState, useCallback } from 'react';

interface UseResizableSidebarOptions {
  initialWidth: number;
  minWidth: number;
  maxWidth: number;
  direction: 'left' | 'right';
}

export function useResizableSidebar({
  initialWidth,
  minWidth,
  maxWidth,
  direction,
}: UseResizableSidebarOptions) {
  const [width, setWidth] = useState(initialWidth);
  const [isDragging, setIsDragging] = useState(false);

  const startResizing = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      setIsDragging(true);
      const startWidth = width;
      const startX = e.clientX;

      const doDrag = (moveEvent: MouseEvent) => {
        const delta = moveEvent.clientX - startX;
        const newWidth = direction === 'left' ? startWidth + delta : startWidth - delta;
        if (newWidth >= minWidth && newWidth <= maxWidth) {
          setWidth(newWidth);
        }
      };

      const stopDrag = () => {
        setIsDragging(false);
        document.removeEventListener('mousemove', doDrag);
        document.removeEventListener('mouseup', stopDrag);
      };

      document.addEventListener('mousemove', doDrag);
      document.addEventListener('mouseup', stopDrag);
    },
    [width, minWidth, maxWidth, direction]
  );

  const resetWidth = useCallback(() => {
    setWidth(initialWidth);
  }, [initialWidth]);

  return { width, isDragging, startResizing, resetWidth };
}
