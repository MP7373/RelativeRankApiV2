package com.relativerank.api.domain;

import java.util.List;

public record ShowList(String userId, List<RankedShow> showList) {

    public ShowList {
        if (showList == null) {
            throw new IllegalArgumentException("showList cannot be null");
        }

        for (var i = 0; i < showList.size(); i++) {
            var rankedShow = showList.get(i);
            if (rankedShow.rank() != i + 1) {
                throw new IllegalArgumentException("show at index: " + i + " was not expected value " + (i + 1));
            }
        }
    }
}
