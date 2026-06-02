package com.example.wallet.service;

import com.example.wallet.dto.AuthDtos;
import com.example.wallet.entity.Role;
import com.example.wallet.entity.RoleName;
import com.example.wallet.entity.User;
import com.example.wallet.entity.Wallet;
import com.example.wallet.exception.BadRequestException;
import com.example.wallet.exception.DuplicateResourceException;
import com.example.wallet.repository.RoleRepository;
import com.example.wallet.repository.UserRepository;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.security.JwtService;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, WalletRepository walletRepository,
                       PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService, JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Email already registered");
        }

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new BadRequestException("USER role is not configured"));

        User user = new User(request.fullName(), request.email().toLowerCase(), passwordEncoder.encode(request.password()), userRole);
        user = userRepository.save(user);

        Wallet wallet = new Wallet(user);
        wallet.setBalance(BigDecimal.ZERO);
        walletRepository.save(wallet);
        user.setWallet(wallet);
        user = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return new AuthDtos.AuthResponse(
                jwtService.generateToken(userDetails),
                "Bearer",
                Instant.now().plusMillis(86_400_000),
                toUserResponse(user));
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        return new AuthDtos.AuthResponse(
                jwtService.generateToken(userDetails),
                "Bearer",
                Instant.now().plusMillis(86_400_000),
                toUserResponse(user));
    }

    private AuthDtos.UserResponse toUserResponse(User user) {
        return new AuthDtos.UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().getName().name(), user.isEnabled());
    }
}