package com.epam.edp.demo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.epam.edp.demo.entity.Roles;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.repository.UserRepository;

/**
 * Service for verifying customer information
 */
@Service
public class CustomerVerificationService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UserRepository userRepository;

    @Autowired
    public CustomerVerificationService(UserRepository userRepository) {
        this.userRepository = userRepository;
        logger.info("CustomerVerificationService initialized");
    }

    /**
     * Checks if the provided email exists in the users table with the role of "customer"
     *
     * @param email The email to check
     * @return true if the email belongs to a customer, false otherwise
     */
    public boolean isCustomer(String email) {
        if (email == null || email.isEmpty()) {
            logger.warn("Cannot verify customer with null or empty email");
            return false;
        }

        try {
            logger.info("Checking if email {} belongs to a customer", email);
            User user = userRepository.findByEmail(email);

            if (user == null) {
                logger.info("No user found with email: {}", email);
                return false;
            }

            boolean isCustomer = Roles.CUSTOMER.equals(user.getRoles());
            logger.info("Email {} belongs to a user with role: {}. Is customer: {}",
                    email, user.getRoles(), isCustomer);

            return isCustomer;
        } catch (Exception e) {
            logger.error("Error verifying customer status for email {}: {}", email, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Retrieves a customer's full name by their email address
     *
     * @param email The email of the customer
     * @return The customer's full name if found, empty string otherwise
     */
    public String getCustomerFullName(String email) {
        if (email == null || email.isEmpty()) {
            logger.warn("Cannot get customer name with null or empty email");
            return "";
        }

        try {
            logger.info("Retrieving customer name for email: {}", email);
            User user = userRepository.findByEmail(email);

            if (user == null) {
                logger.info("No user found with email: {}", email);
                return "";
            }

            if (!Roles.CUSTOMER.equals(user.getRoles())) {
                logger.info("User with email {} is not a customer", email);
                return "";
            }

            String firstName = user.getFirstName() != null ? user.getFirstName() : "";
            String lastName = user.getLastName() != null ? user.getLastName() : "";
            String fullName = (firstName + " " + lastName).trim();

            logger.info("Found customer name for {}: {}", email, fullName);
            return fullName;
        } catch (Exception e) {
            logger.error("Error retrieving customer name for email {}: {}", email, e.getMessage(), e);
            return "";
        }
    }
}