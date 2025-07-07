package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.config.jwt.TokenProvider;
import com.CBTServer.WebCSAT.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class TokenService {
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    public String createNewAccessToken(String refreshToken) {
        // 토큰 유효성 검사에 실패하면 예외 발생
        if(!tokenProvider.validToken(refreshToken)) {
            throw new IllegalArgumentException("refreshToken이 유효하지 않음");
        }

        Long useId = refreshTokenService.findByRefreshToken(refreshToken).getUserId();
        System.out.println("createNewAccessToken => "+useId);
        User user = userService.findById(useId);
        return tokenProvider.generateToken(user, Duration.ofHours(2));
    }
}
