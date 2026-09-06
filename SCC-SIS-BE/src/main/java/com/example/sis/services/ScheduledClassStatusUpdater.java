package com.example.sis.services;

import com.example.sis.models.ClassEntity;
import com.example.sis.repositories.ClassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduledClassStatusUpdater {

    private static final Logger log = LoggerFactory.getLogger(ScheduledClassStatusUpdater.class);

    private final ClassRepository classRepository;
    private final StatusManagementService statusManagementService;

    public ScheduledClassStatusUpdater(ClassRepository classRepository, StatusManagementService statusManagementService) {
        this.classRepository = classRepository;
        this.statusManagementService = statusManagementService;
    }

    @Scheduled(cron = "0 0 0 * * *") // Chạy vào lúc nửa đêm mỗi ngày
    @Transactional
    public void autoUpdateClassStatuses() {
        log.info("Bắt đầu tự động cập nhật trạng thái các lớp học và tốt nghiệp học viên...");
        LocalDate today = LocalDate.now();

        // Tìm các lớp chưa FINISHED nhưng đã qua ngày kết thúc
        List<ClassEntity> classesToFinish = classRepository.findAllOrderByStartDateDesc().stream()
                .filter(c -> c.getStatus() != ClassEntity.ClassStatus.FINISHED && c.getStatus() != ClassEntity.ClassStatus.CANCELLED)
                .filter(c -> c.getEndDate() != null && c.getEndDate().isBefore(today))
                .collect(Collectors.toList());

        for (ClassEntity classEntity : classesToFinish) {
            try {
                classEntity.setStatus(ClassEntity.ClassStatus.FINISHED);
                classRepository.save(classEntity);
                
                statusManagementService.autoGraduateClassStudents(classEntity.getClassId(), null);
                log.info("Đã tự động tốt nghiệp học viên trong lớp ID: {}", classEntity.getClassId());
            } catch (Exception e) {
                log.error("Lỗi khi tự động cập nhật lớp {}: {}", classEntity.getClassId(), e.getMessage());
            }
        }
        log.info("Hoàn thành tự động cập nhật trạng thái lớp học.");
    }
}
