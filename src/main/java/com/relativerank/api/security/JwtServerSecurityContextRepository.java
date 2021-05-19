package com.relativerank.api.security;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public record JwtServerSecurityContextRepository(ReactiveAuthenticationManager authenticationManager,
                                                 JwtEncoder jwtEncoder) implements ServerSecurityContextRepository {

    @Override
    public Mono<Void> save(ServerWebExchange serverWebExchange, SecurityContext securityContext) {
        return null;
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange serverWebExchange) {
        var authHeaders = serverWebExchange.getRequest().getHeaders().get("Authorization");
        if (authHeaders != null && authHeaders.size() > 0) {
            var jwt = authHeaders.get(0).substring(7);
            var decodedJwt = jwtEncoder.decodeJwt(jwt);

            return authenticationManager.authenticate(new JwtAuthentication(decodedJwt))
                    .map(SecurityContextImpl::new);
        }

        return Mono.just(new SecurityContextImpl(new JwtAuthentication(null)));
    }
}
