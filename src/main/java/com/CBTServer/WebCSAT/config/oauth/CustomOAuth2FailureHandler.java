package com.CBTServer.WebCSAT.config.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class CustomOAuth2FailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        // Default Error Message
        String errorMessage = "로그인 중 오류가 발생했습니다.";

        if (exception instanceof OAuth2AuthenticationException authEx) {
            String errorCode = authEx.getError().getErrorCode();
            if ("provider_mismatch".equals(errorCode)) {
                errorMessage = authEx.getError().getDescription();
            }
        }

        String redirectUrl = UriComponentsBuilder.fromPath("/login")
                .queryParam("error", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
