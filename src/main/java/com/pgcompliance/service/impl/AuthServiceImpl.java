package com.pgcompliance.service.impl;

import com.pgcompliance.dto.LoginRequestDto;
import com.pgcompliance.dto.LoginResponseDto;
import com.pgcompliance.dto.RegisterRequestDto;
import com.pgcompliance.entity.User;
import com.pgcompliance.exception.ResourceNotFoundException;
import com.pgcompliance.repository.UserRepository;
import com.pgcompliance.security.JwtTokenProvider;
import com.pgcompliance.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl
        implements AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public String register(
            RegisterRequestDto request) {

        if (userRepository.existsByUsername(
                request.getUsername())) {

            throw new RuntimeException(
                    "Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()))
                .role(request.getRole())
                .active(true)
                .build();

        userRepository.save(user);

        return "User registered successfully";
    }

    @Override
    public LoginResponseDto login(
            LoginRequestDto request) {

        User user = userRepository.findByUsername(
                request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new RuntimeException(
                    "Invalid credentials");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {

            throw new IllegalStateException(
                    "Account is inactive");
        }

        String token = jwtTokenProvider.generateToken(user);

        return LoginResponseDto.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}