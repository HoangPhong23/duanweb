import React from 'react';

export default function DashboardSkeleton() {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
      {[1, 2, 3, 4].map((i) => (
        <div key={i} className="h-32 bg-gray-200/60 animate-pulse rounded-2xl border border-gray-100">
          <div className="p-6 h-full flex flex-col justify-between">
            <div className="h-4 bg-gray-300 rounded w-1/2"></div>
            <div className="h-8 bg-gray-300 rounded w-1/3 mt-4"></div>
          </div>
        </div>
      ))}
    </div>
  );
}
