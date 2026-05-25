package com.flightrez.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.flightrez.enums.Role;

public class UserAccount {
    private final long id;
    private final String username;
    private final String fullName;
    private final String email;
    private final Role role;
    private final boolean staff;
    @JsonIgnore
    private final String password;

    public UserAccount(long id, String fullName, String email, Role role) {
        this(id, email, fullName, email, role, role == Role.ADMIN, "");
    }

    public UserAccount(long id, String fullName, String email, Role role, String password) {
        this(id, email, fullName, email, role, role == Role.ADMIN, password);
    }

    public UserAccount(long id, String username, String fullName, String email, Role role, boolean staff, String password) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.staff = staff;
        this.password = password;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public boolean isStaff() {
        return staff;
    }

    @JsonProperty("is_staff")
    public boolean getIsStaff() {
        return staff;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }
}
