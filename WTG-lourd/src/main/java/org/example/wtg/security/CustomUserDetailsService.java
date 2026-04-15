package org.example.wtg.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.wtg.entities.User;
import org.example.wtg.repositories.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // 1. Cherche l'utilisateur en BDD
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + email));

        // 2. Parse le JSON des rôles  ex: ["ROLE_ADMIN"] → List["ROLE_ADMIN"]
        List<String> rolesList;
        try {
            rolesList = objectMapper.readValue(user.getRoles(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            rolesList = List.of("ROLE_USER");
        }

        // 3. Vérifie que l'utilisateur a le droit d'accéder à l'application interne
        //    Seuls ROLE_ADMIN et ROLE_COMPTABLE peuvent se connecter ici
        boolean aAcces = rolesList.stream()
                .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_COMPTABLE"));

        if (!aAcces) {
            // DisabledException = "compte désactivé" → Spring Security affiche
            // le message d'erreur "User is disabled" sur la page de login
            throw new DisabledException("Accès refusé : cette application est réservée au personnel.");
        }

        // 4. Convertit les rôles en objets Spring Security
        List<SimpleGrantedAuthority> authorities = rolesList.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        // 5. Retourne l'objet que Spring Security utilise pour l'authentification
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}
