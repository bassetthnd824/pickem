package com.curleesoft.pickem.backend.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.model.Season;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.service.SeasonCalendar.ParsedSeason;

@Service
public class SeasonService {

    public static final String NOT_FOUND = "Season not found";

    public static final String NOT_UNIQUE = "season must be unique";

    public static final String NO_CURRENT = "No current season";

    private final SeasonRepository seasonRepository;

    private final Clock clock;

    public SeasonService(SeasonRepository seasonRepository, Clock clock) {
        this.seasonRepository = seasonRepository;
        this.clock = clock;
    }

    public List<Season> search(String season, String beginDate, String endDate, Boolean current) {
        String seasonFilter = normalizeContains(season);
        String begin = blankToNull(beginDate);
        String end = blankToNull(endDate);

        return seasonRepository.findAll().stream()
                .filter(item -> seasonFilter == null || contains(item.getSeason(), seasonFilter))
                .filter(item -> begin == null || begin.equals(item.getBeginDate()))
                .filter(item -> end == null || end.equals(item.getEndDate()))
                .filter(item -> current == null || item.isCurrent() == current.booleanValue())
                .sorted(Comparator.comparing(Season::getSeason, Comparator.nullsLast(String::compareTo))).toList();
    }

    public Season get(String id) {
        return seasonRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    public Season current() {
        return SeasonCalendar.selectCurrent(seasonRepository.findAll(), LocalDate.now(clock))
                .orElseThrow(() -> new ResourceNotFoundException(NO_CURRENT));
    }

    public Season create(Season request) {
        Season season = new Season();
        apply(season, request);
        ensureUnique(season);
        return seasonRepository.save(season);
    }

    public Season update(String id, Season request) {
        Season existing = get(id);
        apply(existing, request);
        existing.setVersion(request.getVersion());
        ensureUnique(existing);
        return seasonRepository.save(existing);
    }

    public void delete(String id) {
        get(id);
        seasonRepository.delete(id);
    }

    private void apply(Season target, Season request) {
        ParsedSeason parsed = SeasonCalendar.parseSeason(trim(request.getSeason()), trim(request.getBeginDate()),
                trim(request.getEndDate()));
        target.setSeason(parsed.season());
        target.setBeginDate(parsed.beginDate().toString());
        target.setEndDate(parsed.endDate().toString());
        target.setCurrent(request.isCurrent());
    }

    private void ensureUnique(Season season) {
        boolean duplicate = seasonRepository.query(collection -> collection.whereEqualTo("season", season.getSeason()))
                .stream().anyMatch(found -> !found.getId().equals(season.getId()));

        if (duplicate) {
            throw new ConflictException(NOT_UNIQUE);
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
