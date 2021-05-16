package com.relativerank.api.routes;

import com.relativerank.api.routes.handlers.ShowListRouteHandlers;
import com.relativerank.api.routes.handlers.ShowRouteHandlers;
import com.relativerank.api.routes.handlers.UserRouteHandlers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class Routes {

    @Bean
    RouterFunction<ServerResponse> apiRoutes(UserRouteHandlers userRouteHandlers,
                                             ShowRouteHandlers showRouteHandlers,
                                             ShowListRouteHandlers showListRouteHandlers) {
        return RouterFunctions.route()
                .POST("/login", userRouteHandlers::login)
                .POST("/users", userRouteHandlers::createUser)
                .PATCH("/users/{username}", userRouteHandlers::updateUser)
                .DELETE("/users/{username}", userRouteHandlers::deleteUser)
                .GET("/shows", showRouteHandlers::getAllShows)
                .GET("/shows/{id}", showRouteHandlers::getShow)
                .POST("/shows", showRouteHandlers::createShow)
                .PUT("/shows/{id}", showRouteHandlers::upsertShow)
                .DELETE("/shows/{id}", showRouteHandlers::deleteShow)
                .GET("/import-from-mal", showRouteHandlers::importFromMal)
                .GET("show-lists/{username}", showListRouteHandlers::getShowList)
                .build();
    }
}
