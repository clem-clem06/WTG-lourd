package org.example.wtg.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Permet d'afficher les messages d'erreur précis (DisabledException, etc.)
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Désactive CSRF (simplifie les appels API et les formulaires en dev)
            .csrf(AbstractHttpConfigurer::disable)

            // ── Règles d'accès ──────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // La page de login est accessible à tout le monde (sinon boucle infinie)
                .requestMatchers("/login", "/css/**", "/js/**").permitAll()

                // Routes admin : réservées aux administrateurs
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Routes comptable : accessibles par Comptable ET Admin
                .requestMatchers("/comptable/**").hasAnyRole("COMPTABLE", "ADMIN")

                // Tout le reste nécessite d'être connecté
                .anyRequest().authenticated()
            )

            // ── Page de connexion personnalisée ─────────────────────────
            .formLogin(form -> form
                .loginPage("/login")              // notre page HTML de login
                .loginProcessingUrl("/login")     // Spring traite le POST sur cette URL
                .defaultSuccessUrl("/dashboard", true) // redirection après connexion
                .failureUrl("/login?erreur=true") // redirection si login échoue
                .usernameParameter("email")       // nom du champ email dans le formulaire
                .passwordParameter("password")    // nom du champ password dans le formulaire
                .permitAll()
            )

            // ── Déconnexion ─────────────────────────────────────────────
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?deconnexion=true")
                .permitAll()
            )

            .authenticationProvider(authenticationProvider());

        return http.build();
    }
}
