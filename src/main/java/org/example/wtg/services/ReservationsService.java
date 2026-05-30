package org.example.wtg.services;

import org.example.wtg.entities.Order;
import org.example.wtg.entities.Payment;
import org.example.wtg.repositories.OrderRepository;
import org.example.wtg.repositories.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReservationsService {

    private static final Logger log = LoggerFactory.getLogger(ReservationsService.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public ReservationsService(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<Order> listerCommandes() {
        return orderRepository.findAll();
    }

    public long totalCommandes() {
        return orderRepository.count();
    }

    public long commandesPayees() {
        return orderRepository.findByStatus("paid").size();
    }

    public long commandesEnAttente() {
        return orderRepository.findByStatus("pending").size();
    }

    /** Revenu total : somme des commandes payées */
    public double revenuTotal() {
        return orderRepository.findAll().stream()
                .filter(o -> "paid".equals(o.getStatus()))
                .mapToDouble(o -> o.getTotal() == null ? 0 : o.getTotal())
                .sum();
    }

    /** Nombre de commandes par statut pour le BarChart */
    public Map<String, Long> commandesParStatut() {
        return orderRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStatus() == null ? "inconnu" : o.getStatus(),
                        Collectors.counting()
                ));
    }

    @Transactional
    public Order validerPaiement(Integer id) {
        Order o = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Commande introuvable (id=" + id + ")"));
        if (!"pending".equals(o.getStatus()))
            throw new IllegalStateException("Seules les commandes en attente peuvent être validées.");

        // Met à jour la commande
        o.setStatus("paid");
        Order saved = orderRepository.save(o);

        // Met aussi à jour tous les Payment liés (c'est ce que Symfony affiche)
        for (Payment p : o.getPayments()) {
            p.setStatus("paid");
            paymentRepository.save(p);
        }

        log.info("Paiement validé commande id={} ({} payment(s) mis à jour)", id, o.getPayments().size());
        return saved;
    }

    @Transactional
    public Order annulerCommande(Integer id) {
        Order o = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Commande introuvable (id=" + id + ")"));
        if ("cancel".equals(o.getStatus()))
            throw new IllegalStateException("Cette commande est déjà annulée.");

        // Met à jour la commande
        o.setStatus("cancel");
        Order saved = orderRepository.save(o);

        // Met aussi à jour tous les Payment liés
        for (Payment p : o.getPayments()) {
            p.setStatus("cancel");
            paymentRepository.save(p);
        }

        log.info("Commande annulée id={} ({} payment(s) mis à jour)", id, o.getPayments().size());
        return saved;
    }
}
