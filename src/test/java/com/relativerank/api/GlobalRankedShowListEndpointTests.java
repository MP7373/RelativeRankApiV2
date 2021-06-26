package com.relativerank.api;

import com.relativerank.api.db.GlobalRankedShowList;
import com.relativerank.api.db.RankedShow;
import com.relativerank.api.dto.ProblemDetails;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.List;

public class GlobalRankedShowListEndpointTests extends EndpointTestsBase {

    @Test
    void getGlobalRankedShowList_WhenPageExists_Returns200_OkStatus_WithResponseBodyContainingGlobalRankedShowList() {
        var page = "1";
        var globalRankedShowList = new GlobalRankedShowList(
                page,
                1,
                List.of(new RankedShow("Yuru Camp", 1, 0.66),
                        new RankedShow("Love Live", 2,0.5)));

        Mockito.when(globalRankedShowListRepository.findById(page)).thenReturn(Mono.just(globalRankedShowList));

        webTestClient.get()
                .uri("/global-ranked-show-list/" + page)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GlobalRankedShowList.class)
                .value(response -> Assertions.assertEquals(globalRankedShowList, response));
    }

    @Test
    void getGlobalRankedShowList_WhenPageDoesNotExist_Returns404_NotFoundStatus_WithResponseBodyContainingProblemDetails() {
        var page = "1";

        Mockito.when(globalRankedShowListRepository.findById(page)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/global-ranked-show-list/" + page)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ProblemDetails.class)
                .value(response -> {
                    Assertions.assertEquals("not found", response.title());
                    Assertions.assertEquals("404", response.status());
                    Assertions.assertEquals("provided page does not exist", response.detail());
                });
    }
}
