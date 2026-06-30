package sc.liste.noel.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import sc.liste.noel.common.service.JwtAuthFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Front et back sont servis depuis la MÊME origine (le SPA est embarqué
                // dans le back) : le navigateur n'émet plus de requête cross-origin,
                // la configuration CORS est donc devenue inutile et a été retirée.
                .csrf(AbstractHttpConfigurer::disable)
                // Pas de session côté serveur : le JWT suffit
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                ).exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"messageRetour\":\"Non autorisé\",\"codeRetour\":1}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"messageRetour\":\"Accès refusé\",\"codeRetour\":1}");
                        })
                )
                // Définition des routes publiques et protégées.
                // IMPORTANT : l'ordre compte (premier matcher qui correspond = gagnant).
                // On place donc les règles /api/** AVANT la couche statique/SPA pour
                // ne PAS affaiblir la protection des endpoints déjà sécurisés.
                .authorizeHttpRequests(auth -> auth
                        // --- 1) API ---
                        // Consultation publique d'une liste précise (lecture seule)
                        .requestMatchers(HttpMethod.GET, "/api/liste/*").permitAll()
                        // Tout le reste de /api/liste/** nécessite d'être connecté
                        .requestMatchers("/api/liste/**").authenticated()
                        // Endpoints compte + technique publics (comportement inchangé)
                        .requestMatchers(
                                "/api/compte/**",
                                "/error",
                                "/health",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // --- 2) SPA React (servi par le back, même origine) ---
                        // Tout GET restant correspond au SPA : la coquille index.html,
                        // les ressources statiques (/assets/**, JS, CSS, favicon...) ET
                        // les routes React Router (/ma-liste, /profil...) qui seront
                        // forwardées vers index.html par SpaController.
                        // Ces ressources DOIVENT être publiques : la sécurité porte sur
                        // les données (/api/**, déjà filtrées ci-dessus), pas sur l'app.
                        // Les routes /api/liste/** protégées sont déjà capturées plus haut,
                        // donc ce permitAll GET ne les affaiblit pas.
                        .requestMatchers(HttpMethod.GET, "/**").permitAll()

                        // --- 3) Tout le reste (POST/PUT/DELETE hors API publique) ---
                        .anyRequest().authenticated()
                )

                // On branche notre filtre JWT avant le filtre d'authentification de Spring
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}