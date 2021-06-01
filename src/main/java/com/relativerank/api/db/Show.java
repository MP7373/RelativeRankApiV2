package com.relativerank.api.db;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public record Show(String id,
                   @TextIndexed @Indexed(unique = true) String name) {}
