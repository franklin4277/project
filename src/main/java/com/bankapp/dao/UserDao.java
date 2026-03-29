package com.bankapp.dao;

import com.bankapp.config.DatabaseManager;
import com.bankapp.enums.Role;
import com.bankapp.model.Client;
import com.bankapp.model.Manager;
import com.bankapp.model.Operator;
import com.bankapp.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserDao {
    public User authenticate(String username, String password) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, username, password, full_name, role, client_id FROM users WHERE username = ? AND password = ?")) {
            statement.setString(1, username);
            statement.setString(2, password);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public User findById(int userId) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, username, password, full_name, role, client_id FROM users WHERE id = ?")) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public boolean usernameExists(String username) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT 1 FROM users WHERE username = ? LIMIT 1")) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    public User createClientUser(String username, String password, String fullName) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int clientId;
                try (PreparedStatement clientStatement = connection.prepareStatement(
                    "INSERT INTO clients(full_name) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                    clientStatement.setString(1, fullName);
                    clientStatement.executeUpdate();
                    try (ResultSet keys = clientStatement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Failed to create client profile.");
                        }
                        clientId = keys.getInt(1);
                    }
                }

                int userId;
                try (PreparedStatement userStatement = connection.prepareStatement(
                    "INSERT INTO users(username,password,full_name,role,client_id) VALUES (?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                    userStatement.setString(1, username);
                    userStatement.setString(2, password);
                    userStatement.setString(3, fullName);
                    userStatement.setString(4, Role.CLIENT.name());
                    userStatement.setInt(5, clientId);
                    userStatement.executeUpdate();
                    try (ResultSet keys = userStatement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Failed to create user credentials.");
                        }
                        userId = keys.getInt(1);
                    }
                }

                connection.commit();
                return new Client(userId, username, password, fullName, clientId);
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private User map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String fullName = rs.getString("full_name");
        Role role = Role.valueOf(rs.getString("role"));
        Integer clientId = (Integer) rs.getObject("client_id");

        return switch (role) {
            case CLIENT -> new Client(id, username, password, fullName, clientId);
            case MANAGER -> new Manager(id, username, password, fullName);
            case OPERATOR -> new Operator(id, username, password, fullName);
        };
    }
}
