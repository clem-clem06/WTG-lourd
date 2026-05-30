package org.example.wtg.services;

import org.example.wtg.entities.Offre;
import org.example.wtg.repositories.OffreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffresServiceTest {

    @Mock OffreRepository offreRepository;
    @InjectMocks OffresService service;

    // ── creerOffre ─────────────────────────────────────────────

    @Test
    void creerOffre_champsValides_sauvegarde() {
        Offre saved = new Offre(); saved.setId(1); saved.setNom("Start-up");
        when(offreRepository.save(any())).thenReturn(saved);

        Offre result = service.creerOffre("Start-up", 10, 299.0, 2990.0);

        assertThat(result.getNom()).isEqualTo("Start-up");
        verify(offreRepository).save(any(Offre.class));
    }

    @Test
    void creerOffre_nomVide_lanceException() {
        assertThatThrownBy(() -> service.creerOffre("", 10, 299.0, 2990.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nom");
    }

    @Test
    void creerOffre_unitesNegatives_lanceException() {
        assertThatThrownBy(() -> service.creerOffre("Test", 0, 299.0, 2990.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unités");
    }

    @Test
    void creerOffre_prixNegatif_lanceException() {
        assertThatThrownBy(() -> service.creerOffre("Test", 5, -1.0, 100.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mensuel");
    }

    // ── supprimerOffre ─────────────────────────────────────────

    @Test
    void supprimerOffre_idExistant_supprime() {
        Offre o = new Offre(); o.setId(1); o.setNom("PME");
        when(offreRepository.findById(1)).thenReturn(Optional.of(o));

        service.supprimerOffre(1);

        verify(offreRepository).deleteById(1);
    }

    @Test
    void supprimerOffre_idInexistant_lanceException() {
        when(offreRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.supprimerOffre(99))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("introuvable");
    }

    // ── listerOffres ───────────────────────────────────────────

    @Test
    void listerOffres_retourneTout() {
        when(offreRepository.findAll()).thenReturn(List.of(new Offre(), new Offre()));

        assertThat(service.listerOffres()).hasSize(2);
    }
}
