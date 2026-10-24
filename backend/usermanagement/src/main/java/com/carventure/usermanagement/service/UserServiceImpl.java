package com.carventure.usermanagement.service;

import com.carventure.usermanagement.client.AuthServiceClient;
import com.carventure.usermanagement.dto.UserDTO;
import com.carventure.usermanagement.exception.UserNotFoundException;
import com.carventure.usermanagement.model.AccountStatus;
import com.carventure.usermanagement.model.User;
import com.carventure.usermanagement.repository.UserManagementRepository;
import com.carventure.usermanagement.userkafka.event.UserRegisteredEvent;
import com.carventure.usermanagement.userkafka.kafka.UserEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    //private final DisputeProducer service;
    private final UserManagementRepository userManagementRepository;
    private final AuthServiceClient authServiceClient;
    private final UserEventProducer userEventProducer;

    @Override
    public User toUserEntity(UserDTO userDTO) {
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setAddress(userDTO.getAddress());
        user.setPincode(userDTO.getPincode());
        user.setDateOfBirth(userDTO.getDateOfBirth());
        return user;
    }

    @Override
    public UserDTO toUserDTOEntity(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getUserId());
        userDTO.setUsername(user.getUsername());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setEmail(user.getEmail());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setAddress(user.getAddress());
        userDTO.setPincode(user.getPincode());
        userDTO.setDateOfBirth(user.getDateOfBirth());
        userDTO.setAccountStatus(user.getAccountStatus());
        return userDTO;
    }

    // Customer accounts

    @Override
    public UserDTO registerCustomer(UserDTO userDTO) {
        User user = toUserEntity(userDTO);
        user.setAccountStatus(AccountStatus.APPROVED);
        userEventProducer.publishUserRegistered(
                new UserRegisteredEvent(
                        user.getUsername(),
                        Set.of("Customer"),
                        user.getCreatedAt()
                ));
        User savedUser = userManagementRepository.save(user);
        return toUserDTOEntity(savedUser);
    }

    @Override
    public List<UserDTO> viewAllCustomers() {
        List<String> customerUsernames = authServiceClient.getUsernamesByRole("CUSTOMER");
        System.out.println("hello");
        List<User> customers = userManagementRepository.findAllByUsernameIn(customerUsernames);

        List<UserDTO> customerDTOs = new ArrayList<>();
        for (User customer : customers) {
            customerDTOs.add(toUserDTOEntity(customer));
        }
        return customerDTOs;
    }

    @Override
    public UserDTO viewCustomer(String customerUsername) {
        User user = userManagementRepository.findByUsername(customerUsername)
                .orElseThrow(() -> new UserNotFoundException("No customer found with username: " + customerUsername));
        return toUserDTOEntity(user);
    }

    @Override
    public UserDTO updateCustomer(String customerUsername, UserDTO userDTO) {
        User existingUser = userManagementRepository.findByUsername(customerUsername)
                .orElseThrow(() -> new UserNotFoundException("No customer found with username: " + customerUsername));

        existingUser.setFirstName(userDTO.getFirstName());
        existingUser.setLastName(userDTO.getLastName());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setPhoneNumber(userDTO.getPhoneNumber());
        existingUser.setAddress(userDTO.getAddress());
        existingUser.setPincode(userDTO.getPincode());
        existingUser.setDateOfBirth(userDTO.getDateOfBirth());

        User updatedUser = userManagementRepository.save(existingUser);
        return toUserDTOEntity(updatedUser);
    }

    @Override
    @Transactional
    public boolean deleteCustomer(String customerUsername) {
        if (!userManagementRepository.existsByUsername(customerUsername)) {
            throw new UserNotFoundException("No customer found with username: " + customerUsername);
        }
        userManagementRepository.deleteByUsername(customerUsername);
        return true;
    }

    // Seller / Dealer accounts

    @Override
    public UserDTO registerSeller(UserDTO userDTO) {
        User user = toUserEntity(userDTO);
        user.setAccountStatus(AccountStatus.PENDING);
        User savedUser = userManagementRepository.save(user);
        return toUserDTOEntity(savedUser);
    }

    @Override
    public UserDTO registerDealer(UserDTO userDTO) {
        User user = toUserEntity(userDTO);
        user.setAccountStatus(AccountStatus.PENDING);
        User savedUser = userManagementRepository.save(user);
        return toUserDTOEntity(savedUser);
    }

    @Override
    public List<UserDTO> viewAllSellers() {
        List<String> sellerUsernames = authServiceClient.getUsernamesByRole("SELLER");
        List<User> sellers = userManagementRepository.findAllByUsernameIn(sellerUsernames);

        List<UserDTO> sellerDTOs = new ArrayList<>();
        for (User seller : sellers) {
            sellerDTOs.add(toUserDTOEntity(seller));
        }
        return sellerDTOs;
    }

    @Override
    public UserDTO viewSeller(String sellerUsername) {
        User user = userManagementRepository.findByUsername(sellerUsername)
                .orElseThrow(() -> new UserNotFoundException("No seller found with username: " + sellerUsername));
        return toUserDTOEntity(user);
    }

    @Override
    public UserDTO updateAccountStatus(String sellerUsername, AccountStatus accountStatus) {
        User existingUser = userManagementRepository.findByUsername(sellerUsername)
                .orElseThrow(() -> new UserNotFoundException("No seller found with username: " + sellerUsername));

        existingUser.setAccountStatus(accountStatus);

        User updatedUser = userManagementRepository.save(existingUser);
        return toUserDTOEntity(updatedUser);
    }

    @Override
    public UserDTO updateSeller(String sellerUsername, UserDTO userDTO) {
        User existingUser = userManagementRepository.findByUsername(sellerUsername)
                .orElseThrow(() -> new UserNotFoundException("No seller found with username: " + sellerUsername));

        existingUser.setFirstName(userDTO.getFirstName());
        existingUser.setLastName(userDTO.getLastName());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setPhoneNumber(userDTO.getPhoneNumber());
        existingUser.setAddress(userDTO.getAddress());
        existingUser.setPincode(userDTO.getPincode());
        existingUser.setDateOfBirth(userDTO.getDateOfBirth());

        User updatedUser = userManagementRepository.save(existingUser);
        return toUserDTOEntity(updatedUser);
    }

    @Override
    @Transactional
    public boolean deleteSeller(String sellerUsername) {
        if (!userManagementRepository.existsByUsername(sellerUsername)) {
            throw new UserNotFoundException("No seller found with username: " + sellerUsername);
        }
        userManagementRepository.deleteByUsername(sellerUsername);
        return true;
    }

    // Admin accounts

    @Override
    public UserDTO registerAdmin(UserDTO userDTO) {
        User user = toUserEntity(userDTO);
        user.setAccountStatus(AccountStatus.APPROVED);
        User savedUser = userManagementRepository.save(user);
        return toUserDTOEntity(savedUser);
    }

    @Override
    public UserDTO viewAdmin(String adminUsername) {
        User user = userManagementRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new UserNotFoundException("No admin found with username: " + adminUsername));
        return toUserDTOEntity(user);
    }

    @Override
    public UserDTO updateAdmin(String adminUsername, UserDTO userDTO) {
        User existingUser = userManagementRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new UserNotFoundException("No admin found with username: " + adminUsername));

        existingUser.setFirstName(userDTO.getFirstName());
        existingUser.setLastName(userDTO.getLastName());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setPhoneNumber(userDTO.getPhoneNumber());
        existingUser.setAddress(userDTO.getAddress());
        existingUser.setPincode(userDTO.getPincode());
        existingUser.setDateOfBirth(userDTO.getDateOfBirth());

        User updatedUser = userManagementRepository.save(existingUser);
        return toUserDTOEntity(updatedUser);
    }

    @Override
    @Transactional
    public boolean deleteAdmin(String adminUsername) {
        if (!userManagementRepository.existsByUsername(adminUsername)) {
            throw new UserNotFoundException("No admin found with username: " + adminUsername);
        }
        userManagementRepository.deleteByUsername(adminUsername);
        return true;
    }

    public boolean checkUser(String username) {
        return userManagementRepository.existsByUsername(username);
    }

    @Override
    public String getUsernameById(Integer userId) {
        return userManagementRepository.findById(userId)
                .map(User::getUsername)
                .orElseThrow(() -> new UserNotFoundException("User " + userId + " not found"));
    }
}