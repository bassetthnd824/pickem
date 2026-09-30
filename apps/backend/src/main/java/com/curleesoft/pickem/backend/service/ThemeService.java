package com.curleesoft.pickem.backend.service;

import java.util.Comparator;
import java.util.List;

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

    public static final String PATH_SLASH = "theme path must not begin with a slash";

    public static final String NAME_NOT_UNIQUE = "theme name must be unique";

    public static final String PATH_NOT_UNIQUE = "theme path must be unique";

    private final ThemeRepository themeRepository;

    public ThemeService(ThemeRepository themeRepository) {
        this.themeRepository = themeRepository;
    }

    public List<Theme> search(String themeName, String themePath, Boolean active) {
        String name = SearchText.normalizeContains(themeName);
        String path = SearchText.normalizeContains(themePath);

        return themeRepository.findAll().stream()
                .filter(item -> name == null || SearchText.contains(item.getThemeName(), name))
                .filter(item -> path == null || SearchText.contains(item.getThemePath(), path))
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
        String path = SearchText.trim(request.getThemePath());

        if (path != null && path.startsWith("/")) {
            throw new InvalidRequestException(PATH_SLASH);
        }

        target.setThemeName(SearchText.trim(request.getThemeName()));
        target.setThemePath(path);
        target.setActive(request.getActive());
        target.setPrimary(SearchText.trim(request.getPrimary()));
        target.setSecondary(SearchText.trim(request.getSecondary()));
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

}
