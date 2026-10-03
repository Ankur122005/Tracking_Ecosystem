package in.mak.family;

import in.mak.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    List<FamilyMember> findByFamily(Family family);

    Optional<FamilyMember> findByFamilyAndUser(
            Family family,
            User user
    );

    Optional<FamilyMember> findByUserAndStatus(
            User user,
            String status
    );

    boolean existsByFamilyAndUser(
            Family family,
            User user
    );
}