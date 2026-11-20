package com.clinic.application.port.in;

import com.clinic.domain.model.QueueSnapshot;

public interface GetLiveQueueUseCase {
    QueueSnapshot getSnapshot(Long clinicId, Long doctorId);
}
