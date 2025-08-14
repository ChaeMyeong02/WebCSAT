// src/main/java/com/CBTServer/WebCSAT/service/CurrentUserService.java
package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.User;
import com.CBTServer.WebCSAT.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public Optional<Long> userId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();

        Object principal = auth.getPrincipal();

        // 1) JWT 방식
        if (principal instanceof Jwt jwt) {
            Object claim = jwt.getClaim("userId");
            if (claim == null) claim = jwt.getClaim("id");
            if (claim == null) claim = jwt.getSubject(); // sub
            try {
                if (claim != null) return Optional.of(Long.valueOf(claim.toString()));
            } catch (NumberFormatException ignored) {}
            // sub가 email이라면 email로 조회
            if (claim != null && claim.toString().contains("@")) {
                return userRepository.findByEmail(claim.toString()).map(User::getUserId);
            }
        }

        // 2) 세션(UserDetails) 방식
        if (principal instanceof UserDetails ud) {
            return userRepository.findByEmail(ud.getUsername()).map(User::getUserId);
        }

        // 3) 세션(plain String = username/email)
        if (principal instanceof String s) {
            return userRepository.findByEmail(s).map(User::getUserId);
        }

        return Optional.empty();
    }

    public Long requiredUserId() {
        return userId().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
