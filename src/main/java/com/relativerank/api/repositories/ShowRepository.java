package com.relativerank.api.repositories;

import com.relativerank.api.domain.Show;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

@Profile("!test")
public interface ShowRepository extends ReactiveMongoRepository<Show, String> {

    Mono<Show> findByName(String name);
}
