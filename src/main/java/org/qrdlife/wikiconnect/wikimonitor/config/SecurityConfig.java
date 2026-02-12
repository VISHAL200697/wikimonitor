package org.qrdlife.wikiconnect.wikimonitor.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/js/**",
                                                                "/css/**",
                                                                "/images/**",
                                                                "/login",
                                                                "/oauth2/callback",
                                                                "/auth/wikimedia",
                                                                "/access-denied")
                                                .permitAll()
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/").hasRole("USER")
                                                .anyRequest().hasRole("USER"))
                                .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))

                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/", true)
                                                .permitAll())

                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())

                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/api/**"));

                return http.build();
        }
}
