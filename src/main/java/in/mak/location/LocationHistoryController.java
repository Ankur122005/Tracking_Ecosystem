package in.mak.location;

import in.mak.family.Family;
import in.mak.family.FamilyMember;
import in.mak.family.FamilyMemberRepository;
import in.mak.family.FamilyRepository;
import in.mak.user.User;
import in.mak.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/location-history")
public class LocationHistoryController {

    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;

    public LocationHistoryController(
            LocationRepository locationRepository,
            UserRepository userRepository,
            FamilyRepository familyRepository,
            FamilyMemberRepository familyMemberRepository) {

        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.familyMemberRepository = familyMemberRepository;
    }

    /*
     * CHILD:
     * Get my own location history.
     *
     * GET /api/location-history
     */
    @GetMapping
    public List<LocationHistoryResponse> getMyLocationHistory(
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        // Only children have location history.
        if (!"CHILD".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Location history is available only for children"
            );
        }

        return getHistoryForUser(userId);
    }

    /*
     * PARENT:
     * Get a child's location history.
     *
     * GET /api/location-history/child/{childUserId}
     */
    @GetMapping("/child/{childUserId}")
    public List<LocationHistoryResponse> getChildLocationHistory(
            @PathVariable Long childUserId,
            Authentication authentication) {

        Long parentUserId = getAuthenticatedUserId(authentication);

        User parent = userRepository.findById(parentUserId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Parent user not found"
                        )
                );

        User child = userRepository.findById(childUserId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Child user not found"
                        )
                );

        // Requester must actually be a parent.
        if (!"PARENT".equals(parent.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only parents can view a child's location history"
            );
        }

        // Target must actually be a child.
        if (!"CHILD".equals(child.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Location history is available only for children"
            );
        }

        // Parent and child must belong to the same active family.
        FamilyMember relationship =
                findSharedFamilyRelationship(parent, child);

        if (relationship == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Parent and child are not members of the same family"
            );
        }

        return getHistoryForUser(childUserId);
    }

    /*
     * Find an active family relationship between two users.
     */
    private FamilyMember findSharedFamilyRelationship(
            User parent,
            User child) {

        return familyRepository.findAll()
                .stream()

                // Find families where the parent is an ACTIVE member.
                .map(family ->
                        familyMemberRepository
                                .findByFamilyAndUser(family, parent)
                                .orElse(null)
                )
                .filter(member ->
                        member != null &&
                                "ACTIVE".equals(member.getStatus())
                )

                // Get the family from that membership.
                .map(FamilyMember::getFamily)

                // Check whether the child is also an ACTIVE member.
                .map(family ->
                        familyMemberRepository
                                .findByFamilyAndUser(family, child)
                                .orElse(null)
                )
                .filter(member ->
                        member != null &&
                                "ACTIVE".equals(member.getStatus())
                )

                .findFirst()
                .orElse(null);
    }

    /*
     * Load location history for a specific user.
     */
    private List<LocationHistoryResponse> getHistoryForUser(
            Long userId) {

        return locationRepository.findAll()
                .stream()
                .filter(location ->
                        location.getUserId().equals(userId))
                .sorted((a, b) ->
                        b.getTimestamp()
                                .compareTo(a.getTimestamp()))
                .map(location ->
                        new LocationHistoryResponse(
                                location.getId(),
                                location.getLatitude(),
                                location.getLongitude(),
                                location.getAccuracy(),
                                location.getTimestamp()
                        )
                )
                .toList();
    }

    /*
     * Get the user ID from the JWT.
     */
    private Long getAuthenticatedUserId(
            Authentication authentication) {

        try {
            return Long.valueOf(authentication.getName());

        } catch (Exception e) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user"
            );
        }
    }

    public record LocationHistoryResponse(
            Long id,
            Double latitude,
            Double longitude,
            Double accuracy,
            java.time.Instant timestamp
    ) {
    }
}