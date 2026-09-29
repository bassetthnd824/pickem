package com.curleesoft.pickem.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.model.User;
import com.curleesoft.pickem.backend.repository.TransactionReads;
import com.curleesoft.pickem.backend.repository.UserRepository;
import com.curleesoft.pickem.backend.security.FirebaseIdentityClient;
import com.curleesoft.pickem.backend.security.RoleClaims;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Transaction;

/**
 * Manager CRUD for users. Replaces {@code UserAction}. Roles are written to
 * Firebase with {@code setCustomUserClaims}, which replaces the claim map, so
 * dropping {@code manager} clears that claim. There is no password field.
 * Deletes are not reference-checked; that block is US-35.
 */
@Service
public class UserService {

    public static final String NOT_FOUND = "User not found";

    public static final String UID_REQUIRED = "uid is required";

    public static final String UID_INVALID = "uid is invalid";

    public static final String UID_MISMATCH = "uid does not match the user";

    public static final String ALREADY_EXISTS = "user already exists";

    public static final String EMAIL_INVALID = "email address is invalid";

    public static final String FIRST_NAME_INVALID = "first name is invalid";

    public static final String LAST_NAME_INVALID = "last name is invalid";

    public static final String NICK_NAME_INVALID = "nick name is invalid";

    public static final String THEME_INVALID = "theme is invalid";

    public static final String ROLES_REQUIRED = "User must belong to at least one group.";

    public static final String ROLE_INVALID = "role is invalid";

    public static final String EMAIL_NOT_UNIQUE = "email address must be unique";

    private static final int EMAIL_MAX = 200;

    private static final int NAME_MAX = 40;

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
        String email = normalizeContains(emailAddr);
        String first = normalizeContains(firstName);
        String last = normalizeContains(lastName);

        return userRepository.findAll().stream()
                .filter(item -> email == null || contains(item.getEmailAddr(), email))
                .filter(item -> first == null || contains(item.getFirstName(), first))
                .filter(item -> last == null || contains(item.getLastName(), last))
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
        User saved = userRepository.save(user,
                (transaction, collection, documentId) -> rejectExisting(transaction, collection, documentId),
                (transaction, collection, documentId) -> rejectDuplicateEmail(transaction, collection, documentId,
                        user));
        publishClaims(saved);
        return saved;
    }

    public User update(String id, User request) {
        User existing = get(id);
        String submitted = firstText(request.getUid(), request.getId());

        if (submitted != null && !existing.getUid().equals(submitted)) {
            throw new InvalidRequestException(UID_MISMATCH);
        }

        apply(existing, request);
        existing.setVersion(request.getVersion());
        User saved = userRepository.save(existing, (transaction, collection, documentId) -> rejectDuplicateEmail(
                transaction, collection, documentId, existing));
        publishClaims(saved);
        return saved;
    }

    public void delete(String id) {
        get(id);
        userRepository.delete(id);
    }

    private void apply(User target, User request) {
        String email = trim(request.getEmailAddr());
        String firstName = trim(request.getFirstName());
        String lastName = trim(request.getLastName());
        String nickName = trim(request.getNickName());
        String themeId = trim(request.getThemeId());

        if (!StringUtils.hasText(email) || email.length() > EMAIL_MAX || !validEmail(email)) {
            throw new InvalidRequestException(EMAIL_INVALID);
        }

        if (!StringUtils.hasText(firstName) || firstName.length() > NAME_MAX) {
            throw new InvalidRequestException(FIRST_NAME_INVALID);
        }

        if (!StringUtils.hasText(lastName) || lastName.length() > NAME_MAX) {
            throw new InvalidRequestException(LAST_NAME_INVALID);
        }

        if (!StringUtils.hasText(nickName) || nickName.length() > NAME_MAX) {
            throw new InvalidRequestException(NICK_NAME_INVALID);
        }

        if (!StringUtils.hasText(themeId)) {
            throw new InvalidRequestException(THEME_INVALID);
        }

        if (!themeService.pathExists(themeId)) {
            throw new InvalidRequestException(ThemeService.NOT_FOUND);
        }

        target.setEmailAddr(email);
        target.setFirstName(firstName);
        target.setLastName(lastName);
        target.setNickName(nickName);
        target.setThemeId(themeId);
        target.setRoles(requireRoles(request.getRoles()));
    }

    private void publishClaims(User user) {
        identityClient.setCustomUserClaims(user.getUid(), RoleClaims.forRoles(user.getRoles()));
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

    private static boolean validEmail(String email) {
        int at = email.indexOf('@');
        return at > 0 && email.indexOf('@', at + 1) < 0 && email.indexOf('.', at + 1) > at + 1
                && email.indexOf(' ') < 0;
    }

    private static String firstText(String primary, String fallback) {
        String trimmed = trim(primary);
        if (StringUtils.hasText(trimmed)) {
            return trimmed;
        }
        trimmed = trim(fallback);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private static String normalizeContains(String value) {
        String trimmed = blankToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
