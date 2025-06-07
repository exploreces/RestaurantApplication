package com.epam.edp.demo.controller;

import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.epam.edp.demo.dto.request.ReservationRequestDto;
import com.epam.edp.demo.dto.response.ReservationResponse;
import com.epam.edp.demo.exception.UnAuthorizedException;
import com.epam.edp.demo.exception.ValidationException;
import com.epam.edp.demo.service.ReservationService;
import com.epam.edp.demo.service.impl.AuthService;
import com.epam.edp.demo.validation.Validation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {


    private final ReservationService reservationService;
    private final AuthService authService;
    private final Validation validation;

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<Map<String, Map<String, List<ReservationResponse>>>> getReservations(@RequestHeader(value = "Authorization",required = false) String authHeader) {

        //Authorize
        // Extract token from header (assumes "Bearer <token>")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnAuthorizedException("Authorization token is missing or invalid");
        }
        String email;
        try{
            String token = authHeader.substring(7);
            email= authService.extractUserEmailFromToken(token);
        }
        catch (RuntimeException e){
            throw new UnAuthorizedException("Authorization token is missing or invalid");
        }


        List<ReservationResponse> reservationResponse = reservationService.getAllReservation(email);
        Map<String, List<ReservationResponse>> innerMap = new HashMap<>();
        innerMap.put("reservation", reservationResponse);

        Map<String, Map<String, List<ReservationResponse>>> response = new HashMap<>();
        response.put("data", innerMap);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String,String>> getReservations(@PathVariable(required = false) String id, @RequestHeader(value = "Authorization",required = false) String authHeader) {
        //Authorize
        // Extract token from header (assumes "Bearer <token>")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnAuthorizedException("Authorization token is missing or invalid");
        }
        if(id==null || id.isBlank()){
            throw new ValidationException("Id is required");
        }
        try{
            String token = authHeader.substring(7);
            authService.extractUserEmailFromToken(token);//Just checking for valid token or not
        }
        catch (RuntimeException e){
            throw new UnAuthorizedException("Authorization token is missing or invalid");
        }

        boolean respone = reservationService.deleteReservationOfUser(id);
        Map<String,String> map=new HashMap<>();
        if (respone) {
            String message = "Deleted Reservation Successfully";
            map.put("message",message);
            return new ResponseEntity<>(map, HttpStatus.OK);
        } else {
            map.put("message","Could'nt Cancel the Reservation");
            return new ResponseEntity<>(map, HttpStatus.BAD_REQUEST);
        }
    }

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}/edit")
    public ResponseEntity<Map<String,ReservationResponse>> updateReservation(@PathVariable String id, @RequestBody ReservationRequestDto reservationRequestDto
            , @RequestHeader(value = "Authorization",required = false) String authHeader) {

        //Authorization
        // Extract token from header (assumes "Bearer <token>")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnAuthorizedException("Authorization token is missing or invalid");
        }

        String email;
        try{
            String token = authHeader.substring(7);
            email= authService.extractUserEmailFromToken(token);
        }
        catch (RuntimeException e){
            throw new UnAuthorizedException("Authorization token is missing or invalid");
        }

        validation.validateTimeFromReservation(reservationRequestDto.getTimeFrom());
        validation.validateGuests(reservationRequestDto.getGuestsNumber());
        validation.validateTimeTo(reservationRequestDto.getTimeTo(),reservationRequestDto.getTimeFrom());

        ReservationResponse updatedReservation = reservationService.updateReservation(
                id,
                email,
                reservationRequestDto
        );

        if (updatedReservation == null) {
            throw new ResourceNotFoundException("Reservation not found or you can't update it");
        }
        Map<String,ReservationResponse> response=new HashMap<>();
        response.put("data",updatedReservation);



        return new ResponseEntity<>(response, HttpStatus.OK);
    }


}

