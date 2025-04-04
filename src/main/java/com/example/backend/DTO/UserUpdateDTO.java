package com.example.backend.DTO;

import java.util.Set;

public class UserUpdateDTO {
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String gender;
    private boolean active;
    private Set<String> roles;
    private String password;

    // Getters và Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}