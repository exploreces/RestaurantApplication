package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.request.AnonymousVisitorReservationDto;
import com.epam.edp.demo.dto.request.ReservationRequestDto;
import com.epam.edp.demo.dto.request.WaiterReservationRequestDto;
import com.epam.edp.demo.dto.response.ReservationResponse;
import com.epam.edp.demo.dto.response.TableResponseDto;
import com.epam.edp.demo.entity.Reservation;

import java.util.List;

public interface ReservationService {
    List<Reservation> getAllReservation();
    List<ReservationResponse> getAllReservation(String email);
    List<Reservation> getReservationByLocationIdDateTableId(String locationId, String date, String tableId);
    List<String> getAvailableTimeSlot(List<Reservation> conflictDateReservation,String time,String date);
    List<TableResponseDto> getAvailableSlots(String locationId, String date, String time, String guests);
    ReservationResponse createReservation(ReservationRequestDto reservationRequestDto, String email);
    ReservationResponse updateReservation(String reservationId, String userEmail, ReservationRequestDto updateRequest);
     boolean deleteReservationOfUser(String id);
    boolean deleteReservation(String id);
    boolean statusChange(String id,String status,String email);

    // Add the missing methods
    ReservationResponse createReservationByWaiter(WaiterReservationRequestDto requestDto, String customerEmail, String waiterEmail);
    List<ReservationResponse> getReservationsByWaiter(String waiterEmail);
    Reservation findReservationById(String reservationId);
    boolean cancelReservation(String reservationId);
    ReservationResponse createAnonymousReservation(AnonymousVisitorReservationDto requestDto, String visitorName, String waiterEmail);
    ReservationResponse postponeReservation(String reservationId, String newDate, String newTimeFrom, String newTimeTo);
}