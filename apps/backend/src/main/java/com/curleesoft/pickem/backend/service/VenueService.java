package com.curleesoft.pickem.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.curleesoft.pickem.backend.model.Venue;
import com.curleesoft.pickem.backend.repository.TransactionReads;
import com.curleesoft.pickem.backend.repository.VenueRepository;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Transaction;

/**
 * Manager CRUD for venues. Optional {@code cfbdVenueId} is unique when present.
 * Deletes are not reference-checked here; that block is US-35.
 */
@Service
public class VenueService {

    public static final String NOT_FOUND = "Venue not found";

    public static final String NAME_INVALID = "venue name is invalid";

    public static final String CITY_STATE_INVALID = "cityState is invalid";

    public static final String CFBD_NOT_UNIQUE = "cfbdVenueId must be unique";

    private static final int NAME_MAX = 60;

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public List<Venue> search(String venueName, String cityState, Long cfbdVenueId) {
        String name = normalizeContains(venueName);
        String city = normalizeContains(cityState);

        return venueRepository.findAll().stream()
                .filter(item -> name == null || contains(item.getVenueName(), name))
                .filter(item -> city == null || contains(item.getCityState(), city))
                .filter(item -> cfbdVenueId == null || cfbdVenueId.equals(item.getCfbdVenueId()))
                .sorted(Comparator.comparing((Venue item) -> item.getVenueName(),
                        Comparator.nullsLast((String left, String right) -> left.compareTo(right))))
                .toList();
    }

    public Venue get(String id) {
        return venueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    public Venue create(Venue request) {
        Venue venue = new Venue();
        apply(venue, request);
        return venueRepository.save(venue,
                (transaction, collection, documentId) -> rejectDuplicate(transaction, collection, documentId, venue));
    }

    public Venue update(String id, Venue request) {
        Venue existing = get(id);
        apply(existing, request);
        existing.setVersion(request.getVersion());
        return venueRepository.save(existing, (transaction, collection, documentId) -> rejectDuplicate(transaction,
                collection, documentId, existing));
    }

    public void delete(String id) {
        get(id);
        venueRepository.delete(id);
    }

    private void apply(Venue target, Venue request) {
        String name = trim(request.getVenueName());
        String cityState = trim(request.getCityState());

        if (!StringUtils.hasText(name) || name.length() > NAME_MAX) {
            throw new InvalidRequestException(NAME_INVALID);
        }

        if (!StringUtils.hasText(cityState) || cityState.length() > NAME_MAX) {
            throw new InvalidRequestException(CITY_STATE_INVALID);
        }

        target.setVenueName(name);
        target.setCityState(cityState);
        target.setCfbdVenueId(request.getCfbdVenueId());
    }

    private static void rejectDuplicate(Transaction transaction, CollectionReference collection, String documentId,
            Venue venue) {
        Long externalId = venue.getCfbdVenueId();

        if (externalId == null) {
            return;
        }

        if (TransactionReads.anotherDocumentMatches(transaction, collection.whereEqualTo("cfbdVenueId", externalId),
                documentId)) {
            throw new ConflictException(CFBD_NOT_UNIQUE);
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
