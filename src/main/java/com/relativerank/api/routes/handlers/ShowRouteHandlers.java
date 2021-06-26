package com.relativerank.api.routes.handlers;

import com.relativerank.api.db.RankedShow;
import com.relativerank.api.db.Show;
import com.relativerank.api.dto.MalShowDetails;
import com.relativerank.api.dto.ProblemDetails;
import com.relativerank.api.dto.ShowRequest;
import com.relativerank.api.repositories.ShowRepository;
import com.relativerank.api.util.Constants;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public record ShowRouteHandlers(ShowRepository showRepository,
                                WebClient webClient,
                                ReactiveMongoTemplate reactiveMongoTemplate,
                                String malUserListUrl) {

    @NonNull
    public Mono<ServerResponse> getAllShows(ServerRequest serverRequest) {
        var showName = serverRequest.queryParam("show-name").orElse(null);
        if (showName != null) {
            return searchShow(showName);
        }

        return ServerResponse.ok()
                .body(BodyInserters.fromPublisher(showRepository.findAll(), Show.class));
    }

    @NonNull
    private Mono<ServerResponse> searchShow(String showName) {
        TextIndexDefinition textIndex = new TextIndexDefinition.TextIndexDefinitionBuilder()
                .onField("name")
                .build();
        TextCriteria criteria = TextCriteria.forDefaultLanguage().matching(showName);
        Query query = TextQuery.queryText(criteria).with(PageRequest.of(0, 10));
        Flux<Show> shows = reactiveMongoTemplate.find(query, Show.class);

        return ServerResponse.ok().body(BodyInserters.fromPublisher(shows, Show.class));
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
                .map(show -> new Show(null, show.name()));

        return showFromBody.flatMap(this::createShow);
    }

    @NonNull
    private Mono<ServerResponse> createShow(Show show) {
        return showRepository.save(show)
                .flatMap(savedShow -> ServerResponse.created(URI.create("/show/" + savedShow.id()))
                        .body(BodyInserters.fromValue(savedShow)))
                .onErrorResume(DuplicateKeyException.class, error -> ServerResponse.status(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body(BodyInserters.fromValue(new ProblemDetails(
                                "conflict",
                                "409",
                                "A show already exists with the name " + show.name()))));
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
                .switchIfEmpty(showFromBody.flatMap(this::createShow));
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

    private Mono<List<RankedShow>> recursivelyGetAllShowsOfUsersMalList(String malUsername, int showOffset, List<MalShowDetails> malList) {
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

                    malList.sort((a, b) -> b.score() - a.score());

                    var numberOfShows = malList.size();
                    var rankCounter = new AtomicInteger(0);
                    var rankedList = malList.stream()
                            .map(malShow -> {
                                var rank = rankCounter.incrementAndGet();
                                var percentileRank = 1.0 - (double) rank / (numberOfShows + 1);
                                return new RankedShow(malShow.anime_title(), rank, percentileRank);
                            })
                            .collect(Collectors.toList());
                    return Mono.just(rankedList);
                });
    }
}
