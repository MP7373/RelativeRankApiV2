package com.relativerank.api;

import com.relativerank.api.repositories.ShowRepository;
import com.relativerank.api.repositories.UserRepository;
import com.relativerank.api.security.HmacSha512PasswordEncoder;
import com.relativerank.api.security.JwtEncoder;
import okhttp3.mockwebserver.MockWebServer;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {ApiApplication.class, ShowEndpointTests.TestConfig.class})
@ActiveProfiles("test")
public class EndpointTestsBase {

    static class TestConfig {

        @Bean
        String malUserListUrl() {
            return String.format("http://localhost:%s", mockWebServer.getPort());
        }

        @Bean
        HmacSha512PasswordEncoder passwordEncoder() {
            return Mockito.mock(HmacSha512PasswordEncoder.class, Mockito.CALLS_REAL_METHODS);
        }
    }

    static {
        var server = new MockWebServer();
        try {
            server.start();
        } catch (IOException e) {
            throw new RuntimeException();
        }
        mockWebServer = server;
    }

    protected static final MockWebServer mockWebServer;

    @Autowired
    protected WebTestClient webTestClient;

    @MockBean
    protected ShowRepository showRepository;

    @MockBean
    protected UserRepository userRepository;

    @Autowired
    protected JwtEncoder jwtEncoder;

    @Autowired
    protected HmacSha512PasswordEncoder passwordEncoder;

    @Value("${relativerank-admin-username}")
    protected String adminUsername;
}
