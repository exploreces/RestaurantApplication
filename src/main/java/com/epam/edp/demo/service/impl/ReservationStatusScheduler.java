package com.epam.edp.demo.service.impl;

import com.epam.edp.demo.repository.impl.DynamoDbReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationStatusScheduler {

    private final DynamoDbReservationRepository reservationRepository;

    @Scheduled(cron = "0 30 10 * * *",zone = "Asia/Kolkata")   // 10:30 For in progress
    @Scheduled(cron = "0 0 12 * * *",zone = "Asia/Kolkata")   // 12:00 For FINISHED
    @Scheduled(cron = "0 15 12 * * *",zone = "Asia/Kolkata")   // 12:15 For IN_PROGRESS
    @Scheduled(cron = "0 45 13 * * *",zone = "Asia/Kolkata")  // 13:45 For Finished
    @Scheduled(cron = "0 00 14 * * *",zone = "Asia/Kolkata")  // 14:00 For IN_PROGRESS
    @Scheduled(cron = "0 30 15 * * *",zone = "Asia/Kolkata")  // 15:30 For FINISHED
    @Scheduled(cron = "0 45 15 * * *",zone = "Asia/Kolkata")  // 15:45 For INPROGRESS
    @Scheduled(cron = "0 15 17 * * *",zone = "Asia/Kolkata")  // 17:15 For FINISHED
    @Scheduled(cron = "0 30 17 * * *",zone = "Asia/Kolkata")  // 17:30 For IN_PROGRESS
    @Scheduled(cron = "0 0 19 * * *",zone = "Asia/Kolkata")   // 19:00 For FINISHED
    @Scheduled(cron = "0 15 19 * * *",zone = "Asia/Kolkata")   // 19:15 For INPROGRESS
    @Scheduled(cron = "0 45 20 * * *",zone = "Asia/Kolkata")  // 20:45 For FINISHED
    @Scheduled(cron = "0 00 21 * * *",zone = "Asia/Kolkata")  // 21:00 For INPROGRESS
    @Scheduled(cron = "0 30 22 * * *",zone = "Asia/Kolkata")  // 22:30 For FINISHED
    public void scheduledStatusUpdate() {
        reservationRepository.updateReservationStatusesBasedOnTime(reservationRepository.getAllReservation());
    }
}
