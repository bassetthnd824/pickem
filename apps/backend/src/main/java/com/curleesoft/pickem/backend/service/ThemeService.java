package com.curleesoft.pickem.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.model.Theme;
import com.curleesoft.pickem.backend.repository.ThemeRepository;
import com.curleesoft.pickem.backend.repository.TransactionReads;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.Transaction;

/**
 * Manager CRUD for themes. Replaces {@code ThemeAction}. Create and update
 * accept only the compiled catalog of 18 keys. Managers may still activate,
 * deactivate, and rename those rows. Deletes are not reference-checked; that
 * block is US-35.
 */
@Service
public class ThemeService {

    public static final String NOT_FOUND = "Theme not found";

    public static final String PATH_SLASH = "theme path must not begin with a slash";

    public static final String KEY_NOT_IN_CATALOG = "theme key is not one of the 18 compiled palettes";

    public static final String THEME_INACTIVE = "theme is not active";

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
                .sorted(
                        Comparator.comparing(
                                (Theme item) -> item.getThemeName(),
                                Comparator.nullsLast((String left, String right) -> left.compareTo(right))
                        )
                )
                .toList();
    }

    public Theme get(String id) {
        return themeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    public boolean pathExists(String themePath) {
        return findByPath(themePath).isPresent();
    }

    public Optional<Theme> findByPath(String themePath) {
        String path = SearchText.trim(themePath);

        if (!StringUtils.hasText(path)) {
            return Optional.empty();

        }

        String key = Objects.requireNonNull(path, "themePath");
        List<Theme> matches = themeRepository.query(themes -> {
            CollectionReference documents = Objects.requireNonNull(themes, "themes");
            Query query = documents.whereEqualTo("themePath", key);
            return Objects.requireNonNull(query, "query");

        });

        if (matches.isEmpty()) {
            return Optional.empty();

        }

        Theme found = matches.get(0);

        if (found == null) {
            return Optional.empty();

        }

        return Optional.of(found);
    }

    /**
     * Account picker rows: active documents whose key has a CSS block, in catalog
     * order. An inactive theme stays on users who already chose it, but it is not
     * offered here.
     */
    public List<ThemeChoice> activeChoices() {
        List<ThemeChoice> choices = new ArrayList<>();

        for (Theme theme : themeRepository.findAll()) {

            if (theme == null || !ThemeCatalog.contains(theme.getThemePath())) {
                continue;

            }

            if (!Boolean.TRUE.equals(theme.getActive())) {
                continue;

            }

            String id = theme.getId();
            String name = theme.getThemeName();
            String themePath = theme.getThemePath();

            if (id == null || name == null || themePath == null) {
                continue;

            }

            choices.add(
                    new ThemeChoice(
                            Objects.requireNonNull(id, "theme id"),
                            Objects.requireNonNull(name, "theme name"),
                            Objects.requireNonNull(themePath, "theme path")
                    )
            );

        }

        choices.sort(Comparator.comparingInt(choice -> {
            ThemeChoice row = Objects.requireNonNull(choice, "choice");
            String themePath = Objects.requireNonNull(row.themePath(), "themePath");
            return ThemeCatalog.indexOf(themePath);

        }));
        return List.copyOf(choices);
    }

    public Theme create(Theme request) {
        Theme theme = new Theme();
        apply(theme, request);
        return themeRepository.save(
                theme,
                (transaction, collection, documentId) -> rejectDuplicate(
                        transaction,
                        collection,
                        documentId,
                        theme
                )
        );
    }

    public Theme update(String id, Theme request) {
        Theme existing = get(id);
        apply(existing, request);
        existing.setVersion(request.getVersion());
        return themeRepository.save(
                existing,
                (transaction, collection, documentId) -> rejectDuplicate(
                        transaction,
                        collection,
                        documentId,
                        existing
                )
        );
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

        if (!ThemeCatalog.contains(path)) {
            throw new InvalidRequestException(KEY_NOT_IN_CATALOG);

        }

        target.setThemeName(SearchText.trim(request.getThemeName()));
        target.setThemePath(path);
        target.setActive(request.getActive());
        target.setPrimary(SearchText.trim(request.getPrimary()));
        target.setSecondary(SearchText.trim(request.getSecondary()));
    }

    private static void rejectDuplicate(
            Transaction transaction,
            CollectionReference collection,
            String documentId,
            Theme theme
    ) {

        if (TransactionReads.anotherDocumentMatches(
                transaction,
                collection.whereEqualTo("themeName", theme.getThemeName()),
                documentId
        )) {
            throw new ConflictException(NAME_NOT_UNIQUE);

        }

        if (TransactionReads.anotherDocumentMatches(
                transaction,
                collection.whereEqualTo("themePath", theme.getThemePath()),
                documentId
        )) {
            throw new ConflictException(PATH_NOT_UNIQUE);

        }

    }

}
