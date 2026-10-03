package in.mak.user;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean shareLocation = true;

    @Column(nullable = false)
    private boolean saveLocationHistory = false;

    public User() {
    }

    public User(
            String name,
            String email,
            String passwordHash,
            String role
    ) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;

        // Default privacy rules:
        // Parent -> current location only, no history.
        // Child  -> location sharing enabled, history can be enabled later.
        this.shareLocation = true;
        this.saveLocationHistory = false;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isShareLocation() {
        return shareLocation;
    }

    public void setShareLocation(boolean shareLocation) {
        this.shareLocation = shareLocation;
    }

    public boolean isSaveLocationHistory() {
        return saveLocationHistory;
    }

    public void setSaveLocationHistory(boolean saveLocationHistory) {
        this.saveLocationHistory = saveLocationHistory;
    }
}