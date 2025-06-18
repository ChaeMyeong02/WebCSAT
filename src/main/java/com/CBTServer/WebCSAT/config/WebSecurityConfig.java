package com.CBTServer.WebCSAT.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
//    @Bean
//    public WebSecurityCustomizer configure() {
//        return (web) -> web.ignoring()
//                .requestMatchers("/");
//    }

//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http.csrf(csrf-> csrf.disable());
//        http.authorizeHttpRequests(auth -> auth
//                .requestMatchers("/login", "/signup", "/user").permitAll()
//        );
//        return http.build();
//    }
}
