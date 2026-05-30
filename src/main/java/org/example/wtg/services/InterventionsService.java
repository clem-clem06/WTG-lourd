package org.example.wtg.services;

import org.example.wtg.entities.Intervention;
import org.example.wtg.entities.Unite;
import org.example.wtg.repositories.InterventionRepository;
import org.example.wtg.repositories.UniteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class InterventionsService {

    private static final Logger log = LoggerFactory.getLogger(InterventionsService.class);

    // États d'intervention
    private static final String EN_COURS = "En cours";
    private static final String TYPE_REPARATION = "Réparation";
    // États d'unité (mêmes valeurs que UnitesService : "Incident"/"Maintenance")
    private static final String UNITE_OK = "OK";
    private static final String UNITE_INCIDENT = "Incident";
    private static final String UNITE_MAINTENANCE = "Maintenance";
    private static final String UNITE_ATTENTE_PAIEMENT = "en attente de paiement";

    private final InterventionRepository interventionRepository;
    private final UniteRepository uniteRepository;

    public InterventionsService(InterventionRepository interventionRepository,
                                UniteRepository uniteRepository) {
        this.interventionRepository = interventionRepository;
        this.uniteRepository = uniteRepository;
    }

    /** Toutes les unités disponibles pour la liste de sélection */
    @Transactional(readOnly = true)
    public List<Unite> listerUnitesDisponibles() {
        List<Unite> unites = uniteRepository.findAll();
        unites.forEach(u -> {
            if (u.getBaie() != null) u.getBaie().getReference();
        });
        return unites;
    }

    @Transactional(readOnly = true)
    public List<Intervention> listerInterventions() {
        List<Intervention> list = interventionRepository.findAll();
        // Initialise les collections lazy dans la transaction
        list.forEach(i -> i.getUnites().forEach(u -> {
            if (u.getBaie() != null) u.getBaie().getReference();
        }));
        return list;
    }

    public long totalInterventions() { return interventionRepository.count(); }

    public long interventionsPlanifiees() {
        return interventionRepository.findAll().stream()
                .filter(i -> "Planifiée".equals(i.getEtat())).count();
    }

    public long interventionsEnCours() {
        return interventionRepository.findAll().stream()
                .filter(i -> "En cours".equals(i.getEtat())).count();
    }

    public long interventionsTerminees() {
        return interventionRepository.findAll().stream()
                .filter(i -> "Terminée".equals(i.getEtat())).count();
    }

    @Transactional
    public Intervention creerIntervention(String type, String description, String etat,
                                          LocalDateTime dateDebut, List<Integer> uniteIds) {
        if (type == null || type.isBlank())
            throw new IllegalArgumentException("Le type est obligatoire.");
        if (etat == null || etat.isBlank())
            throw new IllegalArgumentException("L'état est obligatoire.");

        Intervention i = new Intervention();
        i.setType(type.trim());
        i.setDescription(description == null ? "" : description.trim());
        i.setEtat(etat);
        i.setDateDebut(dateDebut != null ? dateDebut : LocalDateTime.now());

        if (uniteIds != null && !uniteIds.isEmpty()) {
            List<Unite> unites = uniteRepository.findAllById(uniteIds);
            i.setUnites(unites);
        }

        Intervention saved = interventionRepository.save(i);
        recalculerEtats(i.getUnites());
        log.info("Intervention créée id={} type={} etat={} ({} unités)",
                saved.getId(), type, etat, i.getUnites().size());
        return saved;
    }

    @Transactional
    public Intervention modifierIntervention(Integer id, String type, String description,
                                             String etat, java.time.LocalDate dateDebut,
                                             List<Integer> uniteIds) {
        if (type == null || type.isBlank())
            throw new IllegalArgumentException("Le type est obligatoire.");
        Intervention i = interventionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Intervention introuvable (id=" + id + ")"));

        // On garde les anciennes unités : si on en retire, il faut aussi recalculer leur état.
        Set<Unite> affectees = new HashSet<>(i.getUnites());

        i.setType(type.trim());
        i.setDescription(description == null ? "" : description.trim());
        i.setEtat(etat);
        if (dateDebut != null) i.setDateDebut(dateDebut.atStartOfDay());
        if ("Terminée".equals(etat) && i.getDateFin() == null) i.setDateFin(LocalDateTime.now());
        if (uniteIds != null) i.setUnites(uniteRepository.findAllById(uniteIds));
        Intervention saved = interventionRepository.save(i);

        affectees.addAll(i.getUnites()); // + les nouvelles unités
        recalculerEtats(affectees);

        log.info("Intervention id={} modifiée → type={} etat={}", id, type, etat);
        return saved;
    }

    @Transactional
    public Intervention changerEtat(Integer id, String nouvelEtat) {
        Intervention i = interventionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Intervention introuvable (id=" + id + ")"));
        i.setEtat(nouvelEtat);
        if ("Terminée".equals(nouvelEtat) && i.getDateFin() == null) {
            i.setDateFin(LocalDateTime.now());
        }
        Intervention saved = interventionRepository.save(i);
        recalculerEtats(i.getUnites());
        log.info("Intervention id={} → état {}", id, nouvelEtat);
        return saved;
    }

    @Transactional
    public void supprimerIntervention(Integer id) {
        Intervention i = interventionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Intervention introuvable (id=" + id + ")"));
        // On mémorise les unités avant suppression pour recalculer leur état après.
        Set<Unite> affectees = new HashSet<>(i.getUnites());
        interventionRepository.deleteById(id);
        recalculerEtats(affectees);
        log.info("Intervention supprimée id={} type={}", id, i.getType());
    }

    // ─────────────────────────────────────────────────────────
    //  SYNCHRO ÉTAT DES UNITÉS
    // ─────────────────────────────────────────────────────────

    /** Recalcule l'état de plusieurs unités. */
    private void recalculerEtats(Collection<Unite> unites) {
        unites.forEach(this::recalculerEtatUnite);
    }

    /**
     * Détermine l'état d'une unité à partir de ses interventions "En cours" :
     *  - une réparation en cours  → "Incident"
     *  - une autre intervention en cours → "Maintenance"
     *  - aucune intervention en cours → "OK"
     * On ne touche jamais à une unité "en attente de paiement" (géré côté commandes).
     */
    private void recalculerEtatUnite(Unite u) {
        if (UNITE_ATTENTE_PAIEMENT.equalsIgnoreCase(u.getEtat())) {
            return;
        }

        List<Intervention> actives = interventionRepository.findActivesByUniteId(u.getId());

        String nouvel;
        if (actives.isEmpty()) {
            nouvel = UNITE_OK;
        } else if (actives.stream().anyMatch(it -> TYPE_REPARATION.equalsIgnoreCase(it.getType()))) {
            nouvel = UNITE_INCIDENT;
        } else {
            nouvel = UNITE_MAINTENANCE;
        }

        if (!nouvel.equals(u.getEtat())) {
            u.setEtat(nouvel);
            uniteRepository.save(u);
            log.info("Unité id={} : état recalculé → {}", u.getId(), nouvel);
        }
    }
}
