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

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/current-locations")
public class CurrentLocationController {

    private final CurrentLocationRepository currentLocationRepository;
    private final UserRepository userRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyRepository familyRepository;

    public CurrentLocationController(
            CurrentLocationRepository currentLocationRepository,
            UserRepository userRepository,
            FamilyMemberRepository familyMemberRepository,
            FamilyRepository familyRepository
    ) {
        this.currentLocationRepository = currentLocationRepository;
        this.userRepository = userRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.familyRepository = familyRepository;
    }

    @PostMapping("/{userId}")
    public CurrentLocation updateCurrentLocation(
            @PathVariable Long userId,
            @RequestBody CurrentLocationRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (request.latitude() == null || request.longitude() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Latitude and longitude are required"
            );
        }

        CurrentLocation currentLocation =
                currentLocationRepository.findById(userId)
                        .orElseGet(CurrentLocation::new);

        currentLocation.setUserId(user.getId());
        currentLocation.setLatitude(request.latitude());
        currentLocation.setLongitude(request.longitude());
        currentLocation.setAccuracy(request.accuracy());
        currentLocation.setUpdatedAt(Instant.now());

        return currentLocationRepository.save(currentLocation);
    }

    @GetMapping("/family/{targetUserId}")
    public Map<String, Object> getFamilyMemberCurrentLocation(
            @PathVariable Long targetUserId,
            Authentication authentication
    ) {

        Long requesterUserId;

        try {
            requesterUserId = Long.valueOf(authentication.getName());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user"
            );
        }

        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Requester user not found"
                ));

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Target user not found"
                ));

        FamilyMember relationship =
                findSharedFamilyRelationship(requester, target);

        if (relationship == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Users are not members of the same family"
            );
        }

        if (!target.isShareLocation()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Target user is not sharing their location"
            );
        }

        CurrentLocation location =
                currentLocationRepository.findById(targetUserId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Current location not available"
                        ));

        return Map.of(
                "userId", target.getId(),
                "name", target.getName(),
                "role", target.getRole(),
                "latitude", location.getLatitude(),
                "longitude", location.getLongitude(),
                "accuracy", location.getAccuracy(),
                "updatedAt", location.getUpdatedAt()
        );
    }

    private FamilyMember findSharedFamilyRelationship(
            User requester,
            User target
    ) {

        return familyRepository.findAll()
                .stream()
                .map(family ->
                        familyMemberRepository.findByFamilyAndUser(
                                family,
                                requester
                        ).orElse(null)
                )
                .filter(member -> member != null)
                .filter(member ->
                        "ACTIVE".equals(member.getStatus())
                )
                .map(FamilyMember::getFamily)
                .map(family ->
                        familyMemberRepository.findByFamilyAndUser(
                                family,
                                target
                        ).orElse(null)
                )
                .filter(member -> member != null)
                .filter(member ->
                        "ACTIVE".equals(member.getStatus())
                )
                .findFirst()
                .orElse(null);
    }

    public record CurrentLocationRequest(
            Double latitude,
            Double longitude,
            Double accuracy
    ) {
    }
}