package org.example.wtg.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Plus de @EnableWebSecurity ni de SecurityFilterChain
// → on n'a plus de serveur HTTP, juste les beans d'authentification
@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    // Argon2id : même algorithme que Symfony (security.yaml : algorithm: argon2id)
    // Paramètres identiques : memory=65536 KB, iterations=4, parallelism=1
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 4);
    }

    // AuthenticationManager : vérifie email + mot de passe
    // On l'utilisera dans le LoginController JavaFX
    @Bean
    public AuthenticationManager authenticationManager() {
        // NB : le constructeur DaoAuthenticationProvider(UserDetailsService) n'existe
        // qu'à partir de Spring Security 6.3. Spring Boot 3.2.4 embarque la 6.2.x,
        // donc on garde le style "setters" qui fonctionne dans toutes les versions.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false); // affiche les vrais messages d'erreur
        return new ProviderManager(provider);
    }
}
