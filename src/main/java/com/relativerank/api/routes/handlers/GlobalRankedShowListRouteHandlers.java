package com.relativerank.api.routes.handlers;

import com.relativerank.api.dto.ProblemDetails;
import com.relativerank.api.repositories.GlobalRankedShowListRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public record GlobalRankedShowListRouteHandlers(GlobalRankedShowListRepository globalRankedShowListRepository) {

    @NonNull
    public Mono<ServerResponse> getGlobalRankedShowList(ServerRequest serverRequest) {
        var page = serverRequest.pathVariable("page");

        return globalRankedShowListRepository
                .findById(page)
                .flatMap(globalRankedShowList -> ServerResponse.ok()
                        .body(BodyInserters.fromValue(globalRankedShowList)))
                .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body(BodyInserters.fromValue(new ProblemDetails(
                                "not found",
                                "404",
                                "provided page does not exist"))));
    }
}
