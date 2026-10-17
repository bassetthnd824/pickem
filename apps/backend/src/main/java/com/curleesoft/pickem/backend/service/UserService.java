package com.curleesoft.pickem.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.model.User;
import com.curleesoft.pickem.backend.repository.BaseRepository;
import com.curleesoft.pickem.backend.repository.TransactionReads;
import com.curleesoft.pickem.backend.repository.UserRepository;
import com.curleesoft.pickem.backend.security.FirebaseIdentityClient;
import com.curleesoft.pickem.backend.security.IdentityAdminException;
import com.curleesoft.pickem.backend.security.RoleClaims;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Transaction;

/**
 * Manager CRUD for users. Replaces {@code UserAction}. Roles are written to
 * Firebase before the user document. {@code setCustomUserClaims} replaces the
 * claim map, so dropping {@code manager} clears that claim. If the document
 * write does not commit, claims are put back in line with the stored document.
 * Delete clears claims and revokes refresh tokens before removing the document,
 * so an existing session cookie stops authorizing. There is no password field.
 * Deletes are not reference-checked; that block is US-35.
 */
@Service
public class UserService {

    public static final String NOT_FOUND = "User not found";

    public static final String UID_REQUIRED = "uid is required";

    public static final String UID_INVALID = "uid is invalid";

    public static final String UID_MISMATCH = "uid does not match the user";

    public static final String ALREADY_EXISTS = "user already exists";

    public static final String ROLES_REQUIRED = "User must belong to at least one group.";

    public static final String ROLE_INVALID = "role is invalid";

    public static final String EMAIL_NOT_UNIQUE = "email address must be unique";

    private static final int UID_MAX = 128;

    private final UserRepository userRepository;

    private final ThemeService themeService;

    private final FirebaseIdentityClient identityClient;

    public UserService(UserRepository userRepository, ThemeService themeService,
            FirebaseIdentityClient identityClient) {
        this.userRepository = userRepository;
        this.themeService = themeService;
        this.identityClient = identityClient;
    }

    public List<User> search(String emailAddr, String firstName, String lastName) {
        String email = SearchText.normalizeContains(emailAddr);
        String first = SearchText.normalizeContains(firstName);
        String last = SearchText.normalizeContains(lastName);

        return userRepository.findAll().stream()
                .filter(item -> email == null || SearchText.contains(item.getEmailAddr(), email))
                .filter(item -> first == null || SearchText.contains(item.getFirstName(), first))
                .filter(item -> last == null || SearchText.contains(item.getLastName(), last))
                .sorted(Comparator.comparing((User item) -> item.getEmailAddr(),
                        Comparator.nullsLast((String left, String right) -> left.compareTo(right))))
                .toList();
    }

    public User get(String id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    public User create(User request) {
        String uid = requireUid(request.getUid(), request.getId());
        if (userRepository.findById(uid).isPresent()) {
            throw new ConflictException(ALREADY_EXISTS);
        }

        User user = new User();
        user.setId(uid);
        user.setUid(uid);
        apply(user, request);
        return commit(user, List.of(),
                (transaction, collection, documentId) -> rejectExisting(transaction, collection, documentId),
                (transaction, collection, documentId) -> rejectDuplicateEmail(transaction, collection, documentId,
                        user));
    }

    public User update(String id, User request) {
        User existing = get(id);
        String submitted = firstText(request.getUid(), request.getId());

        if (submitted != null && !existing.getUid().equals(submitted)) {
            throw new InvalidRequestException(UID_MISMATCH);
        }

        List<String> previousRoles = existing.getRoles() == null ? List.of() : List.copyOf(existing.getRoles());
        apply(existing, request);
        existing.setVersion(request.getVersion());
        return commit(existing, previousRoles, (transaction, collection, documentId) -> rejectDuplicateEmail(
                transaction, collection, documentId, existing));
    }

    public void delete(String id) {
        User existing = get(id);
        String uid = existing.getUid();

        try {
            RoleClaimSync.publish(identityClient, uid, List.of());
        } catch (IdentityAdminException ex) {
            if (ex.getKind() != IdentityAdminException.Kind.USER_NOT_FOUND) {
                throw ex;
            }

            userRepository.delete(id);
            return;
        }

        try {
            identityClient.revokeRefreshTokens(uid);
        } catch (IdentityAdminException ex) {
            if (ex.getKind() != IdentityAdminException.Kind.USER_NOT_FOUND) {
                RoleClaimSync.align(identityClient, userRepository, uid, existing.getRoles());
                throw ex;
            }
        }

        userRepository.delete(id);
    }

    private User commit(User user, List<String> claimsIfMissing,
            BaseRepository.SaveConstraint... constraints) {
        RoleClaimSync.publish(identityClient, user.getUid(), user.getRoles());

        try {
            return userRepository.save(user, constraints);

        } catch (RuntimeException ex) {
            try {
                RoleClaimSync.align(identityClient, userRepository, user.getUid(), claimsIfMissing);
            } catch (RuntimeException restore) {
                IdentityAdminException failure = new IdentityAdminException(IdentityAdminException.Kind.UNAVAILABLE,
                        IdentityAdminException.CLAIMS_NOT_RESTORED, restore);
                failure.addSuppressed(ex);
                throw failure;
            }

            throw ex;
        }
    }

    private void apply(User target, User request) {
        String themeId = SearchText.trim(request.getThemeId());

        if (!themeService.pathExists(themeId)) {
            throw new InvalidRequestException(ThemeService.NOT_FOUND);
        }

        target.setEmailAddr(SearchText.trim(request.getEmailAddr()));
        target.setFirstName(SearchText.trim(request.getFirstName()));
        target.setLastName(SearchText.trim(request.getLastName()));
        target.setNickName(SearchText.trim(request.getNickName()));
        target.setThemeId(themeId);
        target.setRoles(requireRoles(request.getRoles()));
    }

    private static void rejectExisting(Transaction transaction, CollectionReference collection, String documentId) {
        CollectionReference documents = Objects.requireNonNull(collection, "collection");
        String id = Objects.requireNonNull(documentId, "documentId");
        DocumentReference reference = documents.document(id);

        if (TransactionReads.documentExists(transaction, reference)) {
            throw new ConflictException(ALREADY_EXISTS);
        }
    }

    private static void rejectDuplicateEmail(Transaction transaction, CollectionReference collection,
            String documentId, User user) {
        if (TransactionReads.anotherDocumentMatches(transaction,
                collection.whereEqualTo("emailAddr", user.getEmailAddr()), documentId)) {
            throw new ConflictException(EMAIL_NOT_UNIQUE);
        }
    }

    private static String requireUid(String uid, String id) {
        String resolved = firstText(uid, id);

        if (resolved == null) {
            throw new InvalidRequestException(UID_REQUIRED);
        }

        if (resolved.length() > UID_MAX || resolved.indexOf('/') >= 0 || ".".equals(resolved)
                || "..".equals(resolved)) {
            throw new InvalidRequestException(UID_INVALID);
        }

        return resolved;
    }

    private static List<String> requireRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new InvalidRequestException(ROLES_REQUIRED);
        }

        List<String> submitted = new ArrayList<>();

        for (String role : roles) {
            if (!StringUtils.hasText(role)) {
                continue;
            }

            String normalized = role.trim().toLowerCase(Locale.ROOT);

            if (!RoleClaims.PLAYER.equals(normalized) && !RoleClaims.MANAGER.equals(normalized)) {
                throw new InvalidRequestException(ROLE_INVALID);
            }

            submitted.add(normalized);
        }

        List<String> ordered = RoleClaims.normalize(submitted);

        if (ordered.isEmpty()) {
            throw new InvalidRequestException(ROLES_REQUIRED);
        }

        return ordered;
    }

    private static String firstText(String primary, String fallback) {
        String trimmed = SearchText.trim(primary);
        if (StringUtils.hasText(trimmed)) {
            return trimmed;
        }
        trimmed = SearchText.trim(fallback);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }
}
