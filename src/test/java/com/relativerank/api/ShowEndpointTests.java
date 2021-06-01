package com.relativerank.api;

import com.relativerank.api.db.Show;
import com.relativerank.api.dto.MalShowDetails;
import com.relativerank.api.dto.ProblemDetails;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

class ShowEndpointTests extends EndpointTestsBase {

	@Test
	void getAllShowsEndpoint_Returns200_OkStatus_WithResponseBodyContainingArrayOfAllShows() {
		var show1 = new Show(null, "Shingeki no Kyojin");
		var show2 = new Show(null, "Neon Genesis Evangelion");

		Mockito.when(showRepository.findAll()).thenReturn(Flux.just(show1, show2));

		webTestClient.get()
				.uri("/shows")
				.exchange()
				.expectStatus().isOk()
				.expectBody(new ParameterizedTypeReference<List<Show>>() {})
				.value(showsResponse -> {
					Assertions.assertEquals(2, showsResponse.size());
					Assertions.assertEquals(show1, showsResponse.get(0));
					Assertions.assertEquals(show2, showsResponse.get(1));
					Assertions.assertEquals(show2, showsResponse.get(1));
				});
	}

	@Test
	void getAllShowsEndpoint_WhenShowNameQueryParamIsIncluded_Returns200_OkStatus_WithResponseBodyContainingMatchingShows() {
		var show = new Show(null, "Shingeki no Kyojin");

		Mockito.when(reactiveMongoTemplate.find(ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(Flux.just(show));

		webTestClient.get()
				.uri("/shows?show-name=shingeki")
				.exchange()
				.expectStatus().isOk()
				.expectBody(new ParameterizedTypeReference<List<Show>>() {})
				.value(showsResponse -> {
					Assertions.assertEquals(1, showsResponse.size());
					Assertions.assertEquals(show, showsResponse.get(0));
				});
	}

	@Test
	void getShowEndpoint_WhenShowExistsForId_Returns200_OkStatus_WithResponseBodyContainingShow() {
		var showId = "id";
		var show = new Show(showId, "Shingeki no Kyojin");

		Mockito.when(showRepository.findById(showId)).thenReturn(Mono.just(show));

		webTestClient.get()
				.uri("/shows/" + showId)
				.exchange()
				.expectStatus().isOk()
				.expectBody(Show.class)
				.value(showResponse -> Assertions.assertEquals(show, showResponse));
	}

	@Test
	void getShowEndpoint_WhenShowDoesNotExistForId_Returns404_NotFoundStatus_WithResponseBodySayingShowNotFoundForId() {
		var showId = "id";

		Mockito.when(showRepository.findById(showId)).thenReturn(Mono.empty());

		webTestClient.get()
				.uri("/shows/" + showId)
				.exchange()
				.expectStatus().isNotFound()
				.expectBody(String.class)
				.value(showResponse -> Assertions.assertEquals("No show found for id: " + showId, showResponse));
	}

	@Test
	void createShowEndpoint_WhenUserIsAdmin_WhenShowWithSameNameDoesNotExist_Returns201_CreatedStatus_WithResponseBodyContainingShow() {
		var show = new Show(null, "Shingeki no Kyojin");
		var createdShow = new Show("id", show.name());

		Mockito.when(showRepository.findByName(show.name())).thenReturn(Mono.empty());
		Mockito.when(showRepository.save(show)).thenReturn(Mono.just(createdShow));

		var adminJwt = jwtEncoder.encodeUserJwt(adminUsername);
		webTestClient.post()
				.uri("/shows")
				.header("Authorization", "Bearer " + adminJwt)
				.body(Mono.just(show), Show.class)
				.exchange()
				.expectStatus().isCreated()
				.expectBody(Show.class)
				.value(showResponse -> Assertions.assertEquals(createdShow, showResponse));
	}

	@Test
	void createShowEndpoint_WhenUserIsAdmin_WhenShowWithSameNameAlreadyExists_Returns409_ConflictStatus_WithResponseBodySayingShowAlreadyExistsWithName() {
		var show = new Show(null, "Shingeki no Kyojin");
		var existingShow = new Show("id", show.name());

		Mockito.when(showRepository.save(ArgumentMatchers.any()))
				.thenReturn(Mono.error(new DuplicateKeyException("duplicate")));

		var adminJwt = jwtEncoder.encodeUserJwt(adminUsername);
		webTestClient.post()
				.uri("/shows")
				.header("Authorization", "Bearer " + adminJwt)
				.body(Mono.just(show), Show.class)
				.exchange()
				.expectStatus().value(status -> Assertions.assertEquals(HttpStatus.CONFLICT.value(), status))
				.expectBody(ProblemDetails.class)
				.value(body -> {
					Assertions.assertEquals("conflict", body.title());
					Assertions.assertEquals("409", body.status());
					Assertions.assertEquals("A show already exists with the name " + show.name(), body.detail());
				});
	}

	@Test
	void upsertShowEndpoint_WhenUserIsAdmin_WhenShowWithSameIdAlreadyExists_Returns200_OkStatus_WithResponseBodyContainingUpdatedShow() {
		var showId = "id";
		var updatedShow = new Show(showId, "Shingeki no Kyojin");
		var existingShow = new Show(showId, "Shingeki no Kyojiin Typo Name");

		Mockito.when(showRepository.findById(showId)).thenReturn(Mono.just(existingShow));
		Mockito.when(showRepository.save(updatedShow)).thenReturn(Mono.just(updatedShow));

		var adminJwt = jwtEncoder.encodeUserJwt(adminUsername);
		webTestClient.put()
				.uri("/shows/" + showId)
				.header("Authorization", "Bearer " + adminJwt)
				.body(Mono.just(updatedShow), Show.class)
				.exchange()
				.expectStatus().isOk()
				.expectBody(Show.class)
				.value(body -> Assertions.assertEquals(updatedShow, body));
	}

	@Test
	void upsertShowEndpoint_WhenUserIsAdmin_WhenShowWithSameIdDoesNotExist_AndShowWithSameNameDoesNotExist_Returns201_CreatedStatus_WithResponseBodyContainingCreatedShow() {
		var showId = "id";
		var show = new Show(showId, "Shingeki no Kyojin");

		Mockito.when(showRepository.findById(showId)).thenReturn(Mono.empty());
		Mockito.when(showRepository.findByName(show.name())).thenReturn(Mono.empty());
		Mockito.when(showRepository.save(show)).thenReturn(Mono.just(show));

		var adminJwt = jwtEncoder.encodeUserJwt(adminUsername);
		webTestClient.put()
				.uri("/shows/" + showId)
				.header("Authorization", "Bearer " + adminJwt)
				.body(Mono.just(show), Show.class)
				.exchange()
				.expectStatus().isCreated()
				.expectBody(Show.class)
				.value(body -> Assertions.assertEquals(show, body));
	}

	@Test
	void upsertShowEndpoint_WhenUserIsAdmin_WhenShowWithSameIdDoesNotExist_AndShowWithSameNameAlreadyExists_Returns409_ConflictStatus_WithResponseBodySayingShowAlreadyExistsWithName() {
		var showName = "Shingeki no Kyojin";
		var showId = "id";
		var newShow = new Show(showId, showName);
		var existingShowId = "other id";
		var existingShow = new Show(existingShowId, showName);

		Mockito.when(showRepository.findById(showId)).thenReturn(Mono.empty());
		Mockito.when(showRepository.save(ArgumentMatchers.any()))
				.thenReturn(Mono.error(new DuplicateKeyException("duplicate")));

		var adminJwt = jwtEncoder.encodeUserJwt(adminUsername);
		webTestClient.put()
				.uri("/shows/" + showId)
				.header("Authorization", "Bearer " + adminJwt)
				.body(Mono.just(newShow), Show.class)
				.exchange()
				.expectStatus().value(status -> Assertions.assertEquals(HttpStatus.CONFLICT.value(), status))
				.expectBody(ProblemDetails.class)
				.value(body -> {
					Assertions.assertEquals("conflict", body.title());
					Assertions.assertEquals("409", body.status());
					Assertions.assertEquals("A show already exists with the name " + showName, body.detail());
				});
	}

	@Test
	void deleteShowEndpoint_WhenUserIsAdmin_WhenShowExistsForId_Returns200_OkStatus_WithResponseBodySayingShowWasDeleted() {
		var showId = "id";
		var show = new Show(showId, "Shingeki no Kyojin");

		Mockito.when(showRepository.findById(showId)).thenReturn(Mono.just(show));
		Mockito.when(showRepository.delete(show)).thenReturn(Mono.empty());

		var adminJwt = jwtEncoder.encodeUserJwt(adminUsername);
		webTestClient.delete()
				.uri("/shows/" + showId)
				.header("Authorization", "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.value(body -> Assertions.assertEquals("Show with id: " + show.id() + " and name: " + show.name() + " was deleted", body));
	}

	@Test
	void deleteShowEndpoint_WhenUserIsAdmin_WhenShowDoesNotExistForId_Returns404_NotFoundStatus_WithResponseBodySayingShowNotFoundForId() {
		var showId = "id";
		var show = new Show(showId, "Shingeki no Kyojin");

		Mockito.when(showRepository.findById(showId)).thenReturn(Mono.empty());

		var adminJwt = jwtEncoder.encodeUserJwt(adminUsername);
		webTestClient.delete()
				.uri("/shows/" + showId)
				.header("Authorization", "Bearer " + adminJwt)
				.exchange()
				.expectStatus().isNotFound()
				.expectBody(String.class)
				.value(showResponse -> Assertions.assertEquals("No show found for id: " + showId, showResponse));
	}

	@Test
	void importFromMalEndpoint_ReturnsUsersMalList() {
		mockWebServer.enqueue(new MockResponse().setBody(TestConstants.onePageMalListJsonString)
				.addHeader("Content-Type", "application/json"));
		mockWebServer.enqueue(new MockResponse().setBody("[]")
				.addHeader("Content-Type", "application/json"));

		webTestClient.get()
				.uri("/import-from-mal?username=MP7373")
				.exchange()
				.expectStatus().isOk()
				.expectBody(new ParameterizedTypeReference<List<MalShowDetails>>() {})
				.value(showsResponse -> {
					Assertions.assertEquals(243, showsResponse.size());
				});
	}

	@Test
	void importFromMalEndpoint_WhenNotPassedUsernameQueryParam_Returns404_NotFoundStatus_WithResponseBodySayingUsernameQueryParamRequired() {
		mockWebServer.enqueue(new MockResponse().setBody(TestConstants.onePageMalListJsonString)
				.addHeader("Content-Type", "application/json"));
		mockWebServer.enqueue(new MockResponse().setBody("[]")
				.addHeader("Content-Type", "application/json"));

		webTestClient.get()
				.uri("/import-from-mal")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody(ProblemDetails.class)
				.value(problemDetails -> {
					Assertions.assertEquals("Query parameter username is required", problemDetails.detail());
				});
	}
}
