package com.relativerank.api.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class JwtEncoder {

    private final Algorithm algorithm;

    private final String adminUsername;

    public JwtEncoder(@Value("${relativerank-jwt-secret}") String secret,
                      @Value("${relativerank-admin-username}") String adminUsername)
            throws UnsupportedEncodingException {
        algorithm = Algorithm.HMAC256(secret);
        this.adminUsername = adminUsername;
    }

    public String encodeUserJwt(String username) {
        var jwtBuilder = JWT.create();
        List<String> roles = new ArrayList<>();
        roles.add("USER");
        if (username.equals(adminUsername)) {
            roles.add("ADMIN");
        }

        return jwtBuilder.withIssuer("relativerank.com")
                .withExpiresAt(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                .withSubject(username)
                .withArrayClaim("ROLES", roles.toArray(new String[] {}))

                .sign(algorithm);
    }

    public DecodedJWT decodeJwt(String jwt) {
        var verifier = JWT.require(algorithm)
                .withIssuer("relativerank.com")
                .build();

        return verifier.verify(jwt);
    }
}
