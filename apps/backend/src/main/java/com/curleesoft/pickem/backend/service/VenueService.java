package com.curleesoft.pickem.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.curleesoft.pickem.backend.model.Venue;
import com.curleesoft.pickem.backend.repository.StaleDocumentVersionException;
import com.curleesoft.pickem.backend.repository.TransactionReads;
import com.curleesoft.pickem.backend.repository.VenueRepository;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Transaction;

/**
 * Manager CRUD for venues. Optional {@code cfbdVenueId} is unique when present.
 * {@link ReferenceGuard} rewrites team and matchup venue snapshots after a
 * save, and blocks a delete while a team or matchup still uses the venue.
 */
@Service
public class VenueService {

    public static final String NOT_FOUND = "Venue not found";

    public static final String CFBD_NOT_UNIQUE = "cfbdVenueId must be unique";

    private final VenueRepository venueRepository;

    private final ReferenceGuard referenceGuard;

    public VenueService(VenueRepository venueRepository, ReferenceGuard referenceGuard) {
        this.venueRepository = venueRepository;
        this.referenceGuard = referenceGuard;
    }

    public List<Venue> search(String venueName, String cityState, Long cfbdVenueId) {
        String name = SearchText.normalizeContains(venueName);
        String city = SearchText.normalizeContains(cityState);

        return venueRepository.findAll().stream()
                .filter(item -> name == null || SearchText.contains(item.getVenueName(), name))
                .filter(item -> city == null || SearchText.contains(item.getCityState(), city))
                .filter(item -> cfbdVenueId == null || cfbdVenueId.equals(item.getCfbdVenueId()))
                .sorted(
                        Comparator.comparing(
                                (Venue item) -> item.getVenueName(),
                                Comparator.nullsLast((String left, String right) -> left.compareTo(right))
                        )
                )
                .toList();
    }

    public Venue get(String id) {
        return venueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    public Venue create(Venue request) {
        Venue venue = new Venue();
        apply(venue, request);
        return venueRepository.save(
                venue,
                (transaction, collection, documentId) -> rejectDuplicate(transaction, collection, documentId, venue)
        );
    }

    public Venue update(String id, Venue request) {
        Venue existing = get(id);
        apply(existing, request);
        existing.setVersion(request.getVersion());
        Venue saved = saveOrResume(existing);
        referenceGuard.refreshVenue(saved);
        return saved;
    }

    public void delete(String id) {
        get(id);
        referenceGuard.deleteVenue(Objects.requireNonNull(id, "id"));
    }

    /**
     * A previous attempt may have committed this edit and then failed while
     * rewriting snapshots. A later snapshot patch can move the version by more than
     * one, so any newer stored row with the same fields is refreshed again. A
     * submitted version newer than the row still conflicts.
     */
    private Venue saveOrResume(Venue desired) {

        try {
            return venueRepository.save(
                    desired,
                    (transaction, collection, documentId) -> rejectDuplicate(
                            transaction,
                            collection,
                            documentId,
                            desired
                    )
            );

        } catch (StaleDocumentVersionException ex) {
            Venue stored = get(Objects.requireNonNull(desired.getId(), "id"));

            if (storedIsNewer(stored.getVersion(), desired.getVersion()) && sameCommitted(stored, desired)) {
                return stored;

            }

            throw ex;

        }

    }

    private static boolean storedIsNewer(Long storedVersion, Long submittedVersion) {

        if (submittedVersion == null) {
            return false;

        }

        long stored = storedVersion == null ? 0L : storedVersion;
        return stored > submittedVersion;
    }

    private static boolean sameCommitted(Venue stored, Venue desired) {
        return Objects.equals(stored.getVenueName(), desired.getVenueName())
                && Objects.equals(stored.getCityState(), desired.getCityState())
                && Objects.equals(stored.getCfbdVenueId(), desired.getCfbdVenueId());
    }

    private void apply(Venue target, Venue request) {
        target.setVenueName(SearchText.trim(request.getVenueName()));
        target.setCityState(SearchText.trim(request.getCityState()));
        target.setCfbdVenueId(request.getCfbdVenueId());
    }

    private static void rejectDuplicate(
            Transaction transaction,
            CollectionReference collection,
            String documentId,
            Venue venue
    ) {
        Long externalId = venue.getCfbdVenueId();

        if (externalId == null) {
            return;

        }

        if (TransactionReads.anotherDocumentMatches(
                transaction,
                collection.whereEqualTo("cfbdVenueId", externalId),
                documentId
        )) {
            throw new ConflictException(CFBD_NOT_UNIQUE);

        }

    }

}
