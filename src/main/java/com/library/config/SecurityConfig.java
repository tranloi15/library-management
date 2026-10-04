package com.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/home", "/catalog", "/login", "/register",
                                 "/forgot-password", "/forgot-password/**",
                                 "/reset-password", "/reset-password/**",
                                 "/documents/*/qr", "/documents/*/qr/**",
                                 "/error", "/css/**", "/js/**", "/images/**", "/h2-console/**").permitAll()
                .requestMatchers("/borrow/history", "/borrow/*/renew", "/borrow/qr-request", "/borrow/qr-request/**",
                                 "/borrow/cart", "/borrow/cart/**",
                                 "/borrow/*/cancel-request", "/notifications", "/notifications/**", "/profile", "/profile/**").hasAnyRole("READER", "ADMIN", "LIBRARIAN")
                .requestMatchers("/borrow", "/borrow/**", "/documents", "/documents/**",
                                 "/reports/**", "/dashboard", "/dashboard/**").hasAnyRole("ADMIN", "LIBRARIAN")
                .requestMatchers("/users/**", "/roles/**", "/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/?denied")
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}