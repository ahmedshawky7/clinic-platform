package com.clinic.application.port.out;

import com.clinic.domain.model.QueueSnapshot;

public interface QueueEventPublisherPort {
    void publishQueueUpdate(QueueSnapshot snapshot);
}