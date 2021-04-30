package com.relativerank.api.routes;

import com.relativerank.api.routes.handlers.ShowRouteHandlers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class Routes {

    @Bean
    RouterFunction<ServerResponse> apiRoutes(ShowRouteHandlers showRouteHandlers) {
        return RouterFunctions.route()
                .GET("shows", showRouteHandlers::getAllShows)
                .GET("shows/{id}", showRouteHandlers::getShow)
                .POST("shows", showRouteHandlers::createShow)
                .PUT("shows/{id}", showRouteHandlers::upsertShow)
                .DELETE("shows/{id}", showRouteHandlers::deleteShow)
                .GET("import-from-mal", showRouteHandlers::importFromMal)
                .build();
    }
}
