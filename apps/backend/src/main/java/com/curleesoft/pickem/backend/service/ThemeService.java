package com.curleesoft.pickem.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.model.Theme;
import com.curleesoft.pickem.backend.repository.ThemeRepository;
import com.curleesoft.pickem.backend.repository.TransactionReads;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Transaction;

/**
 * Manager CRUD for themes. Replaces {@code ThemeAction}. A key outside the
 * compiled catalog of 18 is still accepted here; US-36 rejects those creates.
 * Deletes are not reference-checked; that block is US-35.
 */
@Service
public class ThemeService {

    public static final String NOT_FOUND = "Theme not found";

    public static final String NAME_INVALID = "theme name is invalid";

    public static final String PATH_INVALID = "theme path is invalid";

    public static final String PATH_SLASH = "theme path must not begin with a slash";

    public static final String ACTIVE_REQUIRED = "active is required";

    public static final String COLOR_INVALID = "theme color is invalid";

    public static final String NAME_NOT_UNIQUE = "theme name must be unique";

    public static final String PATH_NOT_UNIQUE = "theme path must be unique";

    private static final int NAME_MAX = 40;

    private static final int PATH_MAX = 100;

    private final ThemeRepository themeRepository;

    public ThemeService(ThemeRepository themeRepository) {
        this.themeRepository = themeRepository;
    }

    public List<Theme> search(String themeName, String themePath, Boolean active) {
        String name = normalizeContains(themeName);
        String path = normalizeContains(themePath);

        return themeRepository.findAll().stream()
                .filter(item -> name == null || contains(item.getThemeName(), name))
                .filter(item -> path == null || contains(item.getThemePath(), path))
                .filter(item -> active == null || active.equals(item.getActive()))
                .sorted(Comparator.comparing((Theme item) -> item.getThemeName(),
                        Comparator.nullsLast((String left, String right) -> left.compareTo(right))))
                .toList();
    }

    public Theme get(String id) {
        return themeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    public boolean pathExists(String themePath) {
        if (!StringUtils.hasText(themePath)) {
            return false;
        }

        return !themeRepository.query(themes -> themes.whereEqualTo("themePath", themePath)).isEmpty();
    }

    public Theme create(Theme request) {
        Theme theme = new Theme();
        apply(theme, request);
        return themeRepository.save(theme, (transaction, collection, documentId) -> rejectDuplicate(transaction,
                collection, documentId, theme));
    }

    public Theme update(String id, Theme request) {
        Theme existing = get(id);
        apply(existing, request);
        existing.setVersion(request.getVersion());
        return themeRepository.save(existing, (transaction, collection, documentId) -> rejectDuplicate(transaction,
                collection, documentId, existing));
    }

    public void delete(String id) {
        get(id);
        themeRepository.delete(id);
    }

    private void apply(Theme target, Theme request) {
        String name = trim(request.getThemeName());
        String path = trim(request.getThemePath());

        if (!StringUtils.hasText(name) || name.length() > NAME_MAX) {
            throw new InvalidRequestException(NAME_INVALID);
        }

        if (!StringUtils.hasText(path) || path.length() > PATH_MAX) {
            throw new InvalidRequestException(PATH_INVALID);
        }

        if (path.startsWith("/")) {
            throw new InvalidRequestException(PATH_SLASH);
        }

        if (request.getActive() == null) {
            throw new InvalidRequestException(ACTIVE_REQUIRED);
        }

        String primary = trim(request.getPrimary());
        String secondary = trim(request.getSecondary());

        if (!StringUtils.hasText(primary) || !StringUtils.hasText(secondary)) {
            throw new InvalidRequestException(COLOR_INVALID);
        }

        target.setThemeName(name);
        target.setThemePath(path);
        target.setActive(request.getActive());
        target.setPrimary(primary);
        target.setSecondary(secondary);
    }

    private static void rejectDuplicate(Transaction transaction, CollectionReference collection, String documentId,
            Theme theme) {
        if (TransactionReads.anotherDocumentMatches(transaction,
                collection.whereEqualTo("themeName", theme.getThemeName()), documentId)) {
            throw new ConflictException(NAME_NOT_UNIQUE);
        }

        if (TransactionReads.anotherDocumentMatches(transaction,
                collection.whereEqualTo("themePath", theme.getThemePath()), documentId)) {
            throw new ConflictException(PATH_NOT_UNIQUE);
        }
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
