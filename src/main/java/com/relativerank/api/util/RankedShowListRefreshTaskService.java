package com.relativerank.api.util;

import com.relativerank.api.db.GlobalRankedShowList;
import com.relativerank.api.db.RankedShow;
import com.relativerank.api.dto.ShowScoreCount;
import com.relativerank.api.repositories.GlobalRankedShowListRepository;
import com.relativerank.api.repositories.ShowListRepository;
import com.relativerank.api.repositories.ShowRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@Profile("!test")
@EnableScheduling
public record RankedShowListRefreshTaskService(GlobalRankedShowListRepository globalRankedShowListRepository,
                                               ShowListRepository showListRepository,
                                               ShowRepository showRepository) {

    private static final ConcurrentHashMap<String, Boolean> showCache = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 3600000)
    public void refreshRankedShowList() {
        var start = Instant.now();
        showListRepository.findAll()
                // filter to only include shows that exist in show collection
                .flatMap(showList -> {
                    var showChecks = new ArrayList<Mono<RankedShow>>();
                    for (var show : showList.showList()) {
                        if (!showCache.containsKey(show.name())) {
                            showChecks.add(showRepository.findByName(show.name())
                                    .map(dbShow -> show));
                        } else {
                            showChecks.add(Mono.just(show));
                        }
                    }

                    return Flux.fromIterable(showChecks)
                            .flatMap(showMono -> showMono)
                            .map(show -> {
                                showCache.put(show.name(), true);
                                return show;
                            })
                            .collectList();
                })
                // re normalize filtered list for correct percentile rankings
                .map(filteredExistingInDbShows -> {
                    var rank = new AtomicInteger(0);
                    return filteredExistingInDbShows.stream()
                            .map(show -> {
                                var showRank = rank.incrementAndGet();
                                var percentileRank = 1 - (1.0 / (1 + filteredExistingInDbShows.size()) * showRank);
                                return new RankedShow(show.name(), showRank, percentileRank);
                            })
                            .collect(Collectors.toList());
                })
                // sum up scores for each show
                .reduce(new HashMap<String, ShowScoreCount>(), (map, showList) -> {
                   showList.forEach(rankedShow -> {
                       var count = map.getOrDefault(rankedShow.name(), new ShowScoreCount(0, 0));
                       count = new ShowScoreCount(count.scoreSum() + rankedShow.percentileRank(), count.numberOfTimesEncountered() + 1);
                       map.put(rankedShow.name(), count);
                   });

                   return map;
                })
                // average out rating for each show and sort final list by rating then save
                .doOnSuccess(map -> {
                    record NameScore(String name, double score) {}
                    var nameScoreList = new ArrayList<NameScore>();
                    map.forEach((key, value) -> {
                        nameScoreList.add(new NameScore(key, value.scoreSum() / value.numberOfTimesEncountered()));
                    });

                    nameScoreList.sort(Comparator.comparingDouble(NameScore::score).reversed());

                    var rank = new AtomicInteger(0);
                    var rankedShowList = nameScoreList.stream()
                            .map(nameScore -> new RankedShow(nameScore.name, rank.incrementAndGet(), nameScore.score()))
                            .collect(Collectors.toList());

                    var rankedShowListPage = new ArrayList<RankedShow>();
                    var page = 1;
                    var numberOfPages = rankedShowList.size() / 100 + 1;
                    for (var i = 0; i < rankedShowList.size(); i++) {
                        rankedShowListPage.add(rankedShowList.get(i));
                        if ((i + 1) % 100 == 0 || i == rankedShowList.size() - 1) {
                            var rankedShowListPageToSave = rankedShowListPage;
                            var pageToSave = page++;
                            rankedShowListPage = new ArrayList<>();
                            globalRankedShowListRepository.save(
                                    new GlobalRankedShowList(Integer.toString(pageToSave),
                                    numberOfPages,
                                    rankedShowListPageToSave))
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .subscribe();
                        }
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }
}
