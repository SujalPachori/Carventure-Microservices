package com.carventure.authservice.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserManagementUserDTO {

    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    private String pincode;
    private LocalDate dateOfBirth;
}