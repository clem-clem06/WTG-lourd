package org.example.wtg.services;

import org.example.wtg.entities.Offre;
import org.example.wtg.repositories.OffreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OffresService {

    private static final Logger log = LoggerFactory.getLogger(OffresService.class);

    private final OffreRepository offreRepository;

    public OffresService(OffreRepository offreRepository) {
        this.offreRepository = offreRepository;
    }

    public List<Offre> listerOffres() { return offreRepository.findAll(); }
    public long totalOffres()         { return offreRepository.count(); }

    public Offre creerOffre(String nom, int nombreUnites, double prixMensuel, double prixAnnuel) {
        valider(nom, nombreUnites, prixMensuel, prixAnnuel);
        Offre o = new Offre();
        o.setNom(nom.trim());
        o.setNombreUnites(nombreUnites);
        o.setPrixMensuel(prixMensuel);
        o.setPrixAnnuel(prixAnnuel);
        Offre saved = offreRepository.save(o);
        log.info("Création offre «{}» ({} unités, {}€/mois)", nom, nombreUnites, prixMensuel);
        return saved;
    }

    public Offre modifierOffre(Integer id, String nom, int nombreUnites, double prixMensuel, double prixAnnuel) {
        valider(nom, nombreUnites, prixMensuel, prixAnnuel);
        Offre o = offreRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Offre introuvable (id=" + id + ")"));
        o.setNom(nom.trim());
        o.setNombreUnites(nombreUnites);
        o.setPrixMensuel(prixMensuel);
        o.setPrixAnnuel(prixAnnuel);
        Offre saved = offreRepository.save(o);
        log.info("Modification offre id={} → «{}»", id, nom);
        return saved;
    }

    public void supprimerOffre(Integer id) {
        Offre o = offreRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Offre introuvable (id=" + id + ")"));
        offreRepository.deleteById(id);
        log.info("Suppression offre id={} «{}»", id, o.getNom());
    }

    private void valider(String nom, int nbUnites, double prixM, double prixA) {
        if (nom == null || nom.isBlank())
            throw new IllegalArgumentException("Le nom est obligatoire.");
        if (nbUnites < 1)
            throw new IllegalArgumentException("Le nombre d'unités doit être ≥ 1.");
        if (prixM < 0)
            throw new IllegalArgumentException("Le prix mensuel ne peut pas être négatif.");
        if (prixA < 0)
            throw new IllegalArgumentException("Le prix annuel ne peut pas être négatif.");
    }
}
