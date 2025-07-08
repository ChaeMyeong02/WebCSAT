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
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/img/**", "/css/**", "/js/**").permitAll()
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