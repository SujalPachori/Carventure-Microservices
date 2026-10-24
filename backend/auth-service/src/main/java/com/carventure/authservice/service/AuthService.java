package com.carventure.authservice.service;

import com.carventure.authservice.dto.AuthResponseDTO;
import com.carventure.authservice.dto.LoginRequestDTO;
import com.carventure.authservice.dto.RefreshTokenRequestDTO;
import com.carventure.authservice.dto.RegisterRequestDTO;

public interface AuthService {

    AuthResponseDTO register(RegisterRequestDTO request);

    AuthResponseDTO login(LoginRequestDTO request);

    AuthResponseDTO refreshToken(RefreshTokenRequestDTO request);
}
