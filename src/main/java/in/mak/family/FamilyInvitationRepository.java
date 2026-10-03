package in.mak.family;

import in.mak.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilyInvitationRepository
        extends JpaRepository<FamilyInvitation, Long> {

    List<FamilyInvitation> findByInvitedUserAndStatus(
            User invitedUser,
            String status
    );

    Optional<FamilyInvitation> findByIdAndStatus(
            Long id,
            String status
    );
}