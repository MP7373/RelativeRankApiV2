package com.relativerank.api.routes.handlers;

import com.relativerank.api.db.RankedShow;
import com.relativerank.api.db.Show;
import com.relativerank.api.db.ShowList;
import com.relativerank.api.db.User;
import com.relativerank.api.repositories.ShowListRepository;
import com.relativerank.api.repositories.ShowRepository;
import com.relativerank.api.repositories.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public record DbMigrater(JdbcTemplate jdbcTemplate,
                         ShowRepository showRepository,
                         UserRepository userRepository,
                         ShowListRepository showListRepository) {

    public Mono<ServerResponse> migrate(ServerRequest serverRequest) {
        var selectCount = new AtomicInteger(0);
        var savedCount = new AtomicInteger(0);
        var shows = jdbcTemplate.query("SELECT * FROM public.shows", (rs, rowNumber) -> {
            var showName = rs.getString("name");

            //System.out.println(showName + " select count: " + selectCount.incrementAndGet());
            return showName;
        }).stream()
                .map(showName -> new Show(UUID.randomUUID().toString(), showName))
                .collect(Collectors.toList());

        shows.forEach(show -> {
            showRepository.findByName(show.name())
                    .switchIfEmpty(showRepository.save(show))
                    .doOnError(error -> {
                        System.out.println(show.name() + " errored");
                    })
                    .subscribe();
        });

        var users = jdbcTemplate.query("SELECT * FROM public.users", ((resultSet, rowNumber) -> {
            //System.out.println("select count: " + selectCount.incrementAndGet());
            var username = resultSet.getString("username");
            var password = resultSet.getBytes("password");
            var passwordSalt = resultSet.getBytes(("password_salt"));

            return new User(UUID.randomUUID().toString(), username, password, passwordSalt);
        }));

        users.forEach(user -> {
                userRepository.findByUsername(user.username())
                        .switchIfEmpty(userRepository.save(user).map(savedUser -> {
                            var ids = jdbcTemplate.query("SELECT \"Id\" FROM public.users u WHERE u.username = '" + savedUser.username() + "'", (resultSet, rowNumber) -> {
                                return resultSet.getString(1);
                            });

                            if (ids.size() > 0) {
                                var id = ids.get(0);

                                var rankCount = new AtomicInteger(0);
                                var rankedShowsForUser = jdbcTemplate.query("SELECT name, rank, percentile_rank FROM public.shows JOIN public.user_to_show_mapping ON public.shows.\"Id\" = public.user_to_show_mapping.showid WHERE public.user_to_show_mapping.userid = " + id + " ORDER BY rank", (resultSet, rowNumber) -> {
                                    var name = resultSet.getString("name");
                                    var rank = rankCount.incrementAndGet();
                                    var percentileRank = resultSet.getDouble("percentile_rank");

                                    return new RankedShow(name, rank, percentileRank);
                                });

                                ShowList showList;
                                showList = new ShowList(UUID.randomUUID().toString(), savedUser.username(), rankedShowsForUser);
                                showListRepository.save(showList)
                                        .doOnSuccess(showList1 -> {
                                            System.out.println("show list saved for: " + showList1.username());
                                        })
                                        .subscribe();
                            }
                            return savedUser;
                        }))
                        .subscribe();
        });

        return ServerResponse.ok().build();
    }
}
