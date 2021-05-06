package com.relativerank.api.domain;

public record User(String id, String username, byte[] password, byte[] passwordSalt) {}
