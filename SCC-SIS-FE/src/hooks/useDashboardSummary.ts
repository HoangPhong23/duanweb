import useSWR from 'swr';
import axiosClient from '../shared/api/axiosClient';

const fetcher = (url: string) => axiosClient.get(url).then(res => res.data);

export function useDashboardSummary(centerId?: number) {
  const url = centerId 
    ? `/api/v1/dashboard/summary?centerId=${centerId}` 
    : `/api/v1/dashboard/summary`;

  const { data, error, isLoading } = useSWR(url, fetcher, {
    revalidateOnFocus: false,
    dedupingInterval: 300000,
  });

  return { data, error, isLoading };
}
