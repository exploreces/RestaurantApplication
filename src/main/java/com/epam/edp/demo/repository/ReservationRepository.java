package com.epam.edp.demo.repository;


import com.epam.edp.demo.entity.Reservation;

import java.util.List;

public interface ReservationRepository {
    List<Reservation> getAllReservation();
    List<Reservation> getReservationByLocationIdDateTableId(String locationId, String date, String tableId);
    List<String> getAvailableTimeSlot(List<Reservation> conflictDateReservation);
    Reservation saveReservation(Reservation reservation);
    Reservation updateReservation(Reservation updatedReservation);
    boolean deleteReservation(String id);
    boolean statusChange(String id,String status,String email);
    Reservation findByReservationId(String reservationId);
    void updateReservationStatusesBasedOnTime(List<Reservation> reservations);
}
