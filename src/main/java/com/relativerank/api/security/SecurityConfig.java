package com.relativerank.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final ReactiveAuthenticationManager authenticationManager;

    private final ServerSecurityContextRepository serverSecurityContextRepository;

    public SecurityConfig(ReactiveAuthenticationManager authenticationManager,
                          ServerSecurityContextRepository serverSecurityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.serverSecurityContextRepository = serverSecurityContextRepository;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity serverHttpSecurity,
            CorsConfigurationSource corsConfigurationSource) {
        return serverHttpSecurity.cors().disable()
                .csrf().disable()
                .cors().configurationSource(corsConfigurationSource)
                .and()
                .authenticationManager(authenticationManager)
                .securityContextRepository(serverSecurityContextRepository)
                .authorizeExchange()
                .pathMatchers(HttpMethod.POST, "/login").permitAll()
                .pathMatchers(HttpMethod.POST, "/users").permitAll()
                .pathMatchers(HttpMethod.PATCH, "/users/{username}").hasRole("ADMIN")
                .pathMatchers(HttpMethod.DELETE, "/users/{username}").hasRole("ADMIN")
                .pathMatchers(HttpMethod.GET, "/shows").permitAll()
                .pathMatchers(HttpMethod.GET, "/shows/{id}").permitAll()
                .pathMatchers(HttpMethod.POST, "/shows").hasRole("ADMIN")
                .pathMatchers(HttpMethod.PUT, "/shows/{id}").hasRole("ADMIN")
                .pathMatchers(HttpMethod.DELETE, "/shows/{id}").hasRole("ADMIN")
                .pathMatchers(HttpMethod.GET, "/import-from-mal").permitAll()
                .pathMatchers(HttpMethod.GET, "/show-lists/{username}").permitAll()
                .pathMatchers(HttpMethod.PUT, "/show-lists/{username}").access(AuthHandlers.jwtUsernameMatchesPathUserName)
                .pathMatchers(HttpMethod.GET, "/global-ranked-show-list/{page}").permitAll()
                .and()
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${site-url}") String siteUrl) {
        var corsConfig = new CorsConfiguration();
        corsConfig.applyPermitDefaultValues();
        corsConfig.addAllowedMethod(HttpMethod.GET);
        corsConfig.addAllowedMethod(HttpMethod.POST);
        corsConfig.addAllowedMethod(HttpMethod.PUT);
        corsConfig.addAllowedMethod(HttpMethod.PATCH);
        corsConfig.addAllowedMethod(HttpMethod.DELETE);
        corsConfig.setAllowedOrigins(List.of(siteUrl));

        var  source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return source;
    }
}
