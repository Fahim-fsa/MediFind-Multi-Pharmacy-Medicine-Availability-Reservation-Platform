package com.medifind.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SLP: Platform Security & Compliance → "Role-based access control (RBAC)
 * framework", "Password hashing for all user types"
 *

 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Boot auto-configures a {@link DaoAuthenticationProvider} from
     * whichever {@code UserDetailsService} and {@code PasswordEncoder}
     * beans are on the classpath (ours are {@link com.medifind.security.CustomUserDetailsService}
     * and the bean above). Exposing the resulting {@link AuthenticationManager}
     * as its own bean is what lets AuthService call
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Public: landing page, every auth screen, static assets, health check.
                        .requestMatchers("/", "/auth/**", "/css/**", "/js/**", "/images/**",
                                "/webjars/**", "/favicon.ico").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Role-gated areas — both the page routes and their JSON API counterparts.
                        .requestMatchers("/patient/**", "/api/patient/**").hasRole("PATIENT")
                        .requestMatchers("/pharmacist/**", "/api/pharmacist/**").hasRole("PHARMACIST")
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // We manage the session ourselves in AuthService (see establishSecurityContext),
                // so we only need Spring Security to clear it correctly on logout.
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/?loggedout=true")
                        .permitAll()
                )
                // No formLogin(): an unauthenticated visit to a protected page is redirected
                // straight to the right login screen instead of Spring Security's default
                // "403 Forbidden" response, which would be a dead end for a browser user.
                .exceptionHandling(ex -> ex
                                .authenticationEntryPoint((request, response, authException) -> {
                                    String target = request.getRequestURI().startsWith("/admin")
                                            ? "/auth/admin-login"
                                            : "/auth/login";
                                    response.sendRedirect(request.getContextPath() + target);
                                })
                        // A logged-in user hitting a page their role doesn't allow (e.g. a
                        // patient opening /admin/dashboard) — Boot's default error view
                        // resolver renders templates/error/403.html for this automatically.
                );

        return http.build();
    }
}
