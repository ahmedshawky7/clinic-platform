package com.clinic.application.port.in;

public interface CancelTodayQueueUseCase {
    record CancelResult(int cancelledCount, boolean clinicClosed) {}
    CancelResult cancelTodayQueue(Long clinicId, Long doctorId, String reason, boolean closeClinic);
}