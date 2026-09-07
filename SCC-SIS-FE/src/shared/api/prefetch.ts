// src/shared/api/prefetch.ts
// Prefetch dữ liệu chung ngay sau khi đăng nhập để khi user click vào bất kỳ trang nào,
// dữ liệu đã có sẵn trong cache RAM frontend (0ms thay vì 3-4s qua mạng).
import api from './http';

let prefetched = false;

/**
 * Gọi song song tất cả API mà các trang hay dùng.
 * Kết quả sẽ tự động được lưu vào cache trong http.ts interceptor.
 * Chỉ chạy 1 lần duy nhất sau khi đăng nhập.
 */
export function prefetchCommonData() {
    if (prefetched) return;
    prefetched = true;

    // Chia làm 2 đợt (Waves) để tránh dội bom Render (Render free tier dễ bị nghẽn nếu gọi 12 API cùng lúc)
    // Wave 1: Các API cần cho Dashboard & Global UI (TopNav, Menu)
    const wave1 = [
        { url: '/api/dashboard/summary' },               // Dashboard
        { url: '/api/students/warnings' },               // Dashboard warnings
        { url: '/api/centers/lite' },                    // Global Dropdown
    ];

    // Wave 2: Các API của các trang con (Users, Classes, Students, Roles) - Delay 1 giây
    const wave2 = [
        { url: '/api/roles', params: { active: true } }, 
        { url: '/api/user-views' },                      
        { url: '/api/user-stats/roles' },                
        { url: '/api/classes' },                         
        { url: '/api/centers/all' },                     
        { url: '/api/students' },                        
        { url: '/api/permissions/groups' },              
        { url: '/api/programs' },                        
        { url: '/api/programs/lite' },                   
    ];

    // Chạy Wave 1 ngay lập tức
    for (const ep of wave1) {
        api.get(ep.url, { params: ep.params }).catch(() => {});
    }

    // Chạy Wave 2 sau 1000ms để nhường băng thông cho UI Dashboard render trước
    setTimeout(() => {
        for (const ep of wave2) {
            api.get(ep.url, { params: ep.params }).catch(() => {});
        }
    }, 1000);
}
