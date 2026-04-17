package org.example.wtg.repositories;

import org.example.wtg.entities.Order;
import org.example.wtg.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Trouver toutes les commandes d'un user précis
    // → SELECT * FROM app_order WHERE user_id = ?
    List<Order> findByUser(User user);

    // Filtrer par statut (ex: "paid", "pending")
    // → SELECT * FROM app_order WHERE status = ?
    List<Order> findByStatus(String status);

    // Pour le rapport mensuel (fonctionnalité optionnelle du cahier des charges)
    // → SELECT * FROM app_order WHERE created_at BETWEEN ? AND ?
    List<Order> findByCreatedAtBetween(LocalDateTime debut, LocalDateTime fin);
}
