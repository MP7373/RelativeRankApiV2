package com.relativerank.api;

import com.relativerank.api.db.User;
import com.relativerank.api.dto.DeleteUserRequest;
import com.relativerank.api.dto.ProblemDetails;
import com.relativerank.api.dto.UserAuthenticationResponse;
import com.relativerank.api.dto.UsernamePassword;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class UserEndpointTests extends EndpointTestsBase {

    @Test
    void loginEndpoint_WhenValidLogin_Returns200_OkStatus_WithResponseBodyContainingUsernameAndJwt()
            throws NoSuchAlgorithmException, InvalidKeyException {
        var loginRequest = new UsernamePassword("username", "password");
        var salt = passwordEncoder.generateRandomSalt();
        var hashedPassword = passwordEncoder.hmacSha512HashPassword(loginRequest.password(), salt);
        var user = new User("id", loginRequest.username(), hashedPassword, salt);
        var loginSuccessJwt = jwtEncoder.encodeUserJwt(user.username());

        Mockito.when(userRepository.findByUsername(loginRequest.username())).thenReturn(Mono.just(user));

        webTestClient.post()
                .uri("/login")
                .body(BodyInserters.fromValue(loginRequest))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserAuthenticationResponse.class)
                .value(response -> {
                    Assertions.assertEquals("username", response.username());

                    var decodedResponseJwt = jwtEncoder.decodeJwt(response.jwt());
                    var expectedJwt = jwtEncoder.decodeJwt(loginSuccessJwt);
                    Assertions.assertEquals(expectedJwt.getIssuer(), decodedResponseJwt.getIssuer());
                    Assertions.assertEquals(expectedJwt.getSubject(), decodedResponseJwt.getSubject());
                });
    }

    @Test
    void loginEndpoint_WhenInvalidLogin_Returns401_UnauthorizedStatus_WithResponseBodyContainingInvalidUsernameOrPassword()
            throws NoSuchAlgorithmException, InvalidKeyException {
        var loginRequest = new UsernamePassword("username", "password");
        var existingUsernamePassword = new UsernamePassword("username", "secret");
        var salt = passwordEncoder.generateRandomSalt();
        var hashedPassword = passwordEncoder.hmacSha512HashPassword(existingUsernamePassword.password(), salt);
        var existingUser = new User("id", loginRequest.username(), hashedPassword, salt);

        Mockito.when(userRepository.findByUsername(loginRequest.username())).thenReturn(Mono.just(existingUser));

        webTestClient.post()
                .uri("/login")
                .body(BodyInserters.fromValue(loginRequest))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ProblemDetails.class)
                .value(response -> {
                    Assertions.assertEquals("unauthorized", response.title());
                    Assertions.assertEquals("401", response.status());
                    Assertions.assertEquals("invalid username or password", response.detail());
                });
    }

    @Test
    void createUserEndpoint_WhenUserSuccessfullyCreated_Returns201_CreatedStatus_WithResponseBodyContainingUsernameAndJwt()
            throws NoSuchAlgorithmException, InvalidKeyException {
        var createUserRequest = new UsernamePassword("username", "password");
        var salt = passwordEncoder.generateRandomSalt();
        var hashedPassword = passwordEncoder.hmacSha512HashPassword(createUserRequest.password(), salt);
        var newUser = new User("id", createUserRequest.username(), hashedPassword, salt);
        var newUserJwt = jwtEncoder.encodeUserJwt(newUser.username());

        ArgumentMatcher<User> usersMatchArgMatcher = user -> user.username().equals(newUser.username())
                && Arrays.equals(user.hashedPassword(), newUser.hashedPassword())
                && Arrays.equals(user.passwordSalt(), newUser.passwordSalt());

        Mockito.when(passwordEncoder.generateRandomSalt()).thenReturn(salt);
        Mockito.when(userRepository.save(ArgumentMatchers.argThat(usersMatchArgMatcher)))
                .thenReturn(Mono.just(newUser));

        webTestClient.post()
                .uri("/users")
                .body(BodyInserters.fromValue(createUserRequest))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UserAuthenticationResponse.class)
                .value(response -> {
                    Assertions.assertEquals("username", response.username());

                    var decodedResponseJwt = jwtEncoder.decodeJwt(response.jwt());
                    var decodedNewUserJwt = jwtEncoder.decodeJwt(newUserJwt);
                    Assertions.assertEquals(decodedNewUserJwt.getIssuer(), decodedResponseJwt.getIssuer());
                    Assertions.assertEquals(decodedNewUserJwt.getSubject(), decodedResponseJwt.getSubject());
                });

        // clean up so other calls use real method
        Mockito.when(passwordEncoder.generateRandomSalt()).thenCallRealMethod();
    }

    @Test
    void createUserEndpoint_WhenExceptionIsThrown_Returns201_CreatedStatus_WithResponseBodyContainingFailedToCreateUser()
            throws NoSuchAlgorithmException, InvalidKeyException {
        var createUserRequest = new UsernamePassword("username", "password");

        Mockito.doThrow(RuntimeException.class)
                .when(passwordEncoder)
                .hmacSha512HashPassword(ArgumentMatchers.anyString(), ArgumentMatchers.any(byte[].class));

        webTestClient.post()
                .uri("/users")
                .body(BodyInserters.fromValue(createUserRequest))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ProblemDetails.class)
                .value(response -> {
                    Assertions.assertEquals("internal server error", response.title());
                    Assertions.assertEquals("500", response.status());
                    Assertions.assertEquals("failed to create user", response.detail());
                });

        // clean up so other calls use real method
        Mockito.doCallRealMethod()
                .when(passwordEncoder)
                .hmacSha512HashPassword(ArgumentMatchers.anyString(), ArgumentMatchers.any(byte[].class));
    }

    @Test
    void createUserEndpoint_WhenUsernameIsInvalid_Returns201_CreatedStatus_WithResponseBodyContainingFailedToCreateUser()
            throws NoSuchAlgorithmException, InvalidKeyException {
        var createUserRequest = new UsernamePassword(";'invalid", "password");

        webTestClient.post()
                .uri("/users")
                .body(BodyInserters.fromValue(createUserRequest))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetails.class)
                .value(response -> {
                    Assertions.assertEquals("bad request", response.title());
                    Assertions.assertEquals("400", response.status());
                    Assertions.assertEquals("username must only include a-z, A-Z, 0-9, _, or - and be 1 50 characters long", response.detail());
                });

        // clean up so other calls use real method
        Mockito.doCallRealMethod()
                .when(passwordEncoder)
                .hmacSha512HashPassword(ArgumentMatchers.anyString(), ArgumentMatchers.any(byte[].class));
    }

    @Test
    void updateUserEndpoint_WhenUserIsAdmin_Returns200_OkStatus_WithResponseBodyContainingUsernameAndJwt() {
        var updateUserRequest = new UsernamePassword("username", "password");
        var existingUser = new User("id", updateUserRequest.username(), new byte[] {}, new byte[] {});

        Mockito.when(userRepository.findByUsername(updateUserRequest.username()))
                .thenReturn(Mono.just(existingUser));
        Mockito.when(userRepository.save(ArgumentMatchers.any())).thenReturn(Mono.just(existingUser));

        var adminJwt = jwtEncoder.encodeUserJwt(adminUsername);
        webTestClient.patch()
                .uri("/users/" + updateUserRequest.username())
                .body(BodyInserters.fromValue(updateUserRequest))
                .header("Authorization", "Bearer " + adminJwt)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserAuthenticationResponse.class)
                .value(response -> {
                    Assertions.assertEquals(updateUserRequest.username(), response.username());
                    Assertions.assertNotNull(response.jwt());
                });
    }

    @Test
    void updateUserEndpoint_WhenUserIsAdmin_WhenExceptionIsThrown_Returns201_CreatedStatus_WithResponseBodyContainingFailedToCreateUser()
            throws NoSuchAlgorithmException, InvalidKeyException {
        var updateUserRequest = new UsernamePassword("username", "password");
        var existingUser = new User("id", updateUserRequest.username(), new byte[] {}, new byte[] {});

        Mockito.when(userRepository.findByUsername(updateUserRequest.username()))
                .thenReturn(Mono.just(existingUser));
        Mockito.doThrow(RuntimeException.class)
                .when(passwordEncoder)
                .hmacSha512HashPassword(ArgumentMatchers.anyString(), ArgumentMatchers.any(byte[].class));

        var adminJwt = jwtEncoder.encodeUserJwt(adminUsername);
        webTestClient.patch()
                .uri("/users/" + updateUserRequest.username())
                .body(BodyInserters.fromValue(updateUserRequest))
                .header("Authorization", "Bearer " + adminJwt)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody(ProblemDetails.class)
                .value(response -> {
                    Assertions.assertEquals("internal server error", response.title());
                    Assertions.assertEquals("500", response.status());
                    Assertions.assertEquals("failed to create user", response.detail());
                });

        // clean up so other calls use real method
        Mockito.doCallRealMethod()
                .when(passwordEncoder)
                .hmacSha512HashPassword(ArgumentMatchers.anyString(), ArgumentMatchers.any(byte[].class));
    }

    @Test
    void deleteUserEndpoint_WhenUserIsAdmin_Returns200_OkStatus_WithResponseBodySayingUserDeleted() {
        var deleteUserRequest = new DeleteUserRequest("username");
        var user = new User("id", deleteUserRequest.username(), new byte[] {}, new byte[] {});

        Mockito.when(userRepository.findByUsername(deleteUserRequest.username())).thenReturn(Mono.just(user));
        Mockito.when(userRepository.delete(ArgumentMatchers.any())).thenReturn(Mono.empty());

        var adminJwt = jwtEncoder.encodeUserJwt(adminUsername);
        webTestClient.delete()
                .uri("/users/username")
                .header("Authorization", "Bearer " + adminJwt)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("User with username: " + user.username());
    }
}
