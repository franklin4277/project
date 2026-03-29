package com.bankapp.model;

import com.bankapp.enums.Role;

public class Operator extends User {
    public Operator(int id, String username, String password, String fullName) {
        super(id, username, password, fullName, Role.OPERATOR, null);
    }
}
