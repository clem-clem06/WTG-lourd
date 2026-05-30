package org.example.wtg.services;

import org.example.wtg.entities.User;
import org.example.wtg.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClientsService {

    private final UserRepository userRepository;

    public ClientsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Charge tous les clients avec leurs commandes et unités initialisées
     * (évite LazyInitializationException dans le controller JavaFX).
     */
    @Transactional(readOnly = true)
    public List<User> listerClients() {
        List<User> clients = userRepository.findAllClients();
        // Force l'initialisation des collections lazy dans la transaction
        clients.forEach(u -> {
            u.getOrders().size();
            u.getUnites().size();
        });
        return clients;
    }

    public long totalClients() {
        return userRepository.findAllClients().size();
    }

    /** Clients qui ont passé au moins une commande */
    @Transactional(readOnly = true)
    public long clientsActifs() {
        return userRepository.findAllClients().stream()
                .filter(u -> !u.getOrders().isEmpty())
                .count();
    }

    public long clientsInactifs() {
        return totalClients() - clientsActifs();
    }

    /** Nombre total d'unités louées par tous les clients */
    @Transactional(readOnly = true)
    public long unitesLouees() {
        return userRepository.findAllClients().stream()
                .mapToLong(u -> u.getUnites().size())
                .sum();
    }
}
