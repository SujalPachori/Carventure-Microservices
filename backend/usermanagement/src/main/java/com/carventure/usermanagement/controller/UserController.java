package com.carventure.usermanagement.controller;

import com.carventure.usermanagement.dto.UserDTO;
import com.carventure.usermanagement.model.AccountStatus;
import com.carventure.usermanagement.service.UserService;
import com.carventure.usermanagement.userkafka.event.UserRegisteredEvent;
import com.carventure.usermanagement.userkafka.kafka.UserEventProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Tag(name = "User", description = "Endpoints for managing customer, seller/dealer, and admin accounts")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    // Customer accounts

    @Operation(summary = "Register a new customer account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer registered",
                    content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or missing fields", content = @Content),
            @ApiResponse(responseCode = "409", description = "Username or phone number already exists", content = @Content)
    })
    @PostMapping("/registerCustomer")
    public ResponseEntity<UserDTO> registerCustomer(@Valid @RequestBody UserDTO userDTO) {
        UserDTO registeredCustomer = userService.registerCustomer(userDTO);
        return new ResponseEntity<>(registeredCustomer, HttpStatus.CREATED);
    }

    @Operation(summary = "View all customers")
    @ApiResponse(responseCode = "200", description = "List of customers retrieved")
    @GetMapping("/viewAllCustomers")
    public ResponseEntity<List<UserDTO>> viewAllCustomers() {
        List<UserDTO> customers = userService.viewAllCustomers();
        return new ResponseEntity<>(customers, HttpStatus.OK);
    }


    @Operation(summary = "View a customer by username")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "404", description = "Customer not found", content = @Content)
    })
    @GetMapping("/viewCustomer/{customerId}")
    public ResponseEntity<UserDTO> viewCustomer(@PathVariable("customerId") String customerUsername) {
        UserDTO customer = userService.viewCustomer(customerUsername);
        return new ResponseEntity<>(customer, HttpStatus.OK);
    }

    @Operation(summary = "Update a customer's profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer updated"),
            @ApiResponse(responseCode = "400", description = "Invalid fields", content = @Content),
            @ApiResponse(responseCode = "404", description = "Customer not found", content = @Content)
    })
    @PutMapping("/updateCustomer/{customerId}")
    public ResponseEntity<UserDTO> updateCustomer(@PathVariable("customerId") String customerUsername,
                                                  @Valid @RequestBody UserDTO userDTO) {
        UserDTO updatedCustomer = userService.updateCustomer(customerUsername, userDTO);
        return new ResponseEntity<>(updatedCustomer, HttpStatus.OK);
    }

    @Operation(summary = "Delete a customer account")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Customer deleted"),
            @ApiResponse(responseCode = "404", description = "Customer not found", content = @Content)
    })
    @DeleteMapping("/deleteCustomer/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable("customerId") String customerUsername) {
        userService.deleteCustomer(customerUsername);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Seller / Dealer accounts

    @Operation(summary = "Register a new seller account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Seller registered"),
            @ApiResponse(responseCode = "400", description = "Invalid or missing fields", content = @Content),
            @ApiResponse(responseCode = "409", description = "Username or phone number already exists", content = @Content)
    })
    @PostMapping("/registerSeller")
    public ResponseEntity<UserDTO> registerSeller(@Valid @RequestBody UserDTO userDTO) {
        UserDTO registeredSeller = userService.registerSeller(userDTO);
        return new ResponseEntity<>(registeredSeller, HttpStatus.CREATED);
    }

    @Operation(summary = "Register a new dealer account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dealer registered"),
            @ApiResponse(responseCode = "400", description = "Invalid or missing fields", content = @Content),
            @ApiResponse(responseCode = "409", description = "Username or phone number already exists", content = @Content)
    })
    @PostMapping("/registerDealer")
    public ResponseEntity<UserDTO> registerDealer(@Valid @RequestBody UserDTO userDTO) {
        UserDTO registeredDealer = userService.registerDealer(userDTO);
        return new ResponseEntity<>(registeredDealer, HttpStatus.CREATED);
    }

    @Operation(summary = "View all sellers")
    @ApiResponse(responseCode = "200", description = "List of sellers retrieved")
    @GetMapping("/viewAllSellers")
    public ResponseEntity<List<UserDTO>> viewAllSellers() {
        List<UserDTO> sellers = userService.viewAllSellers();
        return new ResponseEntity<>(sellers, HttpStatus.OK);
    }

//    @GetMapping("/viewSeller/{sellerId}")
//    public ResponseEntity<UserDTO> viewSeller(@PathVariable("sellerId") String sellerUsername) {
//        UserDTO seller = userService.viewSeller(sellerUsername);
//        return new ResponseEntity<>(seller, HttpStatus.OK);
//    }
    @Operation(summary = "View a seller by username")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seller found"),
            @ApiResponse(responseCode = "404", description = "Seller not found", content = @Content)
    })
    @GetMapping("/viewSeller/{username}")
    public ResponseEntity<UserDTO> viewSeller(
            @PathVariable("username") String username) {

        return ResponseEntity.ok(userService.viewSeller(username));
    }

    @Operation(summary = "Update a seller's account status", description = "Sets the account status (e.g. ACTIVE, SUSPENDED, BANNED) for a seller")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account status updated"),
            @ApiResponse(responseCode = "404", description = "Seller not found", content = @Content)
    })
    @PutMapping("/updateAccountStatus/{sellerId}")
    public ResponseEntity<UserDTO> updateAccountStatus(@PathVariable("sellerId") String sellerUsername,
                                                       @RequestParam("accountStatus") AccountStatus accountStatus) {
        UserDTO updatedSeller = userService.updateAccountStatus(sellerUsername, accountStatus);
        return new ResponseEntity<>(updatedSeller, HttpStatus.OK);
    }

    @Operation(summary = "Update a seller's profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seller updated"),
            @ApiResponse(responseCode = "400", description = "Invalid fields", content = @Content),
            @ApiResponse(responseCode = "404", description = "Seller not found", content = @Content)
    })
    @PutMapping("/updateSeller/{sellerId}")
    public ResponseEntity<UserDTO> updateSeller(@PathVariable("sellerId") String sellerUsername,
                                                @Valid @RequestBody UserDTO userDTO) {
        UserDTO updatedSeller = userService.updateSeller(sellerUsername, userDTO);
        return new ResponseEntity<>(updatedSeller, HttpStatus.OK);
    }

    @Operation(summary = "Delete a seller account")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Seller deleted"),
            @ApiResponse(responseCode = "404", description = "Seller not found", content = @Content)
    })
    @DeleteMapping("/deleteSeller/{sellerId}")
    public ResponseEntity<Void> deleteSeller(@PathVariable("sellerId") String sellerUsername) {
        userService.deleteSeller(sellerUsername);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Admin accounts

    @Operation(summary = "Register a new admin account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Admin registered"),
            @ApiResponse(responseCode = "400", description = "Invalid or missing fields", content = @Content),
            @ApiResponse(responseCode = "409", description = "Username or phone number already exists", content = @Content)
    })
    @PostMapping("/registerAdmin")
    public ResponseEntity<UserDTO> registerAdmin(@Valid @RequestBody UserDTO userDTO) {
        UserDTO registeredAdmin = userService.registerAdmin(userDTO);
        return new ResponseEntity<>(registeredAdmin, HttpStatus.CREATED);
    }

    @Operation(summary = "View an admin by username")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin found"),
            @ApiResponse(responseCode = "404", description = "Admin not found", content = @Content)
    })
    @GetMapping("/viewAdmin/{adminId}")
    public ResponseEntity<UserDTO> viewAdmin(@PathVariable("adminId") String adminUsername) {
        UserDTO admin = userService.viewAdmin(adminUsername);
        return new ResponseEntity<>(admin, HttpStatus.OK);
    }

    @Operation(summary = "Update an admin's profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin updated"),
            @ApiResponse(responseCode = "400", description = "Invalid fields", content = @Content),
            @ApiResponse(responseCode = "404", description = "Admin not found", content = @Content)
    })
    @PutMapping("/updateAdmin/{adminId}")
    public ResponseEntity<UserDTO> updateAdmin(@PathVariable("adminId") String adminUsername,
                                               @Valid @RequestBody UserDTO userDTO) {
        UserDTO updatedAdmin = userService.updateAdmin(adminUsername, userDTO);
        return new ResponseEntity<>(updatedAdmin, HttpStatus.OK);
    }

    @Operation(summary = "Delete an admin account")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Admin deleted"),
            @ApiResponse(responseCode = "404", description = "Admin not found", content = @Content)
    })
    @DeleteMapping("/deleteAdmin/{adminId}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable("adminId") String adminUsername) {
        userService.deleteAdmin(adminUsername);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Check if a username exists", description = "Returns true if the username is already registered in any role")
    @ApiResponse(responseCode = "200", description = "Existence check result",
            content = @Content(schema = @Schema(implementation = Boolean.class)))
    @GetMapping("/exists/{username}")
    public boolean checkUser(@PathVariable String username) {
        boolean userex = userService.checkUser(username);
        return userex;
    }

    @GetMapping("/byId/{userId}")
    @Operation(summary = "Resolve a username from a numeric user ID (internal service lookup)")
    public ResponseEntity<String> getUsernameById(@PathVariable Integer userId) {
        return ResponseEntity.ok(userService.getUsernameById(userId));
    }

//    @Operation(summary = "Raise a dispute", description = "Files a dispute (e.g. against a seller or transaction) for admin resolution")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Dispute raised",
//                    content = @Content(schema = @Schema(implementation = DisputeDTO.class))),
//            @ApiResponse(responseCode = "400", description = "Invalid dispute payload", content = @Content)
//    })
//    @PostMapping("/dispute")
//    public ResponseEntity<DisputeDTO> raiseDispute(@RequestBody DisputeDTO disputeDTO){
//        DisputeDTO disputeRes = userService.raiseDispute(disputeDTO);
//        return new ResponseEntity<>(disputeRes, HttpStatus.OK);
//    }
}