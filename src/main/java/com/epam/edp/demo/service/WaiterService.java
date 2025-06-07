package com.epam.edp.demo.service;

import com.epam.edp.demo.entity.Waiter;
import com.epam.edp.demo.exception.ResourceNotFoundException;
import com.epam.edp.demo.repository.WaiterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class WaiterService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final WaiterRepository waiterRepository;

    @Autowired
    public WaiterService(WaiterRepository waiterRepository) {
        this.waiterRepository = waiterRepository;
        logger.info("WaiterService initialized");
    }


    public boolean isWaiter(String email) {
        logger.info("Checking if user with email {} is a waiter", email);
        return waiterRepository.isWaiter(email);
    }


    public String assignWaiterForReservation(String locationId) {
        logger.info("Assigning waiter for reservation at location {}", locationId);
        try {
            String waiterEmail = waiterRepository.getLeastBusyWaiterForLocation(locationId);
            logger.info("Assigned waiter {} for location {}", waiterEmail, locationId);
            return waiterEmail;
        } catch (ResourceNotFoundException e) {
            logger.error("No waiters available at location {}", locationId);
            throw e;
        } catch (Exception e) {
            logger.error("Error assigning waiter for location {}: {}", locationId, e.getMessage());
            throw new RuntimeException("Failed to assign waiter for reservation", e);
        }
    }


    public List<String> getAllWaiters() {
        logger.info("Getting all waiters");
        try {
            return waiterRepository.getAllWaiters();
        } catch (Exception e) {
            logger.error("Error getting all waiters: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get all waiters", e);
        }
    }

    public void addWaiter(String email, String password) {
        logger.info("Adding new waiter with email: {}", email);
        try {
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Email cannot be empty");
            }
            if (password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("Password cannot be empty");
            }

            waiterRepository.addWaiter(email, password);
            logger.info("Successfully added waiter: {}", email);
        } catch (Exception e) {
            logger.error("Error adding waiter {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to add waiter", e);
        }
    }


    public void removeWaiter(String email) {
        logger.info("Removing waiter with email: {}", email);
        try {
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Email cannot be empty");
            }

            waiterRepository.removeWaiter(email);
            logger.info("Successfully removed waiter: {}", email);
        } catch (Exception e) {
            logger.error("Error removing waiter {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to remove waiter", e);
        }
    }
}