package org.example.wtg.repositories;

import org.example.wtg.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Utilisé par CustomUserDetailsService pour la connexion
    // → SELECT * FROM user WHERE email = ?
    Optional<User> findByEmail(String email);

    // Trouver les clients : users qui ont le rôle ROLE_CLIENT dans leur JSON
    // On ne peut pas utiliser findByRoles() car c'est un champ JSON
    // On utilise une @Query avec LIKE pour chercher dans le texte JSON
    // → SELECT * FROM user WHERE roles LIKE '%ROLE_CLIENT%'
    @Query("SELECT u FROM User u WHERE u.roles LIKE '%ROLE_CLIENT%'")
    List<User> findAllClients();

    // Trouver les membres du personnel (Admin + Comptable)
    @Query("SELECT u FROM User u WHERE u.roles LIKE '%ROLE_ADMIN%' OR u.roles LIKE '%ROLE_COMPTABLE%'")
    List<User> findAllPersonnel();
}
