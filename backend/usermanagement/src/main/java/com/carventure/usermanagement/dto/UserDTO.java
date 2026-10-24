package com.carventure.usermanagement.dto;

import com.carventure.usermanagement.model.AccountStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    /**
     * ADDED: the database has always generated this, but it never left the
     * service, so nothing downstream could reference a user numerically.
     *
     * READ_ONLY means Jackson serialises it on responses but ignores it on
     * request bodies — a client cannot pick its own id.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer userId;

    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotBlank(message = "First Name cannot be blank")
    private String firstName;

    @NotBlank(message = "Last Name cannot be blank")
    private String lastName;

    @NotBlank(message = "Email cannot be blank")
    private String email;

    private String phoneNumber;

    private String address;

    @NotBlank(message = "Pincode cannot be blank")
    private String pincode;

    private LocalDate dateOfBirth;

    /**
     * ADDED: the entity has tracked this all along, but with it missing from
     * the DTO the admin console could not show whether a seller was pending,
     * approved or suspended. Set by the service, never by the client.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private AccountStatus accountStatus;
}
