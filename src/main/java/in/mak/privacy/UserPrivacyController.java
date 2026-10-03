package in.mak.privacy;

import in.mak.user.User;
import in.mak.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/privacy")
public class UserPrivacyController {

    private final UserRepository userRepository;

    public UserPrivacyController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /*
     * GET current privacy settings
     *
     * GET /api/privacy/me
     */
    @GetMapping("/me")
    public Map<String, Object> getMyPrivacy(
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        return Map.of(
                "userId", user.getId(),
                "shareLocation", user.isShareLocation(),
                "saveLocationHistory", user.isSaveLocationHistory()
        );
    }

    /*
     * UPDATE location-sharing setting
     *
     * PATCH /api/privacy/me
     *
     * Example:
     *
     * {
     *   "shareLocation": false
     * }
     */
    @PatchMapping("/me")
    public Map<String, Object> updateMyPrivacy(
            @RequestBody PrivacyUpdateRequest request,
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        if (request.shareLocation() != null) {
            user.setShareLocation(
                    request.shareLocation()
            );
        }

        if (request.saveLocationHistory() != null) {

            /*
             * Only allow children to control history.
             *
             * Parents currently do not store location history.
             */
            if ("CHILD".equals(user.getRole())) {

                user.setSaveLocationHistory(
                        request.saveLocationHistory()
                );

            } else if (request.saveLocationHistory()) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Parents do not have location history"
                );
            }
        }

        userRepository.save(user);

        return Map.of(
                "message", "Privacy settings updated",
                "userId", user.getId(),
                "shareLocation", user.isShareLocation(),
                "saveLocationHistory",
                user.isSaveLocationHistory()
        );
    }

    private Long getAuthenticatedUserId(
            Authentication authentication) {

        try {

            return Long.valueOf(
                    authentication.getName()
            );

        } catch (Exception e) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user"
            );
        }
    }

    public record PrivacyUpdateRequest(
            Boolean shareLocation,
            Boolean saveLocationHistory
    ) {
    }
}