package org.example.wtg.services;

import org.example.wtg.entities.User;
import org.example.wtg.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Couche métier pour la gestion des comptes ADMIN / COMPTABLE.
 *
 * Règle de sécurité : on NE touche JAMAIS aux comptes ROLE_CLIENT (ils sont
 * gérés par le site Symfony côté client). Toute tentative est rejetée.
 */
@Service
public class UserManagementService {

    private static final Logger log = LoggerFactory.getLogger(UserManagementService.class);

    // Rôles autorisés pour la création via l'interface admin
    private static final Set<String> ROLES_AUTORISES = Set.of("ROLE_ADMIN", "ROLE_COMPTABLE");

    // Validation email basique (même principe que la contrainte Symfony Assert\Email)
    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final int PASSWORD_MIN = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ─────────────────────────────────────────────────────────
    //  LISTER
    // ─────────────────────────────────────────────────────────

    /**
     * Renvoie uniquement le personnel (admin + comptable).
     * Les clients ne sont PAS inclus.
     */
    public List<User> listerPersonnel() {
        return userRepository.findAllPersonnel();
    }

    // ─────────────────────────────────────────────────────────
    //  CRÉER
    // ─────────────────────────────────────────────────────────

    /**
     * Crée un nouveau compte de personnel.
     *
     * @param email          doit être un email valide et unique
     * @param motDePasseBrut minimum 8 caractères, sera hashé en BCrypt
     * @param role           "ROLE_ADMIN" ou "ROLE_COMPTABLE" uniquement
     * @return l'utilisateur créé (avec son ID généré)
     * @throws IllegalArgumentException si une validation échoue
     * @throws IllegalStateException    si l'email est déjà pris
     */
    public User creerPersonnel(String email, String motDePasseBrut, String role) {

        // 1. Validations d'entrée
        if (role == null || !ROLES_AUTORISES.contains(role)) {
            throw new IllegalArgumentException("Rôle invalide. Seuls ADMIN et COMPTABLE sont autorisés.");
        }
        if (email == null || email.isBlank() || !EMAIL_REGEX.matcher(email).matches()) {
            throw new IllegalArgumentException("Adresse email invalide.");
        }
        if (motDePasseBrut == null || motDePasseBrut.length() < PASSWORD_MIN) {
            throw new IllegalArgumentException(
                    "Mot de passe trop court (minimum " + PASSWORD_MIN + " caractères).");
        }

        // 2. Vérifie l'unicité
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email déjà utilisé : " + email);
        }

        // 3. Construit et sauvegarde
        User user = new User();
        user.setEmail(email);
        // Symfony stocke les rôles dans un champ JSON (colonne `roles`).
        // Format attendu : ["ROLE_ADMIN"]  ← un tableau JSON d'une seule chaîne.
        user.setRoles("[\"" + role + "\"]");
        user.setPassword(passwordEncoder.encode(motDePasseBrut));

        User saved = userRepository.save(user);
        log.info("Création compte staff {} (rôle={}, id={})", email, role, saved.getId());
        return saved;
    }

    // ─────────────────────────────────────────────────────────
    //  MODIFIER
    // ─────────────────────────────────────────────────────────

    /**
     * Modifie un compte existant.
     * Le mot de passe est optionnel : s'il est null ou vide, il n'est pas changé.
     * Le rôle peut passer entre ADMIN et COMPTABLE, mais pas vers CLIENT.
     *
     * @param id                    identifiant du user à modifier
     * @param email                 nouvel email (ou l'ancien si on ne change pas)
     * @param motDePasseOptionnel   null/vide = garder le mot de passe actuel
     * @param role                  "ROLE_ADMIN" ou "ROLE_COMPTABLE"
     * @return l'utilisateur mis à jour
     */
    public User modifierPersonnel(Integer id, String email, String motDePasseOptionnel, String role) {
        if (id == null) {
            throw new IllegalArgumentException("Identifiant manquant.");
        }
        if (role == null || !ROLES_AUTORISES.contains(role)) {
            throw new IllegalArgumentException("Rôle invalide. Seuls ADMIN et COMPTABLE sont autorisés.");
        }
        if (email == null || email.isBlank() || !EMAIL_REGEX.matcher(email).matches()) {
            throw new IllegalArgumentException("Adresse email invalide.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable (id=" + id + ")"));

        // Même protection que pour la suppression : pas touche aux clients
        String rolesActuels = user.getRoles() == null ? "" : user.getRoles();
        if (rolesActuels.contains("ROLE_CLIENT")) {
            throw new IllegalStateException("Impossible de modifier un compte client via cette interface.");
        }

        // Si l'email change, il doit être unique
        if (!email.equalsIgnoreCase(user.getEmail())
                && userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email déjà utilisé : " + email);
        }

        user.setEmail(email);
        user.setRoles("[\"" + role + "\"]");

        // Mot de passe optionnel : on ne le change QUE s'il a été saisi
        if (motDePasseOptionnel != null && !motDePasseOptionnel.isEmpty()) {
            if (motDePasseOptionnel.length() < PASSWORD_MIN) {
                throw new IllegalArgumentException(
                        "Mot de passe trop court (minimum " + PASSWORD_MIN + " caractères).");
            }
            user.setPassword(passwordEncoder.encode(motDePasseOptionnel));
        }

        User saved = userRepository.save(user);
        log.info("Modification compte staff id={} email={} rôle={}", id, email, role);
        return saved;
    }

    // ─────────────────────────────────────────────────────────
    //  SUPPRIMER
    // ─────────────────────────────────────────────────────────

    /**
     * Supprime un compte de personnel.
     * Refuse si :
     *  - l'id n'existe pas,
     *  - le compte est un ROLE_CLIENT (interdiction absolue),
     *  - l'admin essaie de se supprimer lui-même.
     */
    public void supprimerPersonnel(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Identifiant manquant.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable (id=" + id + ")"));

        // Sécurité : on n'autorise la suppression QUE sur le personnel.
        // Si un client a été passé ici par erreur ou malveillance, on refuse.
        String rolesJson = user.getRoles() == null ? "" : user.getRoles();
        if (rolesJson.contains("ROLE_CLIENT")) {
            throw new IllegalStateException("Impossible de supprimer un compte client via cette interface.");
        }
        if (!rolesJson.contains("ROLE_ADMIN") && !rolesJson.contains("ROLE_COMPTABLE")) {
            throw new IllegalStateException("Ce compte n'appartient pas au personnel.");
        }

        // Protection : l'admin connecté ne peut pas supprimer son propre compte
        // (sinon il se déconnecterait lui-même de force).
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && user.getEmail().equalsIgnoreCase(auth.getName())) {
            throw new IllegalStateException("Vous ne pouvez pas vous supprimer vous-même.");
        }

        userRepository.deleteById(id);
        log.info("Suppression compte staff id={} email={}", id, user.getEmail());
    }
}
