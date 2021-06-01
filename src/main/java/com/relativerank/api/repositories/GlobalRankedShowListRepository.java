package com.relativerank.api.repositories;

import com.relativerank.api.db.GlobalRankedShowList;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

@Profile("!test")
public interface GlobalRankedShowListRepository extends ReactiveMongoRepository<GlobalRankedShowList, String> {}
