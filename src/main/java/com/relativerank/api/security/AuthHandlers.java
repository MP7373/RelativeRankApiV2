package com.relativerank.api.security;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.web.server.authorization.AuthorizationContext;

import java.util.Objects;

public class AuthHandlers {

    public static final ReactiveAuthorizationManager<AuthorizationContext> jwtUsernameMatchesPathUserName =
            (authenticationMono, context) -> {
                try {
                    var exchange = context.getExchange();
                    var req = exchange.getRequest();
                    var path = req.getPath();
                    var element = path.elements().get(path.elements().size() - 1);
                    var pathUsername = element.value();

                    return authenticationMono.map(authentication -> {
                        if (authentication instanceof JwtAuthentication jwtAuthentication) {
                            var jwtUsername = (String) jwtAuthentication.getPrincipal();

                            if (Objects.equals(jwtUsername, pathUsername)) {
                                return new AuthorizationDecision(true);
                            }
                        }

                        return new AuthorizationDecision(false);
                    });
                } catch (Exception ignored) {}


                return authenticationMono.map(ignored -> new AuthorizationDecision(false));
            };
}
