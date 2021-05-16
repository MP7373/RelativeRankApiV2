package com.relativerank.api.domain;

public record User(String id, String username, byte[] hashedPassword, byte[] passwordSalt) {}
