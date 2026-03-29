package com.bankapp.model;

import com.bankapp.enums.Role;

public class Client extends User {
    public Client(int id, String username, String password, String fullName, int clientId) {
        super(id, username, password, fullName, Role.CLIENT, clientId);
    }
}
