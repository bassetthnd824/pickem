package com.curleesoft.pickem.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.curleesoft.pickem.backend.config.PickemAuthProperties;
import com.curleesoft.pickem.backend.model.User;
import com.curleesoft.pickem.backend.repository.StaleDocumentVersionException;
import com.curleesoft.pickem.backend.repository.UserRepository;
import com.curleesoft.pickem.backend.security.FirebaseIdentityClient;
import com.curleesoft.pickem.backend.security.GoogleProfile;
import com.curleesoft.pickem.backend.security.InvalidCredentialException;
import com.curleesoft.pickem.backend.security.PickemPrincipal;
import com.curleesoft.pickem.backend.security.RoleClaims;
import com.curleesoft.pickem.backend.security.VerifiedIdentity;
import com.curleesoft.pickem.backend.web.AuthProfileResponse;

/**
 * Verifies a Google ID token, provisions {@code users/{uid}}, and mints the
 * Firebase session cookie exchanged for {@code __session}.
 */
@Service
public class AuthService {

    static final String DEFAULT_THEME_ID = "light";

    static final int NICK_NAME_MAX = 40;

    private final FirebaseIdentityClient identityClient;

    private final UserRepository userRepository;

    private final PickemAuthProperties authProperties;

    public AuthService(FirebaseIdentityClient identityClient, UserRepository userRepository,
            PickemAuthProperties authProperties) {
        this.identityClient = identityClient;
        this.userRepository = userRepository;
        this.authProperties = authProperties;
    }

    public String establishSession(String idToken) {
        VerifiedIdentity identity = identityClient.verifyIdToken(idToken);
        GoogleProfile profile = GoogleProfile.from(identity);
        Optional<User> existing = userRepository.findById(identity.uid());
        if (existing.isEmpty()) {
            provision(identity.uid(), profile);
        } else {
            refresh(existing.get(), profile);
        }
        return identityClient.createSessionCookie(idToken, authProperties.sessionDuration());
    }

    public void logout(PickemPrincipal principal) {
        identityClient.revokeRefreshTokens(principal.uid());
    }

    public AuthProfileResponse profile(PickemPrincipal principal) {
        User user = userRepository.findById(principal.uid())
                .orElseThrow(() -> new InvalidCredentialException("Signed-in user is not provisioned"));
        List<String> roles = user.getRoles() == null ? List.of() : List.copyOf(user.getRoles());
        return new AuthProfileResponse(user.getUid(), user.getEmailAddr(), user.getFirstName(), user.getLastName(),
                user.getNickName(), user.getThemeId(), roles);
    }

    private void provision(String uid, GoogleProfile profile) {
        identityClient.setCustomUserClaims(uid, RoleClaims.forRoles(List.of(RoleClaims.PLAYER)));
        User user = new User();
        user.setId(uid);
        user.setUid(uid);
        user.setEmailAddr(profile.email());
        user.setFirstName(profile.firstName());
        user.setLastName(profile.lastName());
        user.setNickName(nickName(profile.firstName()));
        user.setThemeId(DEFAULT_THEME_ID);
        user.setRoles(List.of(RoleClaims.PLAYER));
        try {
            userRepository.save(user, uid);
        } catch (StaleDocumentVersionException ex) {
            User winner = userRepository.findById(uid)
                    .orElseThrow(() -> new InvalidCredentialException("Could not provision the user", ex));
            refresh(winner, profile);
        }
    }

    private void refresh(User user, GoogleProfile profile) {
        user.setEmailAddr(profile.email());
        user.setFirstName(profile.firstName());
        user.setLastName(profile.lastName());
        userRepository.save(user, user.getUid());
    }

    static String nickName(String firstName) {
        String trimmed = firstName == null ? "" : firstName.trim();
        if (trimmed.length() <= NICK_NAME_MAX) {
            return trimmed;
        }
        return trimmed.substring(0, NICK_NAME_MAX);
    }
}
