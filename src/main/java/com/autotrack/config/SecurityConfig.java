package com.autotrack.config;

import com.autotrack.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

/**
 * Configuration class for Spring Security
 * Sets up OAuth2 GitHub login and security rules
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);
        
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(mvcMatcherBuilder.pattern("/")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/login")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/error")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/css/**")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/js/**")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/images/**")).permitAll()
                        .requestMatchers(mvcMatcherBuilder.pattern("/webjars/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/webhook/github")).permitAll() // Using AntPathRequestMatcher for non-MVC patterns
                        .requestMatchers(new AntPathRequestMatcher("/webhook/commit")).permitAll() // Commit webhook from VS Code
                        .requestMatchers(new AntPathRequestMatcher("/h2-console/**"),
                            new AntPathRequestMatcher("/api/**")).permitAll() // H2 Console access
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                            new AntPathRequestMatcher("/webhook/github"),
                            new AntPathRequestMatcher("/webhook/commit"),
                            new AntPathRequestMatcher("/h2-console/**"),
                            new AntPathRequestMatcher("/api/**")
                        )
                )
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable())) // Required for H2 console
                .build();
    }
}
