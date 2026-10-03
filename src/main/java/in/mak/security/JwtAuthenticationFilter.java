package in.mak.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        System.out.println("========================================");
        System.out.println("JWT FILTER");
        System.out.println("Request: " + request.getMethod() + " " + request.getRequestURI());
        System.out.println("Authorization header present: "
                + (authorization != null));

        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {

            System.out.println("JWT: NO BEARER TOKEN");
            System.out.println("========================================");

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7).trim();

        System.out.println("JWT: Bearer token received");
        System.out.println("JWT token length: " + token.length());

        try {

            Long userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);

            System.out.println("JWT SUCCESS");
            System.out.println("JWT userId: " + userId);
            System.out.println("JWT role: " + role);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId.toString(),
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + role
                                    )
                            )
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            System.out.println(
                    "SecurityContext authentication set successfully"
            );

        } catch (Exception e) {

            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("JWT VALIDATION FAILED");
            System.out.println("Exception type: "
                    + e.getClass().getName());
            System.out.println("Exception message: "
                    + e.getMessage());
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

            SecurityContextHolder.clearContext();
        }

        System.out.println("========================================");

        filterChain.doFilter(request, response);
    }
}