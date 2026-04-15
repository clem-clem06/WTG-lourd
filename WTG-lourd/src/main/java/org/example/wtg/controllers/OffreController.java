package org.example.wtg.controllers;

import org.example.wtg.entities.Offre;
import org.example.wtg.repositories.OffreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/offres")
public class OffreController {

    private final OffreRepository offreRepository;

    public OffreController(OffreRepository offreRepository) {
        this.offreRepository = offreRepository;
    }

    // GET /admin/offres → liste toutes les offres
    @GetMapping
    public List<Offre> listerOffres() {
        return offreRepository.findAll();
    }

    // GET /admin/offres/{id} → une offre par ID
    @GetMapping("/{id}")
    public ResponseEntity<Offre> getOffre(@PathVariable Integer id) {
        return offreRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /admin/offres → créer une offre
    // Body JSON attendu :
    // {
    //   "nom": "Start-up",
    //   "nombreUnites": 10,
    //   "prixMensuel": 900,
    //   "prixAnnuel": 9720
    // }
    @PostMapping
    public ResponseEntity<Offre> creerOffre(@RequestBody Offre offre) {
        Offre saved = offreRepository.save(offre);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // PUT /admin/offres/{id} → modifier une offre existante
    @PutMapping("/{id}")
    public ResponseEntity<Offre> modifierOffre(@PathVariable Integer id, @RequestBody Offre offreModifiee) {
        return offreRepository.findById(id)
                .map(existant -> {
                    existant.setNom(offreModifiee.getNom());
                    existant.setNombreUnites(offreModifiee.getNombreUnites());
                    existant.setPrixMensuel(offreModifiee.getPrixMensuel());
                    existant.setPrixAnnuel(offreModifiee.getPrixAnnuel());
                    return ResponseEntity.ok(offreRepository.save(existant));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /admin/offres/{id} → supprimer une offre
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerOffre(@PathVariable Integer id) {
        if (!offreRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        offreRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
