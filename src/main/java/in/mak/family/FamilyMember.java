package in.mak.family;

import in.mak.user.User;
import jakarta.persistence.*;

@Entity
@Table(
        name = "family_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"family_id", "user_id"}
                )
        }
)
public class FamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String memberRole;

    @Column(nullable = false)
    private String status;

    public FamilyMember() {
    }

    public FamilyMember(
            Family family,
            User user,
            String memberRole,
            String status
    ) {
        this.family = family;
        this.user = user;
        this.memberRole = memberRole;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Family getFamily() {
        return family;
    }

    public void setFamily(Family family) {
        this.family = family;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(String memberRole) {
        this.memberRole = memberRole;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}