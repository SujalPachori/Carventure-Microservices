package com.carventure.car.dto;

import lombok.Data;

@Data
public class UserResponse {

    private Integer userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
}