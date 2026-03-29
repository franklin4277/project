package com.bankapp.service;

import com.bankapp.dao.UserDao;
import com.bankapp.exception.BankException;
import com.bankapp.model.User;
import com.bankapp.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Locale;

public class AuthService {
    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User login(String username, String password) throws BankException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BankException("Username and password are required.");
        }

        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);

        try {
            User user = userDao.findByUsername(normalizedUsername);
            if (user == null || !PasswordUtil.verifyPassword(password, user.getPassword())) {
                throw new BankException("Invalid credentials.");
            }

            // Seamless migration for older plain-text records.
            if (!PasswordUtil.isHashed(user.getPassword())) {
                userDao.updatePassword(user.getId(), PasswordUtil.hashPassword(password));
            }
            return user;
        } catch (SQLException e) {
            throw new BankException("Could not authenticate user: " + e.getMessage());
        }
    }

    public User registerClient(String fullName, String username, String password, String confirmPassword)
        throws BankException {
        if (fullName == null || fullName.isBlank()) {
            throw new BankException("Full name is required.");
        }
        if (username == null || username.isBlank()) {
            throw new BankException("Username is required.");
        }
        if (password == null || password.isBlank()) {
            throw new BankException("Password is required.");
        }
        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new BankException("Password confirmation is required.");
        }

        String trimmedName = fullName.trim();
        String trimmedUsername = username.trim().toLowerCase(Locale.ROOT);

        if (trimmedName.length() < 3) {
            throw new BankException("Full name must have at least 3 characters.");
        }
        if (!trimmedUsername.matches("[A-Za-z0-9._-]{3,30}")) {
            throw new BankException("Username must be 3-30 chars and contain only letters, numbers, ., _, -");
        }
        if (password.length() < 6) {
            throw new BankException("Password must have at least 6 characters.");
        }
        if (!password.equals(confirmPassword)) {
            throw new BankException("Passwords do not match.");
        }

        try {
            if (userDao.usernameExists(trimmedUsername)) {
                throw new BankException("Username is already taken.");
            }
            String passwordHash = PasswordUtil.hashPassword(password);
            return userDao.createClientUser(trimmedUsername, passwordHash, trimmedName);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed: users.username")) {
                throw new BankException("Username is already taken.");
            }
            throw new BankException("Could not create account: " + e.getMessage());
        }
    }
}
