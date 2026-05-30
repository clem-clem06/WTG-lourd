package org.example.wtg.services;

import org.example.wtg.entities.Baie;
import org.example.wtg.entities.Unite;
import org.example.wtg.repositories.BaieRepository;
import org.example.wtg.repositories.UniteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BaiesService {

    private static final Logger log = LoggerFactory.getLogger(BaiesService.class);
    private static final int MAX_UNITES = 42;

    private final BaieRepository baieRepository;
    private final UniteRepository uniteRepository;

    public BaiesService(BaieRepository baieRepository, UniteRepository uniteRepository) {
        this.baieRepository = baieRepository;
        this.uniteRepository = uniteRepository;
    }

    // ─────────────────────────── LECTURE ─────────────────────────────

    public List<Baie> listerBaies() { return baieRepository.findAll(); }
    public long totalBaies()        { return baieRepository.count(); }
    public long totalUnites()       { return uniteRepository.count(); }

    public long unitesLibres() {
        return uniteRepository.findByLocataireIsNull().size();
    }
    public long unitesOccupees() { return totalUnites() - unitesLibres(); }

    // ─────────────────────────── CRÉATION ────────────────────────────

    /**
     * Crée une baie avec {@code nbUnites} unités vierges.
     * Limite : 1 ≤ nbUnites ≤ 42.
     */
    public Baie creerBaie(String reference, int nbUnites) {
        if (reference == null || reference.isBlank())
            throw new IllegalArgumentException("La référence est obligatoire.");
        if (nbUnites < 1 || nbUnites > MAX_UNITES)
            throw new IllegalArgumentException("Nombre d'unités : entre 1 et " + MAX_UNITES + ".");

        Baie baie = new Baie();
        baie.setReference(reference.trim());

        // Pré-crée les unités associées
        List<Unite> unites = new ArrayList<>();
        for (int i = 1; i <= nbUnites; i++) {
            Unite u = new Unite();
            u.setNumero("U" + i);
            u.setNom("Unité " + i);
            u.setEtat("OK");
            u.setType("Rack");
            u.setCouleur("#1e293b");
            u.setBaie(baie);
            unites.add(u);
        }
        baie.setUnites(unites);

        Baie saved = baieRepository.save(baie); // cascade ALL → unités sauvegardées aussi
        log.info("Création baie ref={} ({} unités, id={})", reference, nbUnites, saved.getId());
        return saved;
    }

    // ─────────────────────────── MODIFICATION ────────────────────────

    /**
     * Modifie la référence et le nombre d'unités d'une baie.
     * <ul>
     *   <li>Si {@code nouveauNbUnites > actuel} → on ajoute des unités libres.</li>
     *   <li>Si {@code nouveauNbUnites < actuel} → on supprime des unités LIBRES ;
     *       erreur si pas assez d'unités libres disponibles.</li>
     * </ul>
     */
    public Baie modifierBaie(Integer id, String reference, int nouveauNbUnites) {
        if (reference == null || reference.isBlank())
            throw new IllegalArgumentException("La référence est obligatoire.");
        if (nouveauNbUnites < 1 || nouveauNbUnites > MAX_UNITES)
            throw new IllegalArgumentException("Nombre d'unités : entre 1 et " + MAX_UNITES + ".");

        Baie baie = baieRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Baie introuvable (id=" + id + ")"));
        baie.setReference(reference.trim());

        List<Unite> unitesActuelles = uniteRepository.findByBaie(baie);
        int actuel = unitesActuelles.size();
        int delta  = nouveauNbUnites - actuel;

        if (delta > 0) {
            // Ajouter des unités libres en continuant la numérotation
            for (int i = actuel + 1; i <= nouveauNbUnites; i++) {
                Unite u = new Unite();
                u.setNumero("U" + i);
                u.setNom("Unité " + i);
                u.setEtat("OK");
                u.setType("Rack");
                u.setCouleur("#1e293b");
                u.setBaie(baie);
                uniteRepository.save(u);
            }
        } else if (delta < 0) {
            // Retirer des unités libres (les dernières, en priorité)
            int aRetirer = Math.abs(delta);
            List<Unite> libres = unitesActuelles.stream()
                    .filter(u -> u.getLocataire() == null)
                    .toList();
            if (libres.size() < aRetirer)
                throw new IllegalStateException(
                        "Seulement " + libres.size() + " unité(s) libre(s) — impossible de réduire à " + nouveauNbUnites + ".");
            // Supprimer les dernières unités libres
            libres.stream()
                    .sorted((a, b) -> b.getId().compareTo(a.getId())) // dernières en premier
                    .limit(aRetirer)
                    .forEach(u -> uniteRepository.deleteById(u.getId()));
        }

        Baie saved = baieRepository.save(baie);
        log.info("Modification baie id={} → ref={}, {} unités (delta={})", id, reference, nouveauNbUnites, delta);
        return saved;
    }

    // ─────────────────────────── SUPPRESSION ─────────────────────────

    /**
     * Supprime une baie. Refusé si au moins une unité est occupée.
     */
    public void supprimerBaie(Integer id) {
        Baie baie = baieRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Baie introuvable (id=" + id + ")"));

        boolean occupee = uniteRepository.findByBaie(baie).stream()
                .anyMatch(u -> u.getLocataire() != null);
        if (occupee)
            throw new IllegalStateException(
                    "Impossible de supprimer : au moins une unité de cette baie est louée.");

        baieRepository.deleteById(id);
        log.info("Suppression baie id={} ref={}", id, baie.getReference());
    }

    // ─────────────────────────── HELPERS ─────────────────────────────

    public List<Unite> unitesParBaie(Baie baie) {
        return uniteRepository.findByBaie(baie);
    }
}
