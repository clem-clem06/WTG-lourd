package org.example.wtg.security;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.example.wtg.entities.User;
import org.example.wtg.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires pour la logique d'accès JavaFX :
 * seuls ROLE_ADMIN et ROLE_COMPTABLE peuvent se connecter.
 *
 * On utilise Mockito pour simuler UserRepository (pas d'accès BDD réel),
 * et un ListAppender Logback pour vérifier que l'exception JSON est bien loguée.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        // Branche un appender "mémoire" sur le logger de la classe testée
        // → tout log émis pendant le test sera accessible via logAppender.list
        Logger logger = (Logger) LoggerFactory.getLogger(CustomUserDetailsService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    // ─────────────────────────────────────────────────────────
    //  Cas OK : rôles staff → authentification réussie
    // ─────────────────────────────────────────────────────────

    @Test
    void loadUser_avecRoleAdmin_retourneUserDetails() {
        User user = buildUser("admin@wtg.fr", "[\"ROLE_ADMIN\"]", "hash");
        when(userRepository.findByEmail("admin@wtg.fr")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin@wtg.fr");

        assertThat(details.getUsername()).isEqualTo("admin@wtg.fr");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUser_avecRoleComptable_retourneUserDetails() {
        User user = buildUser("compta@wtg.fr", "[\"ROLE_COMPTABLE\"]", "hash");
        when(userRepository.findByEmail("compta@wtg.fr")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("compta@wtg.fr");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_COMPTABLE");
    }

    // ─────────────────────────────────────────────────────────
    //  Cas KO : utilisateur sans droit d'accès (client classique)
    // ─────────────────────────────────────────────────────────

    @Test
    void loadUser_avecRoleClient_lanceDisabledException() {
        User user = buildUser("client@htmail.fr", "[\"ROLE_CLIENT\"]", "hash");
        when(userRepository.findByEmail("client@htmail.fr")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.loadUserByUsername("client@htmail.fr"))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("réservée au personnel");
    }

    // ─────────────────────────────────────────────────────────
    //  Cas KO : email inexistant
    // ─────────────────────────────────────────────────────────

    @Test
    void loadUser_avecEmailInconnu_lanceUsernameNotFoundException() {
        when(userRepository.findByEmail("inconnu@wtg.fr")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("inconnu@wtg.fr"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("inconnu@wtg.fr");
    }

    // ─────────────────────────────────────────────────────────
    //  Cas KO : JSON de rôles corrompu → WARN logué + rejet
    // ─────────────────────────────────────────────────────────

    @Test
    void loadUser_avecRolesJsonInvalide_logueWarnEtLanceDisabled() {
        User user = buildUser("corrompu@wtg.fr", "pas du json", "hash");
        when(userRepository.findByEmail("corrompu@wtg.fr")).thenReturn(Optional.of(user));

        // Le fallback ROLE_USER est rejeté → DisabledException
        assertThatThrownBy(() -> service.loadUserByUsername("corrompu@wtg.fr"))
                .isInstanceOf(DisabledException.class);

        // Et surtout : le WARN contient bien l'email et le JSON fautif
        assertThat(logAppender.list)
                .anyMatch(event -> event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains("corrompu@wtg.fr")
                        && event.getFormattedMessage().contains("pas du json"));
    }

    // ─────────────────────────────────────────────────────────
    //  Helper
    // ─────────────────────────────────────────────────────────

    private User buildUser(String email, String rolesJson, String password) {
        User u = new User();
        u.setEmail(email);
        u.setRoles(rolesJson);
        u.setPassword(password);
        return u;
    }
}
