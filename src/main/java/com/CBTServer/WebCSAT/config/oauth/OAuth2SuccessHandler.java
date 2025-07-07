package com.CBTServer.WebCSAT.config.oauth;

import com.CBTServer.WebCSAT.config.jwt.TokenProvider;
import com.CBTServer.WebCSAT.util.CookieUtil;
import com.CBTServer.WebCSAT.domain.RefreshToken;
import com.CBTServer.WebCSAT.domain.User;
import com.CBTServer.WebCSAT.repository.RefreshTokenRopository;
import com.CBTServer.WebCSAT.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;

@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    public static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(14);
    public static final Duration ACCESS_TOKEN_DURATION = Duration.ofDays(1);
    public static final String REDIRECT_PATH = "/";

    private final TokenProvider tokenProvider;
    private final RefreshTokenRopository refreshTokenRopository;
    private final OAuth2AuthorizationRequestBasedOnCookieRepository authorizationRequestRepository;
    private final UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        CustomOAuth2User customUser = (CustomOAuth2User) authentication.getPrincipal();
        User user = userService.findByEmail(customUser.getEmail());

        String refreshToken = tokenProvider.generateToken(user, REFRESH_TOKEN_DURATION);
        saveRefreshToken(user.getId(), refreshToken);
        addRefreshTokenToCookie(request, response, refreshToken);

        String accessToken = tokenProvider.generateToken(user, ACCESS_TOKEN_DURATION);
        String targetUrl = getTargetUrl(accessToken);

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }


    // 이미 해당 유저의 리프레시 토큰이 있다면 새로운 토큰으로 업데이트하고, 없다면 새로 생성하여 저장합니다.
    private void saveRefreshToken(Long userId, String newRefreshToken) {
        RefreshToken refreshToken = refreshTokenRopository.findByUserId(userId)
                .map(entity -> entity.update(newRefreshToken))
                .orElse(new RefreshToken(userId, newRefreshToken));

        refreshTokenRopository.save(refreshToken);
    }

    // 생성된 리프레시 토큰을 refresh_token이라는 이름의 HTTP-Only 쿠키에 담아 사용자 브라우저에 전달합니다.
    // 이렇게 쿠키에 저장하면, 자바스크립트로 접근할 수 없어 XSS 공격으로부터 안전하며,
    // 브라우저가 자동으로 요청에 포함시켜 보내주므로 편리합니다
    private void addRefreshTokenToCookie(HttpServletRequest request, HttpServletResponse response, String refreshToken) {
        int cookieMaxAge = (int) REFRESH_TOKEN_DURATION.toSeconds();

        CookieUtil.deleteCookie(request, response, REFRESH_TOKEN_COOKIE_NAME);
        CookieUtil.addCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, cookieMaxAge);
    }

    private void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

    // 클라이언트가 다음 페이지(/articles)로 이동할 때 토큰을 URL 파라미터로 전달
    // localhost:8088/articles?token=....
    private String getTargetUrl(String token) {
        return UriComponentsBuilder.fromUriString(REDIRECT_PATH)
                .queryParam("token", token)
                .build()
                .toUriString();
    }
}
