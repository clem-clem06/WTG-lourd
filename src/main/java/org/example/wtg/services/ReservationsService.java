package org.example.wtg.services;

import org.example.wtg.entities.Order;
import org.example.wtg.entities.OrderItem;
import org.example.wtg.entities.Payment;
import org.example.wtg.entities.Unite;
import org.example.wtg.repositories.OrderRepository;
import org.example.wtg.repositories.PaymentRepository;
import org.example.wtg.repositories.UniteRepository;
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

    // État d'une unité réservée mais dont le virement n'est pas encore reçu.
    // Doit correspondre exactement à la valeur écrite par Symfony (CheckoutService).
    private static final String ETAT_EN_ATTENTE = "en attente de paiement";
    private static final String ETAT_OK = "OK";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UniteRepository uniteRepository;

    public ReservationsService(OrderRepository orderRepository,
                               PaymentRepository paymentRepository,
                               UniteRepository uniteRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.uniteRepository = uniteRepository;
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
            // Le message de checkout ("Virement bancaire en attente de réception")
            // n'est plus d'actualité une fois le virement validé.
            p.setGatewayResponse("Virement reçu");
            paymentRepository.save(p);
        }

        // Active les unités de la commande : "en attente de paiement" → "OK".
        // Sans ça, la facturation passe en "payée" mais les serveurs restent
        // bloqués "en attente" côté espace client Symfony.
        int actives = activerUnites(o);

        log.info("Paiement validé commande id={} ({} payment(s), {} unité(s) activée(s))",
                id, o.getPayments().size(), actives);
        return saved;
    }

    /**
     * Repasse les unités de la commande de "en attente de paiement" à "OK".
     *
     * @return le nombre d'unités effectivement activées
     */
    private int activerUnites(Order o) {
        List<Unite> enAttente = uniteRepository.findByLocataireAndEtat(o.getUser(), ETAT_EN_ATTENTE);
        int aTraiter = Math.min(unitesRequises(o), enAttente.size());
        for (int i = 0; i < aTraiter; i++) {
            Unite u = enAttente.get(i);
            u.setEtat(ETAT_OK);
            uniteRepository.save(u);
        }
        return aTraiter;
    }

    /** Nombre d'unités que représente la commande (Σ offre.nombreUnites × quantité). */
    private int unitesRequises(Order o) {
        int total = 0;
        for (OrderItem item : o.getOrderItems()) {
            total += item.getOffre().getNombreUnites() * item.getQuantity();
        }
        return total;
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

        // Libère les unités réservées non payées : on les remet dans le stock
        // disponible (locataire = null, etat = OK, date de fin effacée).
        int liberees = libererUnites(o);

        log.info("Commande annulée id={} ({} payment(s), {} unité(s) libérée(s))",
                id, o.getPayments().size(), liberees);
        return saved;
    }

    /**
     * Libère les unités "en attente de paiement" de la commande : elles
     * redeviennent disponibles dans le stock.
     *
     * @return le nombre d'unités libérées
     */
    private int libererUnites(Order o) {
        List<Unite> enAttente = uniteRepository.findByLocataireAndEtat(o.getUser(), ETAT_EN_ATTENTE);
        int aTraiter = Math.min(unitesRequises(o), enAttente.size());
        for (int i = 0; i < aTraiter; i++) {
            Unite u = enAttente.get(i);
            u.setLocataire(null);
            u.setDateFinLocation(null);
            u.setEtat(ETAT_OK);
            uniteRepository.save(u);
        }
        return aTraiter;
    }
}
