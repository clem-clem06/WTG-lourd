package org.example.wtg.repositories;

import org.example.wtg.entities.Baie;
import org.example.wtg.entities.Unite;
import org.example.wtg.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UniteRepository extends JpaRepository<Unite, Integer> {

    // Toutes les unités d'une baie précise
    // → SELECT * FROM unite WHERE baie_id = ?
    List<Unite> findByBaie(Baie baie);

    // Toutes les unités louées par un client précis
    // → SELECT * FROM unite WHERE locataire_id = ?
    List<Unite> findByLocataire(User locataire);

    // Unités libres (pas encore louées, locataire = null)
    // → SELECT * FROM unite WHERE locataire_id IS NULL
    List<Unite> findByLocataireIsNull();

    // Unités par état (OK, incident, maintenance)
    // → SELECT * FROM unite WHERE etat = ?
    List<Unite> findByEtat(String etat);
}
