package com.curleesoft.pickem.backend.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.model.Season;
import com.curleesoft.pickem.backend.model.SeasonWeek;
import com.curleesoft.pickem.backend.repository.SeasonRepository;
import com.curleesoft.pickem.backend.repository.SeasonWeekRepository;
import com.curleesoft.pickem.backend.repository.TransactionReads;
import com.curleesoft.pickem.backend.service.SeasonCalendar.ParsedWeek;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Transaction;

/**
 * Manual week CRUD. Thursday begin and Wednesday end (begin + 6 days) are
 * enforced here. CFBD import (US-38) writes season weeks through the repository
 * and is not subject to that day-of-week rule.
 */
@Service
public class SeasonWeekService {

    public static final String NOT_FOUND = "Season week not found";

    public static final String SEASON_NOT_FOUND = "Season not found";

    public static final String NOT_UNIQUE = "week number must be unique for the season";

    private final SeasonWeekRepository seasonWeekRepository;

    private final SeasonRepository seasonRepository;

    public SeasonWeekService(SeasonWeekRepository seasonWeekRepository, SeasonRepository seasonRepository) {
        this.seasonWeekRepository = seasonWeekRepository;
        this.seasonRepository = seasonRepository;
    }

    public List<SeasonWeek> search(String seasonId, Integer weekNumber, String beginDate, String endDate) {
        String season = blankToNull(seasonId);
        String begin = blankToNull(beginDate);
        String end = blankToNull(endDate);
        List<SeasonWeek> candidates = season == null ? seasonWeekRepository.findAll()
                : seasonWeekRepository.query(collection -> collection.whereEqualTo("seasonId", season));

        return candidates.stream().filter(item -> weekNumber == null || weekNumber.equals(item.getWeekNumber()))
                .filter(item -> begin == null || begin.equals(item.getBeginDate()))
                .filter(item -> end == null || end.equals(item.getEndDate()))
                .sorted(Comparator
                        .comparing((SeasonWeek week) -> week.getSeasonId(),
                                Comparator.nullsLast((String left, String right) -> left.compareTo(right)))
                        .thenComparing((SeasonWeek week) -> week.getWeekNumber(),
                                Comparator.nullsLast((Integer left, Integer right) -> left.compareTo(right))))
                .toList();
    }

    public SeasonWeek get(String id) {
        return seasonWeekRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    public SeasonWeek create(SeasonWeek request) {
        SeasonWeek week = new SeasonWeek();
        apply(week, request);
        return seasonWeekRepository.save(week,
                (transaction, collection, documentId) -> rejectDuplicate(transaction, collection, documentId, week));
    }

    public SeasonWeek update(String id, SeasonWeek request) {
        SeasonWeek existing = get(id);
        apply(existing, request);
        existing.setVersion(request.getVersion());
        return seasonWeekRepository.save(existing, (transaction, collection, documentId) -> rejectDuplicate(transaction,
                collection, documentId, existing));
    }

    public void delete(String id) {
        get(id);
        seasonWeekRepository.delete(id);
    }

    private void apply(SeasonWeek target, SeasonWeek request) {
        String seasonId = trim(request.getSeasonId());
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new InvalidRequestException(SEASON_NOT_FOUND));

        if (request.getWeekNumber() == null) {
            throw new InvalidRequestException(SeasonCalendar.WEEK_NUMBER_INVALID);
        }

        ParsedWeek parsed = SeasonCalendar.parseManualWeek(season.getBeginDate(), request.getWeekNumber(),
                trim(request.getBeginDate()), trim(request.getEndDate()));
        target.setSeasonId(season.getId());
        target.setWeekNumber(request.getWeekNumber());
        target.setBeginDate(parsed.beginDate().toString());
        target.setEndDate(parsed.endDate().toString());
    }

    private static void rejectDuplicate(Transaction transaction, CollectionReference collection, String documentId,
            SeasonWeek week) {
        QuerySnapshot snapshot = TransactionReads.get(transaction,
                collection.whereEqualTo("seasonId", week.getSeasonId()));

        for (QueryDocumentSnapshot found : snapshot.getDocuments()) {
            if (found.getId().equals(documentId)) {
                continue;
            }

            SeasonWeek other = found.toObject(SeasonWeek.class);

            if (other != null && week.getWeekNumber().equals(other.getWeekNumber())) {
                throw new ConflictException(NOT_UNIQUE);
            }
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }
}
