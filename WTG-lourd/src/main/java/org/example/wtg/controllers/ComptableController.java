package org.example.wtg.controllers;

import org.example.wtg.entities.Order;
import org.example.wtg.entities.User;
import org.example.wtg.repositories.OrderRepository;
import org.example.wtg.repositories.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/comptable")
public class ComptableController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public ComptableController(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    // ─────────────────────────────────────────────────────────────────
    //  GET /comptable/clients
    //  Retourne la liste des clients (users qui ont passé au moins une commande)
    //  Un "client" = un User avec des Orders dans la BDD
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/clients")
    public List<User> listerClients() {
        // findAllClients() = requête @Query dans UserRepository
        // Cherche les users avec ROLE_CLIENT dans leur champ JSON roles
        return userRepository.findAllClients();
    }

    // ─────────────────────────────────────────────────────────────────
    //  GET /comptable/reservations
    //  Retourne toutes les réservations (commandes) avec leur détail
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/reservations")
    public List<Order> listerReservations() {
        return orderRepository.findAll();
    }
}
