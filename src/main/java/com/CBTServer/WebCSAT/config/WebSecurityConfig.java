package com.CBTServer.WebCSAT.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    @Bean
    public WebSecurityCustomizer configure() {
        return (web) -> web.ignoring()
                .requestMatchers("/static/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf-> csrf.disable());
        http.formLogin(formLogin-> formLogin
                .loginPage("/login")
                .defaultSuccessUrl("/index")
        );
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/signup", "/js/**", "/css/**", "/images/**", "/api/**").permitAll()
                .anyRequest().authenticated()
        );
        http.logout(logout -> logout
                .logoutSuccessUrl("/login"));
        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder byCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
