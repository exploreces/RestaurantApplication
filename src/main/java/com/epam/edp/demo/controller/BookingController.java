package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.request.ReservationRequestDto;
import com.epam.edp.demo.dto.response.ReservationResponse;
import com.epam.edp.demo.dto.response.TableResponseDto;
import com.epam.edp.demo.exception.ResourceNotFoundException;
import com.epam.edp.demo.exception.UnAuthorizedException;
import com.epam.edp.demo.exception.ValidationException;
import com.epam.edp.demo.service.ReservationService;
import com.epam.edp.demo.service.impl.AuthService;
import com.epam.edp.demo.validation.Validation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/bookings")
public class BookingController {


    private final Validation validation;
    private final ReservationService reservationService;
    private final AuthService authService;

    private final Logger logger = LoggerFactory.getLogger(getClass());




    @GetMapping("/tables")
    public ResponseEntity<Map<String, Map<String, List<TableResponseDto>>>> getAvailableTable(
            @RequestParam(value = "date" , required = false) String date,
            @RequestParam(value = "locationId" , required = false) String locationId,
            @RequestParam(value = "time" , required = false) String time,
            @RequestParam(value = "guests" , required = false) String guests
            ){
        logger.info("In Bookings/tables Controller");
        validation.validateLocation(locationId);
        validation.validateTimeFrom(time);
        validation.validateGuests(guests);
        validation.validateDate(date);
        logger.info("Validation Passed Successfully");
        List<TableResponseDto> tables = reservationService.getAvailableSlots(locationId, date, time, guests);
        logger.info("Table Retrieved Successfully");

        Map<String, List<TableResponseDto>> innerMap = new HashMap<>();
        innerMap.put("tables", tables);

        Map<String, Map<String, List<TableResponseDto>>> response = new HashMap<>();
        response.put("data", innerMap);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/client")
    public ResponseEntity<Map<String,ReservationResponse>> createReservation(@RequestBody ReservationRequestDto reservationRequestDto
                             , @RequestHeader(value = "Authorization",required = false) String authHeader){
        //First Authorize it

// Extract token from header (assumes "Bearer <token>")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnAuthorizedException("Authorization token is missing or invalid");
        }
        try {
            String token = authHeader.substring(7);
            String email = authService.extractUserEmailFromToken(token);


            validation.validateReservationRequest(reservationRequestDto);
            ReservationResponse createdReservation = reservationService.createReservation(reservationRequestDto, email);
            Map<String,ReservationResponse> response=new HashMap<>();
            response.put("data",createdReservation);



            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }
        catch (ValidationException e){
            throw new ValidationException(e.getMessage());
        }
        catch(ResourceNotFoundException e){
            throw new ResourceNotFoundException(e.getMessage());
        }
        catch (RuntimeException e){
            throw new UnAuthorizedException("Authorization token is missing or invalid");
        }
    }








}
