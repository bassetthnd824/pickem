package com.curleesoft.pickem.backend.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

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

    public static final String CFBD_NOT_UNIQUE = "cfbdVenueId must be unique";

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public List<Venue> search(String venueName, String cityState, Long cfbdVenueId) {
        String name = SearchText.normalizeContains(venueName);
        String city = SearchText.normalizeContains(cityState);

        return venueRepository.findAll().stream()
                .filter(item -> name == null || SearchText.contains(item.getVenueName(), name))
                .filter(item -> city == null || SearchText.contains(item.getCityState(), city))
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
        target.setVenueName(SearchText.trim(request.getVenueName()));
        target.setCityState(SearchText.trim(request.getCityState()));
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

}
