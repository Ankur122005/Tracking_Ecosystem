package in.mak.location;

import in.mak.user.User;
import in.mak.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationRepository locationRepository;
    private final CurrentLocationRepository currentLocationRepository;
    private final UserRepository userRepository;

    public LocationController(
            LocationRepository locationRepository,
            CurrentLocationRepository currentLocationRepository,
            UserRepository userRepository
    ) {
        this.locationRepository = locationRepository;
        this.currentLocationRepository = currentLocationRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public Location createLocation(
            @RequestBody LocationRequest request,
            Authentication authentication
    ) {

        Long userId = Long.valueOf(authentication.getName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        Instant now = Instant.now();

        // Always update the current location.
        CurrentLocation currentLocation =
                currentLocationRepository.findById(userId)
                        .orElse(new CurrentLocation());

        currentLocation.setUserId(userId);
        currentLocation.setLatitude(request.latitude());
        currentLocation.setLongitude(request.longitude());
        currentLocation.setAccuracy(request.accuracy());
        currentLocation.setUpdatedAt(now);

        currentLocationRepository.save(currentLocation);

        /*
         * Only users whose account allows history
         * get a row in the locations history table.
         *
         * Parent -> false
         * Child  -> true
         */
        if (user.isSaveLocationHistory()) {

            Location history = new Location(
                    userId,
                    request.latitude(),
                    request.longitude(),
                    request.accuracy(),
                    now
            );

            locationRepository.save(history);

            return history;
        }

        // Parent history is intentionally not stored.
        return new Location(
                userId,
                request.latitude(),
                request.longitude(),
                request.accuracy(),
                now
        );
    }

    public record LocationRequest(
            Double latitude,
            Double longitude,
            Double accuracy
    ) {
    }
}