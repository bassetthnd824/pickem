package com.curleesoft.pickem.backend.service;

import java.util.List;

import com.curleesoft.pickem.backend.model.User;
import com.curleesoft.pickem.backend.repository.UserRepository;
import com.curleesoft.pickem.backend.security.FirebaseIdentityClient;
import com.curleesoft.pickem.backend.security.RoleClaims;

/**
 * Writes Firebase role claims, then puts them back in line with the stored user
 * document when the Firestore write does not commit.
 */
final class RoleClaimSync {

    private RoleClaimSync() {
    }

    static void publish(FirebaseIdentityClient client, String uid, List<String> roles) {
        client.setCustomUserClaims(uid, RoleClaims.forRoles(roles));
    }

    static void align(FirebaseIdentityClient client, UserRepository users, String uid, List<String> claimsIfMissing) {
        User stored = users.findById(uid).orElse(null);
        List<String> roles = stored == null ? claimsIfMissing : stored.getRoles();
        publish(client, uid, roles);
    }
}
