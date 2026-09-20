package com.shrihari.evault.repository;

import com.shrihari.evault.model.SecurityAlarm;
import com.shrihari.evault.model.SecurityAlarmLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityAlarmRepository extends JpaRepository<SecurityAlarm, UUID> {

    List<SecurityAlarm> findAllByOrderByTriggeredAtDesc();

    List<SecurityAlarm> findByAcknowledgedFalseOrderByTriggeredAtDesc();

    List<SecurityAlarm> findBySeverityOrderByTriggeredAtDesc(SecurityAlarmLevel severity);

    List<SecurityAlarm> findByCaseIdOrderByTriggeredAtDesc(UUID caseId);

    List<SecurityAlarm> findBySeverityAndAcknowledgedOrderByTriggeredAtDesc(SecurityAlarmLevel severity, boolean acknowledged);

    long countByAcknowledgedFalse();
}
