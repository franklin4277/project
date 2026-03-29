package com.bankapp.model;

import com.bankapp.enums.Role;

public class Manager extends User {
    public Manager(int id, String username, String password, String fullName) {
        super(id, username, password, fullName, Role.MANAGER, null);
    }
}
