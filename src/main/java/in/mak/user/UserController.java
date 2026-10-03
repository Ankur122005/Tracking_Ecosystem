package in.mak.user;

import in.mak.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public User register(@RequestBody RegisterRequest request) {

        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Name is required"
            );
        }

        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required"
            );
        }

        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password is required"
            );
        }

        if (request.role() == null ||
                (!request.role().equalsIgnoreCase("PARENT")
                        && !request.role().equalsIgnoreCase("CHILD"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role must be PARENT or CHILD"
            );
        }

        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered"
            );
        }

        // Password is hashed with BCrypt before being stored.
        String passwordHash =
                passwordEncoder.encode(request.password());

        User user = new User(
                request.name().trim(),
                email,
                passwordHash,
                request.role().trim().toUpperCase()
        );

        // Parents do not have location history stored.
        if (user.getRole().equals("PARENT")) {
            user.setSaveLocationHistory(false);
        }

        // Children can have location history stored.
        if (user.getRole().equals("CHILD")) {
            user.setSaveLocationHistory(true);
        }

        return userRepository.save(user);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {

        System.out.println("========== ANDROID/CLIENT LOGIN TEST ==========");
        System.out.println("Email received: [" + request.email() + "]");
        System.out.println("Password received length: " +
                (request.password() == null ? "NULL" : request.password().length()));

        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required"
            );
        }

        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password is required"
            );
        }

        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    System.out.println("USER NOT FOUND: " + email);
                    return new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Invalid email or password"
                    );
                });

        System.out.println("USER FOUND: id=" + user.getId()
                + ", name=" + user.getName()
                + ", role=" + user.getRole());

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash()
                );

        System.out.println("PASSWORD MATCH: " + passwordMatches);
        System.out.println("==============================================");

        if (!passwordMatches) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        return Map.of(
                "message", "Login successful",
                "token", token,
                "userId", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole()
        );
    }

    public record RegisterRequest(
            String name,
            String email,
            String password,
            String role
    ) {
    }

    public record LoginRequest(
            String email,
            String password
    ) {
    }
}