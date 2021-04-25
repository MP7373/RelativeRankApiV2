package com.relativerank.api.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class Constants {

    private Constants() {}

    public static final Function<String, Mono<ServerResponse>> SHOW_NOT_FOUND_RESPONSE_CREATOR = showId ->
            ServerResponse.status(HttpStatus.NOT_FOUND).body(Mono.just("No show found for id: " + showId), String.class);
}
