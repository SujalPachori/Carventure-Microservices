package com.carventure.adminreporting.kafka;

import com.carventure.adminreporting.event.UserRegisteredEvent;
import com.carventure.adminreporting.model.ReportType;
import com.carventure.adminreporting.service.ReportMetricUpdater;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final ReportMetricUpdater reportMetricUpdater;

    @KafkaListener(topics = "user.registered", groupId = "reporting-service")
    public void onUserRegistered(UserRegisteredEvent event) {
        reportMetricUpdater.increment(ReportType.USERS, "totalCustomers", 1);

        // plain for-loop over roles rather than a stream, to match your usermanagement style
        for (String role : event.getRoles()) {
            reportMetricUpdater.increment(ReportType.USERS, "usersByRole." + role, 1);
        }
    }
}
