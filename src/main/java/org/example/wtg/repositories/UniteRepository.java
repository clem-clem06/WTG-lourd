package org.example.wtg.repositories;

import org.example.wtg.entities.Unite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UniteRepository extends JpaRepository<Unite, Integer> {

}