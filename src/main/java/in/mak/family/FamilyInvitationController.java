package in.mak.family;

import in.mak.user.User;
import in.mak.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/family-invitations")
public class FamilyInvitationController {

    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyInvitationRepository familyInvitationRepository;

    public FamilyInvitationController(
            UserRepository userRepository,
            FamilyRepository familyRepository,
            FamilyMemberRepository familyMemberRepository,
            FamilyInvitationRepository familyInvitationRepository
    ) {
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.familyInvitationRepository = familyInvitationRepository;
    }

    // =========================================================
    // CREATE INVITATION USING FAMILY ID
    // POST /api/family-invitations
    // =========================================================

    @PostMapping
    public Map<String, Object> createInvitation(
            @RequestBody CreateInvitationRequest request,
            Authentication authentication
    ) {

        Long parentUserId = getAuthenticatedUserId(authentication);

        User parent = userRepository.findById(parentUserId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Authenticated user not found"
                        )
                );

        // Only PARENT accounts can invite children
        if (!"PARENT".equalsIgnoreCase(parent.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only a parent can send an invitation"
            );
        }

        if (request == null || request.familyId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Family ID is required"
            );
        }

        Family family = familyRepository.findById(request.familyId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Family not found"
                        )
                );

        // Check that the authenticated parent belongs to this family
        FamilyMember parentMembership =
                familyMemberRepository
                        .findByFamilyAndUser(family, parent)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Parent does not belong to this family"
                                )
                        );

        // Only ACTIVE membership is required.
        if (!"ACTIVE".equalsIgnoreCase(parentMembership.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Parent is not an active member of this family"
            );
        }

        return createInvitation(family, parent, request.childEmail());
    }


    // =========================================================
    // CREATE INVITATION FOR LOGGED-IN PARENT'S FAMILY
    // POST /api/family-invitations/my-family
    // =========================================================

    @PostMapping("/my-family")
    public Map<String, Object> createInvitationForMyFamily(
            @RequestBody CreateMyFamilyInvitationRequest request,
            Authentication authentication
    ) {

        // Get the logged-in user from JWT
        Long parentUserId = getAuthenticatedUserId(authentication);

        User parent = userRepository.findById(parentUserId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Authenticated user not found"
                        )
                );

        // User must be a parent
        if (parent.getRole() == null ||
                !"PARENT".equalsIgnoreCase(parent.getRole().trim())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only a parent can send an invitation"
            );
        }

        // Request must contain child email
        if (request == null ||
                request.childEmail() == null ||
                request.childEmail().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Child email is required"
            );
        }

        /*
         * Find the ACTIVE family membership of this logged-in parent.
         *
         * Example:
         * User 3 -> ACTIVE -> Family 1
         */
        FamilyMember parentMembership =
                familyMemberRepository
                        .findByUserAndStatus(parent, "ACTIVE")
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Parent is not an active member of any family"
                                )
                        );

        Family family = parentMembership.getFamily();

        if (family == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Parent family not found"
            );
        }

        // Create the invitation using the common logic
        return createInvitation(
                family,
                parent,
                request.childEmail()
        );
    }


    // =========================================================
    // COMMON INVITATION CREATION LOGIC
    // =========================================================

    private Map<String, Object> createInvitation(
            Family family,
            User parent,
            String childEmail
    ) {

        if (childEmail == null || childEmail.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Child email is required"
            );
        }

        String email = childEmail.trim().toLowerCase();

        User child = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Child account not found"
                        )
                );

        // The invited account must actually be a CHILD.
        if (!"CHILD".equalsIgnoreCase(child.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The invited account must be a CHILD"
            );
        }

        // Child cannot be invited if already a family member.
        if (familyMemberRepository.existsByFamilyAndUser(
                family,
                child
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Child is already a member of this family"
            );
        }

        // Prevent duplicate pending invitations.
        List<FamilyInvitation> pendingInvitations =
                familyInvitationRepository
                        .findByInvitedUserAndStatus(child, "PENDING");

        boolean alreadyPending =
                pendingInvitations.stream()
                        .anyMatch(invitation ->
                                invitation.getFamily().getId()
                                        .equals(family.getId())
                        );

        if (alreadyPending) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A pending invitation already exists for this child"
            );
        }

        FamilyInvitation invitation =
                new FamilyInvitation(
                        family,
                        parent,
                        child
                );

        FamilyInvitation savedInvitation =
                familyInvitationRepository.save(invitation);

        return Map.of(
                "message", "Invitation created",
                "invitationId", savedInvitation.getId(),
                "familyId", family.getId(),
                "invitedEmail", child.getEmail(),
                "status", savedInvitation.getStatus()
        );
    }


    // =========================================================
    // ACCEPT INVITATION
    // POST /api/family-invitations/{invitationId}/accept
    // =========================================================

    @PostMapping("/{invitationId}/accept")
    public Map<String, Object> acceptInvitation(
            @PathVariable Long invitationId,
            Authentication authentication
    ) {

        Long childUserId = getAuthenticatedUserId(authentication);

        User child = userRepository.findById(childUserId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Authenticated user not found"
                        )
                );

        if (!"CHILD".equalsIgnoreCase(child.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only a child account can accept an invitation"
            );
        }

        FamilyInvitation invitation =
                familyInvitationRepository
                        .findByIdAndStatus(
                                invitationId,
                                "PENDING"
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Pending invitation not found"
                                )
                        );

        // Make sure this invitation belongs to the logged-in child.
        if (!child.getId().equals(
                invitation.getInvitedUser().getId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This invitation belongs to another user"
            );
        }

        // Prevent duplicate membership.
        if (familyMemberRepository.existsByFamilyAndUser(
                invitation.getFamily(),
                child
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Child is already a member of this family"
            );
        }

        FamilyMember childMember =
                new FamilyMember(
                        invitation.getFamily(),
                        child,
                        "CHILD",
                        "ACTIVE"
                );

        familyMemberRepository.save(childMember);

        invitation.setStatus("ACCEPTED");
        familyInvitationRepository.save(invitation);

        return Map.of(
                "message", "Invitation accepted",
                "familyId", invitation.getFamily().getId(),
                "childUserId", child.getId(),
                "role", "CHILD",
                "status", "ACTIVE"
        );
    }


    // =========================================================
    // GET PENDING INVITATIONS
    // GET /api/family-invitations/pending
    // =========================================================

    @GetMapping("/pending")
    public List<Map<String, Object>> getPendingInvitations(
            Authentication authentication
    ) {

        Long childUserId = getAuthenticatedUserId(authentication);

        User child = userRepository.findById(childUserId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        if (!"CHILD".equalsIgnoreCase(child.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only a child account can view pending invitations"
            );
        }

        return familyInvitationRepository
                .findByInvitedUserAndStatus(
                        child,
                        "PENDING"
                )
                .stream()
                .map(invitation ->
                        Map.<String, Object>of(
                                "invitationId",
                                invitation.getId(),

                                "familyId",
                                invitation.getFamily().getId(),

                                "familyName",
                                invitation.getFamily().getName(),

                                "invitedByUserId",
                                invitation.getInvitedBy().getId(),

                                "invitedByName",
                                invitation.getInvitedBy().getName(),

                                "status",
                                invitation.getStatus(),

                                "createdAt",
                                invitation.getCreatedAt()
                        )
                )
                .toList();
    }


    // =========================================================
    // AUTHENTICATED USER ID
    // =========================================================

    private Long getAuthenticatedUserId(
            Authentication authentication
    ) {

        if (authentication == null ||
                authentication.getName() == null ||
                authentication.getName().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user"
            );
        }

        try {

            return Long.valueOf(
                    authentication.getName()
            );

        } catch (NumberFormatException e) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user"
            );
        }
    }


    // =========================================================
    // REQUEST RECORDS
    // =========================================================

    public record CreateInvitationRequest(
            Long familyId,
            String childEmail
    ) {
    }


    public record CreateMyFamilyInvitationRequest(
            String childEmail
    ) {
    }
}