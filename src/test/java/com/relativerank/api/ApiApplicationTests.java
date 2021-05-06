package com.relativerank.api;

import com.relativerank.api.domain.Show;
import com.relativerank.api.dto.MalShowDetails;
import com.relativerank.api.repositories.ShowRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {ApiApplication.class, ApiApplicationTests.TestConfig.class})
@ActiveProfiles("test")
class ApiApplicationTests {

	static class TestConfig {

		@Bean
		public String malUserListUrl() {
			var baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
			return baseUrl;
		}
	}

	static {
		var server = new MockWebServer();
		try {
			server.start();
		} catch (IOException e) {
			throw new RuntimeException();
		}
		mockWebServer = server;
	}

	private static final MockWebServer mockWebServer;

	@AfterAll
	static void shutDownMockWebServer() throws IOException {
		mockWebServer.shutdown();
	}

	@MockBean
	private ShowRepository showRepository;

	@Autowired
	private WebTestClient webTestClient;

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
	void createShowEndpoint_WhenShowWithSameNameDoesNotExist_Returns201_CreatedStatus_WithResponseBodyContainingShow() {
		var show = new Show(null, "Shingeki no Kyojin");
		var createdShow = new Show("id", show.name());

		Mockito.when(showRepository.findByName(show.name())).thenReturn(Mono.empty());
		Mockito.when(showRepository.save(show)).thenReturn(Mono.just(createdShow));

		webTestClient.post()
				.uri("/shows")
				.body(Mono.just(show), Show.class)
				.exchange()
				.expectStatus().isCreated()
				.expectBody(Show.class)
				.value(showResponse -> Assertions.assertEquals(createdShow, showResponse));
	}

	@Test
	void createShowEndpoint_WhenShowWithSameNameAlreadyExists_Returns409_ConflictStatus_WithResponseBodySayingShowAlreadyExistsWithName() {
		var show = new Show(null, "Shingeki no Kyojin");
		var existingShow = new Show("id", show.name());

		Mockito.when(showRepository.findByName(show.name())).thenReturn(Mono.just(existingShow));

		webTestClient.post()
				.uri("/shows")
				.body(Mono.just(show), Show.class)
				.exchange()
				.expectStatus().value(status -> Assertions.assertEquals(HttpStatus.CONFLICT.value(), status))
				.expectBody(String.class)
				.value(body -> Assertions.assertEquals("A show already exists with the name " + show.name(), body));
	}

	@Test
	void upsertShowEndpoint_WhenShowWithSameIdAlreadyExists_Returns200_OkStatus_WithResponseBodyContainingUpdatedShow() {
		var showId = "id";
		var updatedShow = new Show(showId, "Shingeki no Kyojin");
		var existingShow = new Show(showId, "Shingeki no Kyojiin Typo Name");

		Mockito.when(showRepository.findById(showId)).thenReturn(Mono.just(existingShow));
		Mockito.when(showRepository.save(updatedShow)).thenReturn(Mono.just(updatedShow));

		webTestClient.put()
				.uri("/shows/" + showId)
				.body(Mono.just(updatedShow), Show.class)
				.exchange()
				.expectStatus().isOk()
				.expectBody(Show.class)
				.value(body -> Assertions.assertEquals(updatedShow, body));
	}

	@Test
	void upsertShowEndpoint_WhenShowWithSameIdDoesNotExist_AndShowWithSameNameDoesNotExist_Returns201_CreatedStatus_WithResponseBodyContainingCreatedShow() {
		var showId = "id";
		var show = new Show(showId, "Shingeki no Kyojin");

		Mockito.when(showRepository.findById(showId)).thenReturn(Mono.empty());
		Mockito.when(showRepository.findByName(show.name())).thenReturn(Mono.empty());
		Mockito.when(showRepository.save(show)).thenReturn(Mono.just(show));

		webTestClient.put()
				.uri("/shows/" + showId)
				.body(Mono.just(show), Show.class)
				.exchange()
				.expectStatus().isCreated()
				.expectBody(Show.class)
				.value(body -> Assertions.assertEquals(show, body));
	}

	@Test
	void upsertShowEndpoint_WhenShowWithSameIdDoesNotExist_AndShowWithSameNameAlreadyExists_Returns409_ConflictStatus_WithResponseBodySayingShowAlreadyExistsWithName() {
		var showName = "Shingeki no Kyojin";
		var showId = "id";
		var newShow = new Show(showId, showName);
		var existingShowId = "other id";
		var existingShow = new Show(existingShowId, showName);

		Mockito.when(showRepository.findById(showId)).thenReturn(Mono.empty());
		Mockito.when(showRepository.findByName(newShow.name())).thenReturn(Mono.just(existingShow));

		webTestClient.put()
				.uri("/shows/" + showId)
				.body(Mono.just(newShow), Show.class)
				.exchange()
				.expectStatus().value(status -> Assertions.assertEquals(HttpStatus.CONFLICT.value(), status))
				.expectBody(String.class)
				.value(body -> Assertions.assertEquals("A show already exists with the name " + newShow.name(), body));
	}

	@Test
	void deleteShowEndpoint_WhenShowExistsForId_Returns200_OkStatus_WithResponseBodySayingShowWasDeleted() {
		var showId = "id";
		var show = new Show(showId, "Shingeki no Kyojin");

		Mockito.when(showRepository.findById(showId)).thenReturn(Mono.just(show));
		Mockito.when(showRepository.delete(show)).thenReturn(Mono.empty());

		webTestClient.delete()
				.uri("/shows/" + showId)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.value(body -> Assertions.assertEquals("Show with id: " + show.id() + " and name: " + show.name() + " was deleted", body));
	}

	@Test
	void deleteShowEndpoint_WhenShowDoesNotExistForId_Returns404_NotFoundStatus_WithResponseBodySayingShowNotFoundForId() {
		var showId = "id";
		var show = new Show(showId, "Shingeki no Kyojin");

		Mockito.when(showRepository.findById(showId)).thenReturn(Mono.empty());

		webTestClient.delete()
				.uri("/shows/" + showId)
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
}
