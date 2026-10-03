package in.mak.family;

import in.mak.user.User;
import in.mak.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/families")
public class FamilyController {

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;

    public FamilyController(
            FamilyRepository familyRepository,
            FamilyMemberRepository familyMemberRepository,
            UserRepository userRepository
    ) {
        this.familyRepository = familyRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public Map<String, Object> createFamily(
            @RequestBody CreateFamilyRequest request,
            Authentication authentication
    ) {

        Long authenticatedUserId;

        try {
            authenticatedUserId =
                    Long.valueOf(authentication.getName());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user"
            );
        }

        User owner = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Authenticated user not found"
                ));

        if (!"PARENT".equals(owner.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only a parent can create a family"
            );
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Family name is required"
            );
        }

        Family family =
                familyRepository.save(
                        new Family(request.name().trim())
                );

        FamilyMember ownerMember =
                new FamilyMember(
                        family,
                        owner,
                        "PARENT",
                        "ACTIVE"
                );

        familyMemberRepository.save(ownerMember);

        return Map.of(
                "message", "Family created",
                "familyId", family.getId(),
                "name", family.getName(),
                "ownerUserId", owner.getId(),
                "role", "PARENT"
        );
    }

    @GetMapping("/{familyId}/members")
    public java.util.List<FamilyMemberResponse> getFamilyMembers(
            @PathVariable Long familyId,
            Authentication authentication
    ) {

        Long authenticatedUserId =
                Long.valueOf(authentication.getName());

        User requester =
                userRepository.findById(authenticatedUserId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Authenticated user not found"
                                )
                        );

        Family family =
                familyRepository.findById(familyId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Family not found"
                                )
                        );

        if (familyMemberRepository
                .findByFamilyAndUser(family, requester)
                .filter(member ->
                        "ACTIVE".equals(member.getStatus())
                )
                .isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not an active member of this family"
            );
        }

        return familyMemberRepository
                .findByFamily(family)
                .stream()
                .filter(member ->
                        "ACTIVE".equals(member.getStatus())
                )
                .map(member -> new FamilyMemberResponse(
                        member.getUser().getId(),
                        member.getUser().getName(),
                        member.getUser().getEmail(),
                        member.getMemberRole(),
                        member.getStatus()
                ))
                .toList();
    }

    public record CreateFamilyRequest(
            String name
    ) {
    }

    public record FamilyMemberResponse(
            Long userId,
            String name,
            String email,
            String role,
            String status
    ) {
    }
}