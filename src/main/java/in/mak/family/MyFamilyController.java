package in.mak.family;

import in.mak.user.User;
import in.mak.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/my-family")
public class MyFamilyController {

    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;

    public MyFamilyController(
            UserRepository userRepository,
            FamilyRepository familyRepository,
            FamilyMemberRepository familyMemberRepository
    ) {
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.familyMemberRepository = familyMemberRepository;
    }

    @GetMapping("/members")
    public List<FamilyMemberResponse> getMyFamilyMembers(
            Authentication authentication
    ) {

        Long userId =
                Long.valueOf(authentication.getName());

        User currentUser =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "User not found"
                                )
                        );

        Family family =
                familyRepository.findAll()
                        .stream()
                        .filter(f ->
                                familyMemberRepository
                                        .findByFamilyAndUser(
                                                f,
                                                currentUser
                                        )
                                        .map(member ->
                                                "ACTIVE".equals(
                                                        member.getStatus()
                                                )
                                        )
                                        .orElse(false)
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "User does not belong to an active family"
                                )
                        );

        return familyMemberRepository
                .findByFamily(family)
                .stream()
                .filter(member ->
                        "ACTIVE".equals(member.getStatus())
                )
                .map(member -> {

                    User user = member.getUser();

                    return new FamilyMemberResponse(
                            user.getId(),
                            user.getName(),
                            user.getEmail(),
                            user.getRole(),
                            member.getStatus()
                    );
                })
                .toList();
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