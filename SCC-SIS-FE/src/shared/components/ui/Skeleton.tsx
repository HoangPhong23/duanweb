import React from 'react';

// Nếu dự án có dùng utility `cn` hay `classNames`, mình sẽ tự gộp, nếu không thì fallback cơ bản.
export function Skeleton({
  className = "",
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={`animate-pulse rounded-md bg-slate-200/80 ${className}`}
      {...props}
    />
  )
}
