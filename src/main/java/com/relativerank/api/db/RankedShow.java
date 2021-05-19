package com.relativerank.api.db;

public record RankedShow(String name,
                         int rank,
                         double percentileRank) {}
