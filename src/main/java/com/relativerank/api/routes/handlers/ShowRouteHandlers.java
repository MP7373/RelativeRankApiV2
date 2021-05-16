package com.relativerank.api.routes.handlers;

import com.relativerank.api.domain.Show;
import com.relativerank.api.dto.MalShowDetails;
import com.relativerank.api.dto.ProblemDetails;
import com.relativerank.api.dto.ShowRequest;
import com.relativerank.api.repositories.ShowRepository;
import com.relativerank.api.util.Constants;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public record ShowRouteHandlers(ShowRepository showRepository,
                                WebClient webClient,
                                String malUserListUrl) {

    @NonNull
    public Mono<ServerResponse> getAllShows(ServerRequest serverRequest) {
        return ServerResponse.ok()
                .body(BodyInserters.fromPublisher(showRepository.findAll(), Show.class));
    }

    @NonNull
    public Mono<ServerResponse> getShow(ServerRequest serverRequest) {
        var showId = serverRequest.pathVariable("id");

        return showRepository.findById(showId)
                .flatMap(show -> ServerResponse.ok()
                        .body(BodyInserters.fromPublisher(showRepository.findById(showId), Show.class)))
                .switchIfEmpty(Constants.SHOW_NOT_FOUND_RESPONSE_CREATOR.apply(showId));
    }

    @NonNull
    public Mono<ServerResponse> createShow(ServerRequest serverRequest) {
        var showFromBody = serverRequest.body(BodyExtractors.toMono(ShowRequest.class))
                .map(show -> new Show(null, show.name()))
                .cache();

        return createShow(showFromBody);
    }

    @NonNull
    private Mono<ServerResponse> createShow(Mono<Show> showMono) {
        return showMono
                .flatMap(show -> showRepository.findByName(show.name()))
                .flatMap(existingShow -> ServerResponse.status(HttpStatus.CONFLICT)
                        .body(Mono.just("A show already exists with the name " + existingShow.name()), String.class))
                .switchIfEmpty(showMono
                        .flatMap(showRepository::save)
                        .flatMap(savedShow -> ServerResponse.created(URI.create("/show/" + savedShow.id()))
                                .body(BodyInserters.fromValue(savedShow))));
    }

    @NonNull
    public Mono<ServerResponse> upsertShow(ServerRequest serverRequest) {
        var showId = serverRequest.pathVariable("id");
        var showFromBody = serverRequest.body(BodyExtractors.toMono(ShowRequest.class))
                .map(show -> new Show(showId, show.name()))
                .cache();

        return showRepository.findById(showId)
                .flatMap(existingShow -> showFromBody.flatMap(showRepository::save))
                .flatMap(savedShow -> ServerResponse.ok().body(BodyInserters.fromValue(savedShow)))
                .switchIfEmpty(createShow(showFromBody));
    }

    @NonNull
    public Mono<ServerResponse> deleteShow(ServerRequest serverRequest) {
        var showId = serverRequest.pathVariable("id");

        return showRepository.findById(showId)
                .flatMap(existingShow -> showRepository.delete(existingShow).thenReturn(existingShow))
                .flatMap(deletedShow ->  ServerResponse.ok()
                        .body(Mono.just("Show with id: " + deletedShow.id() + " and name: " + deletedShow.name() + " was deleted"), String.class))
                .switchIfEmpty(Constants.SHOW_NOT_FOUND_RESPONSE_CREATOR.apply(showId));
    }

    @NonNull
    public Mono<ServerResponse> importFromMal(ServerRequest serverRequest) {
        var malUsername = serverRequest.queryParam("username").orElse(null);
        if (malUsername == null) {
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(BodyInserters.fromValue(new ProblemDetails("bad request",
                            "400",
                            "Query parameter username is required")));
        }

        return recursivelyGetAllShowsOfUsersMalList(malUsername, 0, new ArrayList<>())
                .flatMap(malUserShowListResponse -> ServerResponse.ok()
                        .body(Mono.just(malUserShowListResponse), new ParameterizedTypeReference<>() {}));
    }

    private Mono<List<MalShowDetails>> recursivelyGetAllShowsOfUsersMalList(String malUsername, int showOffset, List<MalShowDetails> malList) {
        var urlWithParams = String.format(malUserListUrl, malUsername, showOffset);
        return webClient.get()
                .uri(urlWithParams)
                .exchangeToMono(clientResponse -> clientResponse.bodyToMono(new ParameterizedTypeReference<List<MalShowDetails>>() {}))
                .flatMap(newMalListPage -> {
                    if (newMalListPage.size() > 0) {
                        malList.addAll(newMalListPage.stream()
                                .filter(show -> show.status() == 2 || show.score() != 0)
                                .collect(Collectors.toList()));

                        return recursivelyGetAllShowsOfUsersMalList(malUsername, showOffset + 300, malList);
                    }

                    return Mono.just(malList);
                });
    }
}
