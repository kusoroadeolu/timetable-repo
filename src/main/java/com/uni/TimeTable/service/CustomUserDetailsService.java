package com.uni.TimeTable.service;

import com.uni.TimeTable.models.User;
import com.uni.TimeTable.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true, true, true, // accountNonExpired, credentialsNonExpired, accountNonLocked
                user.getAuthorities().stream()
                        .map(auth -> {
                            String authority = auth.getAuthority();
                            // Ensure roles are prefixed with "ROLE_"
                            if (!authority.startsWith("ROLE_")) {
                                authority = "ROLE_" + authority;
                            }
                            return new SimpleGrantedAuthority(authority);
                        })
                        .collect(Collectors.toList())
        );
    }
}