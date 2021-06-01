package com.relativerank.api.routes.handlers;

import com.relativerank.api.db.RankedShow;
import com.relativerank.api.db.ShowList;
import com.relativerank.api.dto.ProblemDetails;
import com.relativerank.api.repositories.ShowListRepository;
import com.relativerank.api.repositories.UserRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public record ShowListRouteHandlers(ShowListRepository showListRepository, UserRepository userRepository) {

    @NonNull
    public Mono<ServerResponse> getShowList(ServerRequest serverRequest) {
        var username = serverRequest.pathVariable("username");

        return showListRepository.findByUsername(username)
                .flatMap(showList -> ServerResponse.ok().body(BodyInserters.fromValue(showList)))
                .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body(BodyInserters.fromValue(new ProblemDetails(
                                "not found",
                                "404",
                                "show list does not exist for provided username"))));
    }

    @NonNull
    public Mono<ServerResponse> upsertShowList(ServerRequest serverRequest) {
        var username = serverRequest.pathVariable("username");
        var updatedShowList = serverRequest.body(BodyExtractors.toMono(
                new ParameterizedTypeReference<List<RankedShow>>() {}));

        return updatedShowList.flatMap(showList -> Mono.zip(userRepository.findByUsername(username), Mono.just(showList)))
                .flatMap(tuple -> showListRepository.save(new ShowList(
                        tuple.getT1().id(), tuple.getT1().username(), tuple.getT2())))
                .flatMap(savedShowList -> ServerResponse.ok().body(BodyInserters.fromValue(savedShowList)))
                .onErrorResume(IllegalArgumentException.class, error -> ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body(BodyInserters.fromValue(new ProblemDetails(
                                "bad request",
                                "400",
                                error.getMessage()))))
                .switchIfEmpty(ServerResponse.badRequest()
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body(BodyInserters.fromValue(new ProblemDetails(
                                "bad request",
                                "400",
                                "user with username " + username + " does not exist"))));
    }
}
