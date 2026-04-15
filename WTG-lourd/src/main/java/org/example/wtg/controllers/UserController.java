package org.example.wtg.controllers;

import org.example.wtg.entities.User;
import org.example.wtg.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @RestController = @Controller + @ResponseBody
// Toutes les méthodes renvoient du JSON automatiquement
@RestController
// Toutes les routes de ce controller commencent par /admin/users
@RequestMapping("/admin/users")
public class UserController {

    // On injecte les dépendances via le constructeur (bonne pratique Spring)
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ─────────────────────────────────────────
    //  GET /admin/users → liste tous les users
    // ─────────────────────────────────────────
    @GetMapping
    public List<User> listerUtilisateurs() {
        return userRepository.findAll();
    }

    // ─────────────────────────────────────────────
    //  GET /admin/users/{id} → un user par son ID
    // ─────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<User> getUtilisateur(@PathVariable Integer id) {
        // findById retourne un Optional<User> (peut être vide si ID inexistant)
        return userRepository.findById(id)
                .map(ResponseEntity::ok)                          // trouvé → 200 OK avec le user
                .orElse(ResponseEntity.notFound().build());       // pas trouvé → 404
    }

    // ─────────────────────────────────────────────────────────
    //  POST /admin/users → créer un nouvel utilisateur
    //  Le body JSON est automatiquement converti en objet User
    // ─────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<User> creerUtilisateur(@RequestBody User user) {
        // Encoder le mot de passe avant de sauvegarder (comme Symfony le fait)
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        // 201 Created avec le user créé dans le body
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ────────────────────────────────────────────────────────────
    //  PUT /admin/users/{id} → modifier un utilisateur existant
    // ────────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<User> modifierUtilisateur(@PathVariable Integer id, @RequestBody User userModifie) {
        return userRepository.findById(id)
                .map(existant -> {
                    // On met à jour seulement les champs fournis
                    existant.setEmail(userModifie.getEmail());
                    existant.setRoles(userModifie.getRoles());

                    // Si un nouveau mot de passe est fourni, on l'encode
                    if (userModifie.getPassword() != null && !userModifie.getPassword().isBlank()) {
                        existant.setPassword(passwordEncoder.encode(userModifie.getPassword()));
                    }

                    return ResponseEntity.ok(userRepository.save(existant));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ────────────────────────────────────────────────────────────────
    //  DELETE /admin/users/{id} → supprimer un utilisateur
    // ────────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerUtilisateur(@PathVariable Integer id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
