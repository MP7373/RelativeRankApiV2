package com.relativerank.api.dto;

import com.relativerank.api.db.RankedShow;
import com.relativerank.api.db.ShowList;

import java.util.List;

public record UpsertShowListRequest(List<RankedShow> showList) {

    public UpsertShowListRequest {
        ShowList.validateShowList(showList);
    }
}
