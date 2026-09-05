// src/shared/api/http.ts
import axios, { AxiosError } from 'axios';
import { ensureValidToken, keycloak } from '../../keycloak';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:7001', // fallback nếu không có env
    timeout: 120000, // 120 seconds for AI chat (Cohere + Qdrant + RAG can be slow)
});

// Cooldown để tránh spam toast (5 giây)
let lastNetworkErrorToastTime = 0;
const TOAST_COOLDOWN_MS = 5000;

// Track nếu modal đã được hiển thị để tránh spam modal
let backendErrorModalShown = false;

// Helper để kiểm tra lỗi mạng
function isNetworkError(error: AxiosError): boolean {
    return (
        error.code === 'ERR_NETWORK' ||
        error.code === 'ERR_FAILED' ||
        error.code === 'ECONNABORTED' ||
        error.message?.includes('timeout') ||
        error.message?.includes('Network Error')
    );
}

// Helper để hiển thị toast cảnh báo với cooldown
function showNetworkErrorToast() {
    const now = Date.now();
    if (now - lastNetworkErrorToastTime < TOAST_COOLDOWN_MS) {
        return; // Skip toast nếu chưa đủ cooldown
    }
    lastNetworkErrorToastTime = now;

    // Dispatch custom event để toast provider có thể hiển thị
    const event = new CustomEvent('backend-network-error', {
        detail: {
            message: 'Không thể kết nối đến máy chủ',
            description: 'Vui lòng kiểm tra kết nối mạng hoặc thử lại sau.',
        },
    });
    window.dispatchEvent(event);
}

// Helper để hiển thị modal backend error
function showBackendErrorModal() {
    const event = new CustomEvent('backend-unavailable', {});
    window.dispatchEvent(event);
}

// ========== SMART IN-MEMORY CACHE & REQUEST DEDUPLICATION ==========
// Lưu trữ kết quả của các GET request trong RAM để khi chuyển qua lại giữa các trang (Users, Centers, Classes...)
// dữ liệu hiển thị tức thì (0ms) mà không phải chờ 2-3s qua mạng sang Render.
interface CacheEntry {
    data: any;
    status: number;
    statusText: string;
    headers: any;
    timestamp: number;
}

const apiCache = new Map<string, CacheEntry>();
const pendingRequests = new Map<string, Promise<any>>();
const CACHE_TTL_MS = 60 * 1000; // Cache 60 giây cho mỗi trang

// Tạo key định danh cho request dựa trên URL và Params
function getCacheKey(config: any): string {
    const paramsStr = config.params ? JSON.stringify(config.params) : '';
    return `${config.url || ''}?${paramsStr}`;
}

// Xóa cache khi có thao tác làm thay đổi dữ liệu (POST, PUT, PATCH, DELETE)
export function clearApiCache(urlPattern?: string) {
    if (!urlPattern) {
        apiCache.clear();
        return;
    }
    for (const key of apiCache.keys()) {
        if (key.includes(urlPattern)) {
            apiCache.delete(key);
        }
    }
}

// ========== REQUEST INTERCEPTOR ==========
api.interceptors.request.use(async (config) => {
    const token = await ensureValidToken(30);
    
    if (token) {
        if (!config.headers) {
            config.headers = {} as any;
        }
        config.headers.Authorization = `Bearer ${token}`;
    }

    const method = (config.method || 'get').toLowerCase();

    // Nếu là thao tác ghi dữ liệu (POST, PUT, PATCH, DELETE), xóa cache liên quan
    if (method !== 'get') {
        const path = (config.url || '').split('?')[0];
        const baseEndpoint = path.split('/').slice(0, 3).join('/'); // Ví dụ: /api/users, /api/classes
        clearApiCache(baseEndpoint);
        return config;
    }

    // Với GET request: kiểm tra xem có cache còn hạn không
    const cacheKey = getCacheKey(config);
    const cached = apiCache.get(cacheKey);

    if (cached && (Date.now() - cached.timestamp < CACHE_TTL_MS)) {
        // Trả về dữ liệu từ RAM ngay tức thì (0ms) bằng Axios adapter giả lập
        config.adapter = async () => ({
            data: cached.data,
            status: cached.status,
            statusText: cached.statusText,
            headers: cached.headers,
            config,
            request: {},
        });
    }

    return config;
});

// ========== RESPONSE INTERCEPTOR ==========
api.interceptors.response.use(
    (res) => {
        const method = (res.config.method || 'get').toLowerCase();
        // Chỉ lưu cache cho GET request thành công và không phải file export/download
        if (method === 'get' && res.status >= 200 && res.status < 300) {
            const isBinary = res.config.responseType === 'blob' || res.config.responseType === 'arraybuffer';
            if (!isBinary) {
                const cacheKey = getCacheKey(res.config);
                apiCache.set(cacheKey, {
                    data: res.data,
                    status: res.status,
                    statusText: res.statusText,
                    headers: res.headers,
                    timestamp: Date.now(),
                });
            }
        }
        return res;
    },
    async (err: AxiosError<any>) => {
        const status = err.response?.status;

        // Xử lý lỗi mạng
        if (isNetworkError(err)) {
            // Hiển thị toast cảnh báo (với cooldown)
            showNetworkErrorToast();
            
            // Hiển thị modal nếu backend không khả dụng (chỉ lần đầu tiên)
            if (!backendErrorModalShown) {
                backendErrorModalShown = true;
                showBackendErrorModal();
            }
            
            return Promise.reject(err);
        }

        if (status === 401) {
            try {
                await keycloak.login();
            } catch (loginError) {
            }
        }

        // Format error message from backend
        if (err.response?.data) {
            const backendMessage = err.response.data.message || err.response.data.error;
            if (backendMessage) {
                err.message = backendMessage;
            }
        }

        return Promise.reject(err);
    },
);

export default api;
