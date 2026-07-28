import { Calendar, Loader2, CheckCircle, XCircle, AlertCircle, KeyRound, Save } from 'lucide-react';
import { useState, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { type StudentAttendanceHistory, submitAttendanceCode } from '@/shared/api/attendance';
import { useToast } from '@/shared/hooks/useToast';
import { useUserProfile } from '@/stores/userProfile';
import { getMyClasses, type ClassDto } from '@/shared/api/classes';
import http from '@/shared/api/http';

export default function MyAttendancePage() {
    const [classes, setClasses] = useState<ClassDto[]>([]);
    const [selectedClass, setSelectedClass] = useState<ClassDto | null>(null);
    const [attendanceData, setAttendanceData] = useState<StudentAttendanceHistory | null>(null);
    const [loading, setLoading] = useState(true);
    const [selectedMonth, setSelectedMonth] = useState<number>(new Date().getMonth() + 1);
    const [selectedYear, setSelectedYear] = useState<number>(new Date().getFullYear());
    const toast = useToast();
    const { me } = useUserProfile();

    // ====== Nhập mã điểm danh ======
    const [attendanceCode, setAttendanceCode] = useState('');
    const [submittingCode, setSubmittingCode] = useState(false);
    const [codeSubmitted, setCodeSubmitted] = useState(false);

    useEffect(() => {
        loadClasses();
    }, [me]);

    useEffect(() => {
        if (selectedClass && me) {
            loadAttendance();
            setCodeSubmitted(false);
        }
    }, [selectedClass, me]);

    useEffect(() => {
        if (attendanceData?.records && attendanceData.records.length > 0) {
            const firstRecord = attendanceData.records[0];
            const date = new Date(firstRecord.attendanceDate);
            setSelectedYear(date.getFullYear());
            setSelectedMonth(date.getMonth() + 1);
        }
    }, [attendanceData]);

    const loadClasses = async () => {
        try {
            setLoading(true);
            const response = await getMyClasses();
            const myClasses = response.data;
            setClasses(myClasses);

            if (myClasses.length > 0 && !selectedClass) {
                setSelectedClass(myClasses[0]);
            }
        } catch (error: any) {
            toast.error('Không thể tải danh sách lớp');
        } finally {
            setLoading(false);
        }
    };

    const loadAttendance = async () => {
        if (!selectedClass || !me) return;

        try {
            setLoading(true);
            const response = await http.get<StudentAttendanceHistory>(
                `/api/attendance/my-attendance/${selectedClass.classId}`
            );
            setAttendanceData(response.data);
        } catch (error: any) {
            if (error?.response?.status !== 404) {
                toast.error('Không thể tải điểm danh');
            }
            setAttendanceData(null);
        } finally {
            setLoading(false);
        }
    };

    const handleSubmitCode = async () => {
        if (!attendanceCode.trim()) {
            toast.error('Vui lòng nhập từ khóa điểm danh');
            return;
        }
        if (!selectedClass) {
            toast.error('Vui lòng chọn lớp học');
            return;
        }

        setSubmittingCode(true);
        try {
            await submitAttendanceCode({
                classId: selectedClass.classId,
                attendanceCode: attendanceCode.trim().toUpperCase(),
            });
            toast.success('Điểm danh thành công! Bạn đã được ghi nhận có mặt.');
            setAttendanceCode('');
            setCodeSubmitted(true);
            setTimeout(() => loadAttendance(), 600);
        } catch (e: any) {
            const status = e?.response?.status;
            const msg = e?.response?.headers?.['x-error-message']
                || e?.response?.data?.message
                || e?.message;

            if (status === 400) {
                if (msg?.includes('không đúng') || msg?.toLowerCase().includes('wrong')) {
                    toast.error('Từ khóa điểm danh không đúng. Vui lòng kiểm tra lại.');
                } else if (msg?.includes('hết hạn')) {
                    toast.error('Từ khóa điểm danh đã hết hạn.');
                } else if (msg?.includes('không có buổi') || msg?.includes('không mở')) {
                    toast.error('Chưa có buổi điểm danh đang mở cho lớp này hôm nay.');
                } else {
                    toast.error(msg || 'Từ khóa không hợp lệ');
                }
            } else if (status === 404) {
                toast.error('Bạn không có trong danh sách điểm danh của lớp này.');
            } else {
                toast.error(msg || 'Không thể điểm danh. Vui lòng thử lại.');
            }
        } finally {
            setSubmittingCode(false);
        }
    };

    const filteredRecords = useMemo(() => {
        if (!attendanceData?.records) return [];
        return attendanceData.records.filter((record) => {
            const date = new Date(record.attendanceDate);
            return date.getMonth() + 1 === selectedMonth && date.getFullYear() === selectedYear;
        });
    }, [attendanceData, selectedMonth, selectedYear]);

    const filteredStats = useMemo(() => {
        const totalSessions = filteredRecords.length;
        const presentCount = filteredRecords.filter((r) => r.status === 'PRESENT').length;
        const absentCount = filteredRecords.filter((r) => r.status === 'ABSENT').length;
        const percentage = totalSessions > 0 ? Math.round((absentCount / totalSessions) * 100) : 0;
        return { totalSessions, presentCount, absentCount, percentage };
    }, [filteredRecords]);

    const months = Array.from({ length: 12 }, (_, i) => ({
        value: i + 1,
        label: `Tháng ${i + 1}`,
    }));

    const years = useMemo(() => {
        if (!attendanceData?.records || attendanceData.records.length === 0) {
            const currentYear = new Date().getFullYear();
            return [{ value: currentYear, label: `${currentYear}` }];
        }
        const yearSet = new Set<number>();
        attendanceData.records.forEach((record) => {
            if (record.attendanceDate) {
                yearSet.add(new Date(record.attendanceDate).getFullYear());
            }
        });
        return Array.from(yearSet)
            .sort((a, b) => b - a)
            .map((year) => ({ value: year, label: `${year}` }));
    }, [attendanceData]);

    return (
        <div className="space-y-6">
            <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-green-100 flex items-center justify-center">
                    <Calendar className="w-5 h-5 text-green-700" />
                </div>
                <h1 className="text-2xl font-bold text-gray-900">Điểm danh của tôi</h1>
            </div>

            {/* Modal style container (Chuẩn giao diện screenshot CMC) */}
            <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                {/* 1. Top Header Bar */}
                <div className="px-6 py-4 border-b border-gray-200 bg-white">
                    <div className="flex items-center justify-between flex-wrap gap-2">
                        <h2 className="text-base font-bold text-gray-900">
                            Học phần: {selectedClass?.name || 'Lớp học'} - 07:00 - 09:40 (Tiết 1-3)
                        </h2>
                        <span className="text-sm font-semibold text-red-600">
                            Vắng mặt({filteredStats.absentCount})
                        </span>
                    </div>
                    <div className="text-xs font-medium text-gray-600 mt-1">
                        Vắng mặt(Số buổi/Số tiết/Tỷ lệ):{' '}
                        <span className="font-bold text-gray-900">
                            ({filteredStats.absentCount}/{filteredStats.totalSessions * 3 || 9}/{filteredStats.percentage}%)
                        </span>
                    </div>
                </div>

                {/* 2. Toolbar & Input Từ khóa điểm danh */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 px-6 py-3 bg-gray-50/70 border-b border-gray-200">
                    <div>
                        <h3 className="text-sm font-bold text-gray-900">
                            Lớp: {selectedClass?.name || 'Chưa chọn lớp'}
                        </h3>
                        <p className="text-xs text-gray-500 font-medium mt-0.5">
                            &#123;Phòng học: VPC2-12A01&#125;
                        </p>
                    </div>

                    <div className="flex items-center gap-2">
                        <span className="text-xs font-medium text-gray-700 whitespace-nowrap">
                            Từ khóa điểm danh
                        </span>
                        <input
                            type="text"
                            placeholder="Từ khóa điểm danh"
                            value={attendanceCode}
                            onChange={(e) => setAttendanceCode(e.target.value.toUpperCase())}
                            onKeyDown={(e) => e.key === 'Enter' && handleSubmitCode()}
                            className="w-44 px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none uppercase font-semibold text-gray-800 bg-white"
                        />
                        <button
                            onClick={handleSubmitCode}
                            disabled={submittingCode || !attendanceCode.trim()}
                            className="bg-[#1a73e8] hover:bg-[#1557b0] text-white px-4 py-1.5 rounded-md text-sm font-semibold flex items-center gap-1.5 shadow-sm transition-all disabled:opacity-50"
                        >
                            {submittingCode ? (
                                <Loader2 size={16} className="animate-spin" />
                            ) : (
                                <Save size={16} />
                            )}
                            Lưu
                        </button>
                    </div>
                </div>

                {/* Status alert message */}
                <AnimatePresence>
                    {codeSubmitted && (
                        <motion.div
                            initial={{ opacity: 0, y: -4 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0 }}
                            className="px-6 py-2.5 bg-green-50 border-b border-green-100 flex items-center gap-2 text-xs text-green-700 font-medium"
                        >
                            <CheckCircle size={15} />
                            Đã ghi nhận điểm danh thành công cho buổi học hôm nay!
                        </motion.div>
                    )}
                </AnimatePresence>

                {/* 3. Selector & Table */}
                <div className="p-6 space-y-5">
                    {/* Class & Date Filter */}
                    <div className="flex flex-wrap items-center justify-between gap-4">
                        {classes.length > 0 && (
                            <div className="flex items-center gap-2">
                                <span className="text-xs font-medium text-gray-600">Chọn lớp:</span>
                                <div className="flex gap-2 overflow-x-auto">
                                    {classes.map((c) => (
                                        <button
                                            key={c.classId}
                                            onClick={() => setSelectedClass(c)}
                                            className={`px-3 py-1.5 rounded-md text-xs font-medium border transition-all ${
                                                selectedClass?.classId === c.classId
                                                    ? 'bg-blue-50 border-blue-500 text-blue-700 font-semibold'
                                                    : 'bg-white border-gray-200 text-gray-700 hover:bg-gray-50'
                                            }`}
                                        >
                                            {c.name}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}

                        <div className="flex items-center gap-2">
                            <span className="text-xs text-gray-600">Tháng:</span>
                            <select
                                value={selectedMonth}
                                onChange={(e) => setSelectedMonth(Number(e.target.value))}
                                className="text-xs border border-gray-300 rounded px-2 py-1 bg-white outline-none focus:ring-1 focus:ring-blue-500"
                            >
                                {months.map((m) => (
                                    <option key={m.value} value={m.value}>
                                        {m.label}
                                    </option>
                                ))}
                            </select>
                            <span className="text-xs text-gray-600">Năm:</span>
                            <select
                                value={selectedYear}
                                onChange={(e) => setSelectedYear(Number(e.target.value))}
                                className="text-xs border border-gray-300 rounded px-2 py-1 bg-white outline-none focus:ring-1 focus:ring-blue-500"
                            >
                                {years.map((y) => (
                                    <option key={y.value} value={y.value}>
                                        {y.label}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>

                    {/* Table Records */}
                    {loading ? (
                        <div className="text-center py-12 text-xs text-gray-500">
                            <Loader2 className="animate-spin h-6 w-6 text-blue-600 mx-auto mb-2" />
                            Đang tải lịch sử điểm danh...
                        </div>
                    ) : filteredRecords.length > 0 ? (
                        <div className="border border-gray-200 rounded-lg overflow-hidden">
                            <table className="w-full text-left border-collapse">
                                <thead className="bg-[#f8f9fa] border-b border-gray-200">
                                    <tr>
                                        <th className="px-4 py-2.5 text-xs font-bold text-gray-700 w-16 text-center">STT</th>
                                        <th className="px-4 py-2.5 text-xs font-bold text-gray-700">Ngày điểm danh</th>
                                        <th className="px-4 py-2.5 text-xs font-bold text-gray-700 text-center">Trạng thái</th>
                                        <th className="px-4 py-2.5 text-xs font-bold text-gray-700">Giảng viên</th>
                                        <th className="px-4 py-2.5 text-xs font-bold text-gray-700">Ghi chú</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-200 text-xs text-gray-800">
                                    {filteredRecords.map((record, index) => (
                                        <tr key={record.sessionId} className="hover:bg-gray-50 transition-colors">
                                            <td className="px-4 py-2.5 text-center font-medium text-gray-600">{index + 1}</td>
                                            <td className="px-4 py-2.5 font-medium">
                                                {new Date(record.attendanceDate).toLocaleDateString('vi-VN', {
                                                    day: '2-digit',
                                                    month: '2-digit',
                                                    year: 'numeric',
                                                })}
                                            </td>
                                            <td className="px-4 py-2.5 text-center">
                                                <span
                                                    className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                                                        record.status === 'PRESENT'
                                                            ? 'bg-green-100 text-green-700'
                                                            : 'bg-red-100 text-red-700'
                                                    }`}
                                                >
                                                    {record.status === 'PRESENT' ? (
                                                        <><CheckCircle size={12} className="mr-1" /> Có mặt</>
                                                    ) : (
                                                        <><XCircle size={12} className="mr-1" /> Vắng</>
                                                    )}
                                                </span>
                                            </td>
                                            <td className="px-4 py-2.5 font-medium text-gray-700">{record.teacherName || '—'}</td>
                                            <td className="px-4 py-2.5 text-gray-600">{record.notes || '—'}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    ) : (
                        <div className="text-center py-10 bg-gray-50 rounded-lg border border-gray-200">
                            <AlertCircle className="w-8 h-8 text-gray-400 mx-auto mb-2" />
                            <p className="text-xs font-medium text-gray-600">Không có dữ liệu điểm danh trong tháng {selectedMonth}/{selectedYear}</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
