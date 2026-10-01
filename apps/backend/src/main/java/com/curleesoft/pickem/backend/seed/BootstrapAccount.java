package com.curleesoft.pickem.backend.seed;

/**
 * Firebase Auth user the seed can grant manager and player. No password.
 */
public record BootstrapAccount(String uid, String email, String firstName, String lastName) {
}
