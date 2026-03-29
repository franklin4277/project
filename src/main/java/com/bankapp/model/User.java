package com.bankapp.model;

import com.bankapp.enums.Role;

public class User {
    private final int id;
    private final String username;
    private final String password;
    private final String fullName;
    private final Role role;
    private final Integer clientId;

    public User(int id, String username, String password, String fullName, Role role, Integer clientId) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.clientId = clientId;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public Integer getClientId() {
        return clientId;
    }
}
