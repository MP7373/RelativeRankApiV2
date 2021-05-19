package com.relativerank.api.repositories;

import com.relativerank.api.db.ShowList;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

@Profile("!test")
public interface ShowListRepository extends ReactiveMongoRepository<ShowList, String> {

    Mono<ShowList> findByUsername(String username);
}
