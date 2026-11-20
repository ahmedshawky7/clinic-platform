package com.clinic.domain.model;

import java.time.LocalDateTime;

public record QueueSnapshot(
        Long clinicId,
        Long doctorId,
        Integer currentQueueNumber,
        int waitingCount,
        long estimatedWaitMinutes,
        String doctorArrivalTime,
        LocalDateTime updatedAt
) {}