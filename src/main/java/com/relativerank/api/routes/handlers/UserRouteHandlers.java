package com.relativerank.api.routes.handlers;

import com.relativerank.api.db.User;
import com.relativerank.api.dto.ProblemDetails;
import com.relativerank.api.dto.UserAuthenticationResponse;
import com.relativerank.api.dto.UsernamePassword;
import com.relativerank.api.repositories.UserRepository;
import com.relativerank.api.security.HmacSha512PasswordEncoder;
import com.relativerank.api.security.JwtEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public record UserRouteHandlers(UserRepository userRepository,
                                JwtEncoder jwtEncoder,
                                HmacSha512PasswordEncoder passwordEncoder) {

    @NonNull
    public Mono<ServerResponse> login(ServerRequest serverRequest) {
        var usernamePasswordMono = serverRequest
                .body(BodyExtractors.toMono(UsernamePassword.class));

        return usernamePasswordMono
                .flatMap(usernamePassword -> Mono.zip(Mono.just(usernamePassword),
                        userRepository.findByUsername(usernamePassword.username())))
                .flatMap(bodyUserDbUserTuple -> {
                    var bodyUser = bodyUserDbUserTuple.getT1();
                    var dbUser = bodyUserDbUserTuple.getT2();
                    var hashedPassword = new byte[] {};
                    try {
                        hashedPassword = passwordEncoder
                                .hmacSha512HashPassword(bodyUser.password(), dbUser.passwordSalt());
                    } catch (Exception ignored) {}

                    var passwordsMatch = Arrays.equals(hashedPassword, dbUser.hashedPassword());
                    if (passwordsMatch) {
                        var jwt = jwtEncoder.encodeUserJwt(dbUser.username());
                        return ServerResponse.ok()
                                .body(BodyInserters.fromValue(new UserAuthenticationResponse(dbUser.username(), jwt)));
                    }

                    return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                            .body(BodyInserters.fromValue(
                                    new ProblemDetails(
                                            "unauthorized", "401", "invalid username or password")));
                });
    }

    @NonNull
    public Mono<ServerResponse> createUser(ServerRequest serverRequest) {
        var usernamePasswordMono = serverRequest
                .body(BodyExtractors.toMono(UsernamePassword.class));

        return usernamePasswordMono.flatMap(usernamePassword -> {
            if (!Pattern.matches("[a-zA-Z0-9\\-_]{1,50}", usernamePassword.username())) {
                return ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body(BodyInserters.fromValue(new ProblemDetails("bad request",
                                "400",
                                "username must only include a-z, A-Z, 0-9, _, or - and be 1 50 characters long")));
            }

            var salt = passwordEncoder.generateRandomSalt();

            byte[] hashedPassword;
            try {
                hashedPassword = passwordEncoder.hmacSha512HashPassword(usernamePassword.password(), salt);
            } catch (Exception e) {
                return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body(BodyInserters.fromValue(
                                new ProblemDetails("internal server error",
                                        "500",
                                        "failed to create user")));
            }

            var userId = UUID.randomUUID().toString();
            return userRepository.save(new User(userId, usernamePassword.username(), hashedPassword, salt))
                    .flatMap(savedUser -> {
                        var jwt = jwtEncoder.encodeUserJwt(savedUser.username());
                        return ServerResponse.status(HttpStatus.CREATED)
                                .body(BodyInserters.fromValue(
                                        new UserAuthenticationResponse(savedUser.username(), jwt)));
                    });
        });
    }

    @NonNull
    public Mono<ServerResponse> updateUser(ServerRequest serverRequest) {
        var usernamePasswordMono = serverRequest
                .body(BodyExtractors.toMono(UsernamePassword.class));

        return usernamePasswordMono.flatMap(usernamePassword -> userRepository
                    .findByUsername(usernamePassword.username())
                    .flatMap(existingUser -> {
                        var salt = passwordEncoder.generateRandomSalt();

                        byte[] hashedPassword;
                        try {
                            hashedPassword = passwordEncoder.hmacSha512HashPassword(usernamePassword.password(), salt);
                        } catch (Exception e) {
                            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                                    .body(BodyInserters.fromValue(
                                            new ProblemDetails("internal server error",
                                                    "500",
                                                    "failed to create user")));
                        }

                        return userRepository
                                .save(new User(existingUser.id(), usernamePassword.username(), hashedPassword, salt))
                                .flatMap(savedUser -> {
                                    var jwt = jwtEncoder.encodeUserJwt(savedUser.username());
                                    return ServerResponse.ok()
                                            .body(BodyInserters.fromValue(
                                                    new UserAuthenticationResponse(savedUser.username(), jwt)));
                                });
                    }));
    }

    @NonNull
    public Mono<ServerResponse> deleteUser(ServerRequest serverRequest) {
        var username = serverRequest.pathVariable("username");

        return userRepository.findByUsername(username)
                .flatMap(userRepository::delete)
                .then(ServerResponse.ok().body(BodyInserters.fromValue("User with username: " + username)));
    }
}
