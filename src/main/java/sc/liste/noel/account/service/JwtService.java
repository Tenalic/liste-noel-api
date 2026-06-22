package sc.liste.noel.account.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    // Injected from application.properties → never hard-coded in the code
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationInSeconds;

    // --- Generate the signing key ---
    // Recomputed from the secret every time (stateless)
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // --- Generate a JWT token ---
    // Called after a successful login
    // The "subject" = user identifier (here the email)
    public String generateToken(String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationInSeconds * 1000);

        return Jwts.builder()
                .subject(email)                     // who the user is
                .issuedAt(now)                      // when the token was created
                .expiration(expiration)             // when it expires
                .signWith(getSigningKey())          // sign with our secret key
                .compact();
    }

    // --- Extract the email from a token ---
    // Used in protected endpoints to know who makes the request
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    // --- Check whether a token is valid ---
    // Returns true if the token is properly signed AND not expired
    public boolean isValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            // Malformed token, invalid signature, or expired
            return false;
        }
    }

    // --- Extract all the data from the token ---
    // Private method used by the two methods above
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())         // verify the signature
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
