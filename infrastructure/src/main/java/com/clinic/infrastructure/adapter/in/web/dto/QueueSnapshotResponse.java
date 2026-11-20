package com.clinic.infrastructure.adapter.in.web.dto;

import java.time.LocalDateTime;

public record QueueSnapshotResponse(
        Long clinicId,
        Long doctorId,
        Integer currentQueueNumber,
        int waitingCount,
        long estimatedWaitMinutes,
        String doctorArrivalTime,
        LocalDateTime updatedAt
) {}