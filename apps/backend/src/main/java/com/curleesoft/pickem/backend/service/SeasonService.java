package com.curleesoft.pickem.backend.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.curleesoft.pickem.backend.model.Season;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.repository.TransactionReads;
import com.curleesoft.pickem.backend.service.SeasonCalendar.ParsedSeason;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Transaction;

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
        String seasonFilter = SearchText.normalizeContains(season);
        String begin = SearchText.blankToNull(beginDate);
        String end = SearchText.blankToNull(endDate);

        return seasonRepository.findAll().stream()
                .filter(item -> seasonFilter == null || SearchText.contains(item.getSeason(), seasonFilter))
                .filter(item -> begin == null || begin.equals(item.getBeginDate()))
                .filter(item -> end == null || end.equals(item.getEndDate()))
                .filter(item -> current == null || item.isCurrent() == current.booleanValue())
                .sorted(Comparator.comparing((Season item) -> item.getSeason(),
                        Comparator.nullsLast((String left, String right) -> left.compareTo(right))))
                .toList();
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
        return seasonRepository.save(season, (transaction, collection, documentId) -> rejectDuplicate(transaction,
                collection, documentId, season.getSeason()));
    }

    public Season update(String id, Season request) {
        Season existing = get(id);
        apply(existing, request);
        existing.setVersion(request.getVersion());
        return seasonRepository.save(existing, (transaction, collection, documentId) -> rejectDuplicate(transaction,
                collection, documentId, existing.getSeason()));
    }

    public void delete(String id) {
        get(id);
        seasonRepository.delete(id);
    }

    private void apply(Season target, Season request) {
        ParsedSeason parsed = SeasonCalendar.parseSeason(SearchText.trim(request.getSeason()),
                SearchText.trim(request.getBeginDate()), SearchText.trim(request.getEndDate()));
        target.setSeason(parsed.season());
        target.setBeginDate(parsed.beginDate().toString());
        target.setEndDate(parsed.endDate().toString());
        target.setCurrent(request.isCurrent());
    }

    private static void rejectDuplicate(Transaction transaction, CollectionReference collection, String documentId,
            String seasonYear) {
        if (TransactionReads.anotherDocumentMatches(transaction, collection.whereEqualTo("season", seasonYear),
                documentId)) {
            throw new ConflictException(NOT_UNIQUE);
        }
    }

}
