package com.example.wallet.security;

import com.example.wallet.entity.User;
import com.example.wallet.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().name());
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), user.isEnabled(), true, true, true, List.of(authority));
    }
}