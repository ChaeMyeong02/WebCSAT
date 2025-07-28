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
import org.springframework.http.HttpMethod;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;



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


        http.exceptionHandling(exception -> exception
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                })
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        );

        http.authorizeHttpRequests(auth -> auth
                        // 로그인 및 토큰 발급 허용
                        .requestMatchers("/login", "/api/token", "/logout").permitAll()
                        // 정적 자원 허용
                        .requestMatchers("/img/**", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/api/user/*/role","api/user").hasRole("ADMIN")
                        // GET 메서드인 경우: 인증된 사용자 모두 허용
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        // 나머지 메서드의 /api/** 요청은 ADMIN만
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        // 나머지는 모두 허용
                        .anyRequest().permitAll());

        http.oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .authorizationEndpoint(authorizationEndpoint ->
                                authorizationEndpoint.authorizationRequestRepository(oAuth2AuthorizationRequestBasedOnCookieRepository()))
                        .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint.userService(oAuth2UserCustomService))
                        .failureHandler(customOAuth2FailureHandler)
                        .successHandler(oAuth2SuccessHandler()));
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