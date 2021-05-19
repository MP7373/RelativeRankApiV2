package com.relativerank.api.db;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public record User(String id,
                   @Indexed(unique = true) String username,
                   byte[] hashedPassword,
                   byte[] passwordSalt) {}
