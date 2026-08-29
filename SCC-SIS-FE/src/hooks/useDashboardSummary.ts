import useSWR from 'swr';
import api from '../shared/api/http';

const fetcher = (url: string) => api.get(url).then(res => res.data);

export function useDashboardSummary(centerId?: number | string | null) {
  let url = `/api/v1/dashboard/summary`;
  
  if (centerId !== undefined && centerId !== null && centerId !== '' && centerId !== 'null' && centerId !== 'undefined' && centerId !== 'all') {
    url += `?centerId=${centerId}`;
  }

  const { data, error, isLoading } = useSWR(url, fetcher, {
    revalidateOnFocus: false,
    dedupingInterval: 300000,
  });

  return { data, error, isLoading };
}
