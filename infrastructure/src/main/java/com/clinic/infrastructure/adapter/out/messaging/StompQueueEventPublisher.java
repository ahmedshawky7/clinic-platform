package com.clinic.infrastructure.adapter.out.messaging;

import com.clinic.application.port.out.QueueEventPublisherPort;
import com.clinic.domain.model.QueueSnapshot;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompQueueEventPublisher implements QueueEventPublisherPort {
    private final SimpMessagingTemplate messagingTemplate;

    public StompQueueEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publishQueueUpdate(QueueSnapshot snapshot) {
        String destination = "/topic/clinic/" + snapshot.clinicId() + "/doctor/" + snapshot.doctorId() + "/queue";
        messagingTemplate.convertAndSend(destination, snapshot);
    }
}
