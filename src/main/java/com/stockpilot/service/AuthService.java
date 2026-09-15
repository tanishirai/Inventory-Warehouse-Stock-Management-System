package com.stockpilot.service;

import com.stockpilot.dao.StaffDAO;
import com.stockpilot.exception.AuthenticationException;
import com.stockpilot.exception.DuplicateSkuException;
import com.stockpilot.io.FileLogger;
import com.stockpilot.model.StaffUser;
import com.stockpilot.util.InputValidator;
import com.stockpilot.util.PasswordUtil;

import java.sql.SQLException;

/** Handles staff registration and login, delegating persistence to StaffDAO. */
public class AuthService {
    private final StaffDAO staffDAO = new StaffDAO();

    public StaffUser register(String username, String password, String fullName, String role)
            throws DuplicateSkuException, SQLException {
        if (InputValidator.isBlank(username) || InputValidator.isBlank(password) || InputValidator.isBlank(fullName)) {
            throw new IllegalArgumentException("Username, password, and full name are all required.");
        }
        if (staffDAO.findByUsername(username).isPresent()) {
            throw new DuplicateSkuException("Username '" + username + "' is already taken.");
        }
        String normalizedRole = "ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "STAFF";
        StaffUser user = staffDAO.insert(username, PasswordUtil.hash(password), fullName, normalizedRole);
        FileLogger.log("Registered new staff user: " + user.getUsername() + " (" + normalizedRole + ")");
        return user;
    }

    public StaffUser login(String username, String password) throws AuthenticationException, SQLException {
        var storedHash = staffDAO.findPasswordHash(username);
        if (storedHash.isEmpty() || !PasswordUtil.matches(password, storedHash.get())) {
            FileLogger.log("Failed login attempt for username: " + username);
            throw new AuthenticationException("Invalid username or password.");
        }
        StaffUser user = staffDAO.findByUsername(username).orElseThrow();
        FileLogger.log("Staff logged in: " + user.getUsername());
        return user;
    }
}
