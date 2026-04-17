package org.example.wtg.repositories;

import org.example.wtg.entities.Baie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BaieRepository extends JpaRepository<Baie, Integer> {

}