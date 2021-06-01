package com.relativerank.api.db;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
public record ShowList(String id,
                       @Indexed(unique = true) String username,
                       List<RankedShow> showList) {

    public ShowList {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        if (username == null) {
            throw new IllegalArgumentException("username cannot be null");
        }

        if (showList == null) {
            throw new IllegalArgumentException("showList cannot be null");
        }

        validateShowList(showList);
    }

    public static void validateShowList(List<RankedShow> showList) {
        for (var i = 0; i < showList.size(); i++) {
            var rankedShow = showList.get(i);
            if (rankedShow.rank() != i + 1) {
                throw new IllegalArgumentException("show rank at index: " + i + " was not expected value " + (i + 1));
            }

            var expectedPercentileRank = 1 - (1.0 / (1 + showList.size()) * (i + 1));
            if (Math.abs(rankedShow.percentileRank() - expectedPercentileRank) > .01) {
                throw new IllegalArgumentException(
                        "show percentile rank at index: " + i + " was not expected value " + expectedPercentileRank);
            }
        }
    }
}
