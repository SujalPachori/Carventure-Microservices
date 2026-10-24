package com.carventure.usermanagement.service;

import com.carventure.usermanagement.dto.UserDTO;
import com.carventure.usermanagement.model.AccountStatus;
import com.carventure.usermanagement.model.User;

import java.util.List;

public interface UserService {

    public User toUserEntity(UserDTO userDTO);
    public UserDTO toUserDTOEntity(User user);

    // Customer accounts
    public UserDTO registerCustomer(UserDTO userDTO);
    public List<UserDTO> viewAllCustomers();
    public UserDTO viewCustomer(String customerUsername);
    public UserDTO updateCustomer(String customerUsername, UserDTO userDTO);
    public boolean deleteCustomer(String customerUsername);

    // Seller / Dealer accounts
    public UserDTO registerSeller(UserDTO userDTO);
    public UserDTO registerDealer(UserDTO userDTO);
    public List<UserDTO> viewAllSellers();
    public UserDTO viewSeller(String sellerUsername);
    public UserDTO updateAccountStatus(String sellerUsername, AccountStatus accountStatus);
    public UserDTO updateSeller(String sellerUsername, UserDTO userDTO);
    public boolean deleteSeller(String sellerUsername);

    // Admin accounts
    public UserDTO registerAdmin(UserDTO userDTO);
    public UserDTO viewAdmin(String adminUsername);
    public UserDTO updateAdmin(String adminUsername, UserDTO userDTO);
    public boolean deleteAdmin(String adminUsername);
    public boolean checkUser(String username);

    String getUsernameById(Integer userId);

}
