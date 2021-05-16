package com.relativerank.api.security;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
public record JwtAuthenticationManager(JwtEncoder jwtEncoder) implements ReactiveAuthenticationManager {

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        try {
            if (authentication instanceof JwtAuthentication jwtAuthentication) {
                var decodedJwt = jwtEncoder.decodeJwt(jwtAuthentication.getJwt());

                var username = decodedJwt.getSubject();
                jwtAuthentication.setPrincipal(username);

                var authorities = decodedJwt.getClaims()
                        .get("ROLES")
                        .asList(String.class)
                        .stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
                jwtAuthentication.setAuthorities(authorities);

                jwtAuthentication.setAuthenticated(true);

                return Mono.just(jwtAuthentication);
            }
        } catch (Exception ignored) {}

        return Mono.empty();
    }
}
