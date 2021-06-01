package com.relativerank.api;

import com.relativerank.api.db.RankedShow;
import com.relativerank.api.db.ShowList;
import com.relativerank.api.db.User;
import com.relativerank.api.dto.ProblemDetails;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

public class ShowListEndpointTests extends EndpointTestsBase {

    @Test
    void getShowListEndpoint_WhenShowListExistsForUsername_Returns200_OkStatus_WithResponseBodyContainingShowList() {
        var username = "Honoka";
        var rankedShow = new RankedShow("Love Live", 1, 0.5);
        var showList = new ShowList("id", username, Collections.singletonList(rankedShow));

        Mockito.when(showListRepository.findByUsername(username)).thenReturn(Mono.just(showList));

        webTestClient.get()
                .uri("/show-lists/" + username)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ShowList.class)
                .value(body -> Assertions.assertEquals(showList, body));
    }

    @Test
    void getShowListEndpoint_WhenShowListDoesNotExistForUsername_Returns404_NotFoundStatus_WithResponseBodyContainingProblemDetails() {
        var username = "Honoka";

        Mockito.when(showListRepository.findByUsername(username)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/show-lists/" + username)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ProblemDetails.class)
                .value(body -> {
                    Assertions.assertEquals("not found", body.title());
                    Assertions.assertEquals("404", body.status());
                    Assertions.assertEquals("show list does not exist for provided username", body.detail());
                });
    }

    @Test
    void upsertShowList_WhenShowListExistsForUsername_AndShowListIsValid_Returns200_OkStatus_WithResponseBodyContainingUpdatedShowList() {
        var username = "Honoka";
        var preUpdateRankedShow = new RankedShow("Love Live", 1, 0.5);
        var preUpdateRankedShows = Collections.singletonList(preUpdateRankedShow);
        var preUpdateShowList = new ShowList("id", username, preUpdateRankedShows);

        var existingUser = new User("id", username, null, null);

        var newListRankedShow1 = new RankedShow("Love Live", 1, 0.666);
        var newListRankedShow2 = new RankedShow("Idoly Pride", 2, 0.333);
        var newRankedShowList = List.of(newListRankedShow1, newListRankedShow2);
        var newShowList = new ShowList(preUpdateShowList.id(), username, newRankedShowList);

        Mockito.when(userRepository.findByUsername(ArgumentMatchers.any())).thenReturn(Mono.just(existingUser));
        Mockito.when(showListRepository.save(new ShowList(
                preUpdateShowList.id(),
                preUpdateShowList.username(),
                newRankedShowList))).thenReturn(Mono.just(newShowList));

        var userJwt = jwtEncoder.encodeUserJwt(username);
        webTestClient.put()
                .uri("/show-lists/" + username)
                .header("Authorization", "Bearer " + userJwt)
                .body(Mono.just(newRankedShowList), new ParameterizedTypeReference<List<RankedShow>>() {})
                .exchange()
                .expectStatus().isOk()
                .expectBody(ShowList.class)
                .value(body -> {
                    Assertions.assertEquals(preUpdateShowList.id(), body.id());
                    Assertions.assertEquals(username, body.username());
                    Assertions.assertEquals(newRankedShowList, body.showList());
                });
    }

    @Test
    void upsertShowList_WhenShowListExistsForUsername_AndShowListIsInValid_Returns400_BadRequestStatus_WithResponseBodyContainingProblemDetails() {
        var username = "Honoka";
        var preUpdateRankedShow = new RankedShow("Love Live", 1, 0.5);
        var preUpdateRankedShows = Collections.singletonList(preUpdateRankedShow);
        var preUpdateShowList = new ShowList("id", username, preUpdateRankedShows);

        var existingUser = new User("id", username, null, null);

        var newListRankedShow1 = new RankedShow("Love Live", 1, 0.7);
        var newListRankedShow2 = new RankedShow("Idoly Pride", 2, 0.2);
        var invalidList = List.of(newListRankedShow1, newListRankedShow2);

        Mockito.when(userRepository.findByUsername(ArgumentMatchers.any())).thenReturn(Mono.just(existingUser));

        var userJwt = jwtEncoder.encodeUserJwt(username);
        webTestClient.put()
                .uri("/show-lists/" + username)
                .header("Authorization", "Bearer " + userJwt)
                .body(Mono.just(invalidList), new ParameterizedTypeReference<List<RankedShow>>() {})
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetails.class)
                .value(body -> {
                    Assertions.assertEquals("bad request", body.title());
                    Assertions.assertEquals("400", body.status());
                });
    }
}
