package org.example.wtg.repositories;

import org.example.wtg.entities.Intervention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterventionRepository extends JpaRepository<Intervention, Integer> {

    // Interventions "En cours" qui concernent une unité donnée.
    // Sert à recalculer l'état d'une unité (Maintenance/Incident/OK).
    @Query("SELECT i FROM Intervention i JOIN i.unites u WHERE u.id = :uniteId AND i.etat = 'En cours'")
    List<Intervention> findActivesByUniteId(@Param("uniteId") Integer uniteId);
}