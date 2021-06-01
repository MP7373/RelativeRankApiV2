package com.relativerank.api.db;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
public record GlobalRankedShowList(String id,
                                   Integer numberOfPages,
                                   List<RankedShow> showList) {}
