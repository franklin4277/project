package com.bankapp.service;

import com.bankapp.dao.UserDao;
import com.bankapp.exception.BankException;
import com.bankapp.model.User;

import java.sql.SQLException;

public class AuthService {
    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User login(String username, String password) throws BankException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BankException("Username and password are required.");
        }

        try {
            User user = userDao.authenticate(username.trim(), password);
            if (user == null) {
                throw new BankException("Invalid credentials.");
            }
            return user;
        } catch (SQLException e) {
            throw new BankException("Could not authenticate user: " + e.getMessage());
        }
    }
}
