import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import type { LucideIcon } from 'lucide-react';
import {
    Building2,
    Users,
    Shield,
    UserPlus,
    Settings,
    BookOpen,
    Sparkles,
    UserCheck,
    AlertTriangle,
} from 'lucide-react';
import { useCenterSelection, useEnsureCenterLoaded } from '../../../../stores/centerSelection';

// Import components
import Stats from '@/features/users/pages/dashboard/components/stats';
import DashboardSkeleton from '@/features/users/pages/dashboard/components/DashboardSkeleton';
import SystemStatus from '@/features/users/pages/dashboard/components/system-status';
import StudentWarnings from '@/features/users/pages/dashboard/components/StudentWarnings';
import QuickActions from '@/features/users/pages/dashboard/components/quick-actions';
import RecentClasses from '@/features/users/pages/dashboard/components/RecentClasses';

// Import APIs & Hooks
import { useUserProfile } from '../../../../stores/userProfile';
import { useDashboardSummary } from '../../../../hooks/useDashboardSummary';

export default function DashboardPage() {
    const navigate = useNavigate();
    const { me, loading: userLoading } = useUserProfile();
    useEnsureCenterLoaded();
    const selectedCenterId = useCenterSelection((s) => s.selectedCenterId);

    // SWR Hook cho cache
    const { data: summaryData, isLoading: isSummaryLoading } = useDashboardSummary(selectedCenterId);

    const [backgroundImage, setBackgroundImage] = useState<string | null>(null);
    const [warningsCount, setWarningsCount] = useState<number>(0);

    // Redirect students and lecturers to their respective pages
    useEffect(() => {
        if (!userLoading && me) {
            const isStudent = me.roles?.some((role) => role.code === 'STUDENT') ?? false;
            const isLecturer = me.roles?.some((role) => role.code === 'LECTURER') ?? false;

            if (isStudent) {
                navigate('/my-classes', { replace: true });
            } else if (isLecturer) {
                navigate('/classes', { replace: true });
            }
        }
    }, [me, userLoading, navigate]);

    useEffect(() => {
        // Load background image from settings
        const savedAppearance = localStorage.getItem('appearanceSettings');
        if (savedAppearance) {
            const parsed = JSON.parse(savedAppearance);
            if (parsed.backgroundImage) {
                setBackgroundImage(parsed.backgroundImage);
            }
        }

        const handleStorageChange = () => {
            const savedAppearance = localStorage.getItem('appearanceSettings');
            if (savedAppearance) {
                const parsed = JSON.parse(savedAppearance);
                setBackgroundImage(parsed.backgroundImage || null);
            }
        };

        window.addEventListener('storage', handleStorageChange);
        return () => window.removeEventListener('storage', handleStorageChange);
    }, []);

    const stats = [
        {
            label: 'Tổng số học viên',
            value: summaryData?.totalStudents?.toString() || '0',
            sub: `${summaryData?.totalStudents || 0} hồ sơ học viên`,
            change: null,
            changeType: 'neutral' as const,
            icon: Users as React.ComponentType<{ size?: number }>,
            iconColor: 'from-green-500 to-emerald-500',
            bgGradient: 'from-green-50 to-emerald-50',
            glowColor: 'shadow-green-200',
            onClick: () => navigate('/students'),
        },
        {
            label: 'Trung tâm',
            value: summaryData?.totalCenters?.toString() || '0',
            sub: `${summaryData?.totalCenters || 0} trung tâm hoạt động`,
            change: null,
            changeType: 'neutral' as const,
            icon: Building2 as React.ComponentType<{ size?: number }>,
            iconColor: 'from-indigo-500 to-purple-500',
            bgGradient: 'from-indigo-50 to-purple-50',
            glowColor: 'shadow-indigo-200',
            onClick: () => navigate('/centers'),
        },
        {
            label: 'Lớp đang hoạt động',
            value: summaryData?.totalClasses?.toString() || '0',
            sub: `${summaryData?.totalClasses || 0} lớp đang hoạt động`,
            change: null,
            changeType: 'neutral' as const,
            icon: BookOpen as React.ComponentType<{ size?: number }>,
            iconColor: 'from-purple-500 to-violet-500',
            bgGradient: 'from-purple-50 to-violet-50',
            glowColor: 'shadow-purple-200',
            onClick: () => navigate('/classes'),
        },
        {
            label: 'Giảng viên',
            value: summaryData?.totalLecturers?.toString() || '0',
            sub: `${summaryData?.totalLecturers || 0} giảng viên`,
            change: null,
            changeType: 'neutral' as const,
            icon: UserCheck as React.ComponentType<{ size?: number }>,
            iconColor: 'from-blue-500 to-cyan-500',
            bgGradient: 'from-blue-50 to-cyan-50',
            glowColor: 'shadow-blue-200',
            onClick: () => navigate('/users'),
        },
    ];

    const systemServices = [
        {
            icon: Building2 as React.ComponentType<{ size?: number }>,
            label: 'Trung tâm',
            status: 'Hoạt động bình thường',
            color: 'from-green-500 to-emerald-500',
        },
        {
            icon: Users as React.ComponentType<{ size?: number }>,
            label: 'Người dùng',
            status: 'Hoạt động bình thường',
            color: 'from-blue-500 to-cyan-500',
        },
        {
            icon: BookOpen as React.ComponentType<{ size?: number }>,
            label: 'Lớp học',
            status: 'Hoạt động bình thường',
            color: 'from-purple-500 to-violet-500',
        },
    ];

    if (userLoading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900 mx-auto"></div>
                    <p className="mt-4 text-gray-600">Đang tải...</p>
                </div>
            </div>
        );
    }

    return (
        <div
            className="dashboard-container space-y-8 px-6 py-8 w-full relative m-0"
            style={{
                minHeight: '100vh',
                backgroundImage: backgroundImage ? `url(${backgroundImage})` : 'none',
                backgroundSize: 'cover',
                backgroundPosition: 'center',
                backgroundRepeat: 'no-repeat',
                backgroundAttachment: 'fixed',
            }}
        >
            {backgroundImage && <div className="fixed inset-0 bg-black/20 pointer-events-none z-0"></div>}

            <div className="relative z-20">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-gray-900">Dashboard Trung tâm</h1>
                    <p className="text-gray-600 mt-1">
                        {selectedCenterId ? 'Tổng quan theo trung tâm đã chọn' : 'Tổng quan toàn hệ thống'}
                    </p>
                </div>

                {isSummaryLoading ? (
                    <DashboardSkeleton />
                ) : (
                    <div className="mb-12 relative z-20">
                        <Stats stats={stats} isLoaded={true} />
                    </div>
                )}

                <div className="mb-8 grid grid-cols-1 lg:grid-cols-2 gap-6 relative z-20">
                    <QuickActions
                        isLoaded={true}
                        actions={[
                            {
                                label: 'Thêm học viên',
                                color: 'bg-gradient-to-br from-blue-500 to-blue-600',
                                icon: UserPlus,
                                onClick: () => navigate('/students?action=create'),
                            },
                            {
                                label: 'Tạo lớp học',
                                color: 'bg-gradient-to-br from-purple-500 to-violet-600',
                                icon: BookOpen,
                                onClick: () => navigate('/classes?action=create'),
                            },
                            {
                                label: 'Xem báo cáo',
                                color: 'bg-gradient-to-br from-emerald-500 to-green-600',
                                icon: Sparkles,
                                onClick: () => navigate('/statistics'),
                            },
                            {
                                label: 'Quản lý lịch',
                                color: 'bg-gradient-to-br from-amber-500 to-orange-600',
                                icon: Settings,
                                onClick: () => navigate('/classes'),
                            },
                        ]}
                    />
                    <RecentClasses />
                </div>

                <div className="mb-8 relative z-20">
                    <StudentWarnings onCountChange={(c) => setWarningsCount(c)} />
                </div>

                <div className="mb-8 relative z-20">
                    <SystemStatus services={systemServices} isLoaded={true} />
                </div>
            </div>
        </div>
    );
}
