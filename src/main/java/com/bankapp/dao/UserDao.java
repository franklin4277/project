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
