package org.example.wtg.controllers;

import org.example.wtg.entities.Baie;
import org.example.wtg.repositories.BaieRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/baies")
public class BaieController {

    private final BaieRepository baieRepository;

    public BaieController(BaieRepository baieRepository) {
        this.baieRepository = baieRepository;
    }

    // GET /admin/baies → liste toutes les baies (avec leurs unités)
    @GetMapping
    public List<Baie> listerBaies() {
        return baieRepository.findAll();
    }

    // GET /admin/baies/{id} → une baie par ID
    @GetMapping("/{id}")
    public ResponseEntity<Baie> getBaie(@PathVariable Integer id) {
        return baieRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /admin/baies → créer une baie
    // Body JSON attendu : { "reference": "B031" }
    @PostMapping
    public ResponseEntity<Baie> creerBaie(@RequestBody Baie baie) {
        Baie saved = baieRepository.save(baie);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // PUT /admin/baies/{id} → modifier la référence d'une baie
    @PutMapping("/{id}")
    public ResponseEntity<Baie> modifierBaie(@PathVariable Integer id, @RequestBody Baie baieModifiee) {
        return baieRepository.findById(id)
                .map(existant -> {
                    existant.setReference(baieModifiee.getReference());
                    return ResponseEntity.ok(baieRepository.save(existant));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /admin/baies/{id} → supprimer une baie
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerBaie(@PathVariable Integer id) {
        if (!baieRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        baieRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
