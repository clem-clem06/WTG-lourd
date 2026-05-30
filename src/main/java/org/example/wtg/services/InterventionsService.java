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
import java.util.List;

@Service
public class InterventionsService {

    private static final Logger log = LoggerFactory.getLogger(InterventionsService.class);

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
        // Initialise la collection lazy unites dans la transaction
        list.forEach(i -> i.getUnites().size());
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
        log.info("Intervention créée id={} type={} etat={} ({} unités)",
                saved.getId(), type, etat, i.getUnites().size());
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
        log.info("Intervention id={} → état {}", id, nouvelEtat);
        return saved;
    }

    @Transactional
    public void supprimerIntervention(Integer id) {
        Intervention i = interventionRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Intervention introuvable (id=" + id + ")"));
        interventionRepository.deleteById(id);
        log.info("Intervention supprimée id={} type={}", id, i.getType());
    }
}
