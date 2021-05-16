package com.relativerank.api.routes.handlers;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ShowListRouteHandlers {

    @NonNull
    public Mono<ServerResponse> getShowList(ServerRequest serverRequest) {
        var username = serverRequest.pathVariable("username");
        return null;
    }
}
