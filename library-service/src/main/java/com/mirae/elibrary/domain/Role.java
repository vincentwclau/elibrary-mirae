package com.mirae.elibrary.domain;

/**
 * Application roles. MEMBER is the default for self-registered users; ADMIN is
 * reserved for catalogue management (seeded only in this scope).
 */
public enum Role {
    MEMBER,
    ADMIN
}
