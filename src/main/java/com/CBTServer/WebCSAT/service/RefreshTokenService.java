package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.domain.RefreshToken;
import com.CBTServer.WebCSAT.repository.RefreshTokenRopository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RefreshTokenService {
    private final RefreshTokenRopository refreshTokenRopository;

    public RefreshToken findByRefreshToken(String refreshToken) {
        RefreshToken retoken = refreshTokenRopository.findByRefreshToken(refreshToken)
                .orElseThrow(()->new IllegalArgumentException("없는 refreshToken"));

        return retoken;
    }
}
