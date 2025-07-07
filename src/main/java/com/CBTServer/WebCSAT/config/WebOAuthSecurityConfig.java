package com.CBTServer.WebCSAT.config;

import com.CBTServer.WebCSAT.config.jwt.TokenProvider;
import com.CBTServer.WebCSAT.config.oauth.CustomOAuth2FailureHandler;
import com.CBTServer.WebCSAT.config.oauth.OAuth2AuthorizationRequestBasedOnCookieRepository;
import com.CBTServer.WebCSAT.config.oauth.OAuth2SuccessHandler;
import com.CBTServer.WebCSAT.config.oauth.OAuth2UserCustomService;
import com.CBTServer.WebCSAT.repository.RefreshTokenRopository;
import com.CBTServer.WebCSAT.service.UserDetailService;
import com.CBTServer.WebCSAT.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.util.pattern.PathPatternParser;

@RequiredArgsConstructor
@Configuration
public class WebOAuthSecurityConfig {

    private final OAuth2UserCustomService oAuth2UserCustomService;
    private final CustomOAuth2FailureHandler customOAuth2FailureHandler;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRopository refreshTokenRopository;
    private final UserService userService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.httpBasic(httpBasic -> httpBasic.disable());
        http.formLogin(formLogin -> formLogin.disable());

        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login","/api/token").permitAll()
                        .requestMatchers("/api/**").authenticated() // 글수정, 삭제기능 등
                        .requestMatchers("/img/**", "/css/**", "/js/**").permitAll()
                        .anyRequest().permitAll());

        http.oauth2Login(oauth2 -> oauth2  // OAuth2 기반 로그인을 설정하는 메서드 => 예: Google, Naver, Kakao 같은 OAuth 제공자 로그인 설정
                        .loginPage("/login") // 1. 커스텀 로그인 페이지 설정 => oauthLogin.html 수행되도록 함
                        // 사용자가 로그인 버튼을 누르면 인가 요청을 생성하는 구간
                        // 인가요청정보(client-id/secret-key 등)를 쿠키에 저장/관리하도록 커스텀 리포지토리를 설정한 것
                        .authorizationEndpoint(authorizationEndpoint ->
                                authorizationEndpoint.authorizationRequestRepository(oAuth2AuthorizationRequestBasedOnCookieRepository()))
                        // 로그인 성공 후 제공자(Google 등)로부터 사용자 정보를 받아오는 구간
                        // 받아온 사용자 정보를 가공/추가 처리하는 서비스를 지정한 것
                        // oAuth2UserCustomService에서 OAuth2UserService를 구현해서 loadUser()에서 사용자 정보를 DB에 저장하거나 추가 정보를 가져올 수 있음
                        .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint.userService(oAuth2UserCustomService))  // 사용자 정보 가공
                        .failureHandler(customOAuth2FailureHandler)
                        //OAuth 로그인에 성공한 후 추가로 처리할 작업을 정의한 핸들러
                        // JWT 토큰 생성, 세션 설정, 리다이렉션 경로 지정 등
                        .successHandler(oAuth2SuccessHandler())); // 로그인 성공 시 추가 처리
        http.logout(logout -> logout
                        .logoutSuccessUrl("/"));
        return http.build();
    }
    @Bean
    public OAuth2SuccessHandler oAuth2SuccessHandler() {
        return new OAuth2SuccessHandler(tokenProvider, refreshTokenRopository,
                oAuth2AuthorizationRequestBasedOnCookieRepository(),
                userService
        );
    }


    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenProvider);
    }


    @Bean
    public OAuth2AuthorizationRequestBasedOnCookieRepository oAuth2AuthorizationRequestBasedOnCookieRepository() {
        return new OAuth2AuthorizationRequestBasedOnCookieRepository();
    }


    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, BCryptPasswordEncoder bCryptPasswordEncoder, UserDetailService userDetailService) throws Exception {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailService);
        authProvider.setPasswordEncoder(bCryptPasswordEncoder);
        return new ProviderManager(authProvider);
    }
}