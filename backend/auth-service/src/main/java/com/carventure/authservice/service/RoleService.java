package com.carventure.authservice.service;

import com.carventure.authservice.dto.UserRolesResponseDTO;
import com.carventure.authservice.entity.Role;

import java.util.List;

public interface RoleService {

    UserRolesResponseDTO addRole(String username, Role role);

    UserRolesResponseDTO getRoles(String username);

    List<String> getUsersByRole(Role role);
}
