package sc.liste.noel.common.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sc.liste.noel.account.service.JwtService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Look for the "auth-token" cookie in the request
        Optional<String> token = extractTokenFromCookie(request);

        if (token.isPresent() && jwtService.isValid(token.get())) {
            // Valid token → identify the user in the Spring Security context
            String email = jwtService.extractEmail(token.get());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(email, null, List.of());

            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        // Missing or invalid token → SecurityContext stays empty
        // Spring Security will deny access to protected endpoints (401)

        filterChain.doFilter(request, response);
    }

    // Looks for the "auth-token" cookie among all the request cookies
    private Optional<String> extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "auth-token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
