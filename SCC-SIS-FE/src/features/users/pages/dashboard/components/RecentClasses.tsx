import React, { useEffect, useMemo, useState } from 'react';
import { Calendar, Users as UsersIcon, Clock, Eye } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { listClasses, type ClassDto, type ClassStatus } from '@/shared/api/classes';
import { getAllStudentsWithEnrollments } from '@/shared/api/students';
import { useCenterSelection } from '@/stores/centerSelection';

type UiStatus = {
  label: string;
  className: string;
};

const statusMap: Record<ClassStatus, UiStatus> = {
  PLANNED: { label: 'Sắp khai giảng', className: 'bg-blue-50 text-blue-700' },
  ONGOING: { label: 'Đang diễn ra', className: 'bg-green-50 text-green-700' },
  FINISHED: { label: 'Đã kết thúc', className: 'bg-gray-50 text-gray-600' },
  CANCELLED: { label: 'Tạm dừng', className: 'bg-amber-50 text-amber-700' },
};

type ClassWithStudentCount = ClassDto & { activeStudentCount: number };

export default function RecentClasses() {
  const navigate = useNavigate();
  const selectedCenterId = useCenterSelection((s) => s.selectedCenterId);
  const [classes, setClasses] = useState<ClassWithStudentCount[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let isMounted = true;
    const run = async () => {
      try {
        const classesRes = await listClasses(selectedCenterId ? { centerId: selectedCenterId } : undefined);
        const data = Array.isArray(classesRes.data) ? classesRes.data : [];
        if (!isMounted) return;

        // Hiển thị ngay danh sách lớp gần đây với count tạm thời là 0
        const initialClasses = data.map((cls) => ({ ...cls, activeStudentCount: 0 }));
        const prio = (s: ClassStatus) => (s === 'PLANNED' ? 0 : s === 'ONGOING' ? 1 : 2);
        initialClasses.sort((a, b) => {
          const d = prio(a.status) - prio(b.status);
          if (d !== 0) return d;
          const getDate = (c: ClassDto) => new Date(c.startDate || c.createdAt).getTime();
          return getDate(a) - getDate(b);
        });
        setClasses(initialClasses.slice(0, 3));
        setLoading(false);

        // Tính toán số học viên active ở background mà không khóa giao diện
        getAllStudentsWithEnrollments().then((studentsRes) => {
          if (!isMounted) return;
          const allStudents = studentsRes?.data || [];
          if (Array.isArray(allStudents)) {
            setClasses((prev) =>
              prev.map((cls) => {
                let activeCount = 0;
                allStudents.forEach((student: any) => {
                  if (student.enrollments && Array.isArray(student.enrollments)) {
                    const hasActive = student.enrollments.some(
                      (e: any) => e.classId === cls.classId && e.status?.toUpperCase() === 'ACTIVE'
                    );
                    if (hasActive) activeCount++;
                  }
                });
                return { ...cls, activeStudentCount: activeCount };
              })
            );
          }
        }).catch(() => {});
      } catch (e) {
        if (isMounted) {
          setClasses([]);
          setLoading(false);
        }
      }
    };
    run();
    return () => {
      isMounted = false;
    };
  }, [selectedCenterId]);

  const items = useMemo(() => classes, [classes]);

  return (
    <section className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
      <div className="px-6 py-4 flex items-center justify-between border-b">
        <div className="text-lg font-semibold text-gray-900">Lớp học gần đây</div>
        <button onClick={() => navigate('/classes')} className="text-sm text-blue-600 hover:text-blue-700 font-medium flex items-center gap-1">
          Xem tất cả <Eye size={16} />
        </button>
      </div>
      {loading && (
        <div className="p-8 text-center text-gray-500">Đang tải danh sách lớp…</div>
      )}
      {!loading && items.length === 0 && (
        <div className="p-8 text-center text-gray-500">Chưa có lớp học</div>
      )}
      <div className="divide-y">
        {items.map((c) => {
          const s = statusMap[c.status];
          const start = c.startDate ? new Date(c.startDate) : null;
          const today = new Date();
          let daysText = '';
          if (start) {
            const diff = Math.ceil((start.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
            daysText = diff > 0 ? `Còn ${diff} ngày` : diff === 0 ? 'Hôm nay' : '';
          }
          return (
            <div key={c.classId} className="px-6 py-5 flex flex-col gap-2 hover:bg-gray-50">
              <div className="flex items-center gap-2">
                <div className="text-sm font-semibold text-gray-900">{c.name}</div>
                <span className={`text-xs px-2 py-1 rounded ${s.className}`}>{s.label}</span>
              </div>
              <div className="text-xs text-gray-500">{c.programName}</div>
              <div className="flex items-center gap-6 text-xs text-gray-600">
                <span className="inline-flex items-center gap-1"><Calendar size={14} />{c.startDate || 'Chưa đặt lịch'}</span>
                <span className="inline-flex items-center gap-1">
                  <UsersIcon size={14} />
                  {c.activeStudentCount}/{c.capacity || 30}
                </span>
                {daysText && (
                  <span className="inline-flex items-center gap-1"><Clock size={14} />{daysText}</span>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
