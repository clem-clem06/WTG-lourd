package org.example.wtg.services;

import org.example.wtg.entities.Unite;
import org.example.wtg.repositories.UniteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UnitesService {

    private static final Logger log = LoggerFactory.getLogger(UnitesService.class);

    private final UniteRepository uniteRepository;

    public UnitesService(UniteRepository uniteRepository) {
        this.uniteRepository = uniteRepository;
    }

    @Transactional(readOnly = true)
    public List<Unite> listerUnites() {
        List<Unite> unites = uniteRepository.findAll();
        // Initialise les associations lazy dans la transaction
        unites.forEach(u -> {
            if (u.getBaie()      != null) u.getBaie().getReference();
            if (u.getLocataire() != null) u.getLocataire().getEmail();
        });
        return unites;
    }

    public long totalUnites()       { return uniteRepository.count(); }
    public long unitesLibres()      { return uniteRepository.findByLocataireIsNull().size(); }
    public long unitesOccupees()    { return totalUnites() - unitesLibres(); }
    public long unitesEnIncident()  { return uniteRepository.findByEtat("Incident").size(); }
    public long unitesEnMaintenance() { return uniteRepository.findByEtat("Maintenance").size(); }

    @Transactional
    public Unite changerEtat(Integer id, String nouvelEtat) {
        if (nouvelEtat == null || nouvelEtat.isBlank())
            throw new IllegalArgumentException("L'état ne peut pas être vide.");
        Unite u = uniteRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unité introuvable (id=" + id + ")"));
        String ancien = u.getEtat();
        u.setEtat(nouvelEtat);
        Unite saved = uniteRepository.save(u);
        log.info("Unité id={} ref={} : état {} → {}", id, u.getNumero(), ancien, nouvelEtat);
        return saved;
    }
}
