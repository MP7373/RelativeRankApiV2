package com.relativerank.api;

import com.relativerank.api.db.RankedShow;
import com.relativerank.api.db.Show;
import com.relativerank.api.db.ShowList;
import com.relativerank.api.repositories.GlobalRankedShowListRepository;
import com.relativerank.api.repositories.ShowListRepository;
import com.relativerank.api.repositories.ShowRepository;
import com.relativerank.api.util.RankedShowListRefreshTaskService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

public class RankedShowListRefreshTaskServiceTests {

    @Test
    void refreshRankedShowList_WhenFindAllReturnsValidList_CallsGlobalRankedShowListRepositorySave() throws InterruptedException {
        var globalRankedShowListRepository = Mockito.mock(GlobalRankedShowListRepository.class);
        var showListRepository = Mockito.mock(ShowListRepository.class);
        var showRepository = Mockito.mock(ShowRepository.class);

        var showListRefreshService = new RankedShowListRefreshTaskService(
                globalRankedShowListRepository,
                showListRepository,
                showRepository);

        var evaRanked = new RankedShow("Eva", 1, 0.5);
        var userShowList = new ShowList("id", "Shinji", Collections.singletonList(evaRanked));
        Mockito.when(showListRepository.findAll()).thenReturn(Flux.just(userShowList));

        var eva = new Show("id", "Eva");
        Mockito.when(showRepository.findByName(evaRanked.name())).thenReturn(Mono.just(eva));

        showListRefreshService.refreshRankedShowList();

        Thread.sleep(2000);

        Mockito.verify(globalRankedShowListRepository).save(ArgumentMatchers.any());
    }
}
