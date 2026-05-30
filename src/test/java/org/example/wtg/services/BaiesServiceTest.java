package org.example.wtg.services;

import org.example.wtg.entities.Baie;
import org.example.wtg.entities.Unite;
import org.example.wtg.repositories.BaieRepository;
import org.example.wtg.repositories.UniteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaiesServiceTest {

    @Mock BaieRepository baieRepository;
    @Mock UniteRepository uniteRepository;
    @InjectMocks BaiesService service;

    // ── creerBaie ──────────────────────────────────────────────

    @Test
    void creerBaie_nombrUnites_valide_sauvegarde() {
        Baie saved = new Baie(); saved.setId(1); saved.setReference("B001");
        when(baieRepository.save(any())).thenReturn(saved);

        Baie result = service.creerBaie("B001", 5);

        assertThat(result.getReference()).isEqualTo("B001");
        verify(baieRepository).save(any(Baie.class));
    }

    @Test
    void creerBaie_referenceVide_lanceException() {
        assertThatThrownBy(() -> service.creerBaie("", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("référence");
    }

    @Test
    void creerBaie_tropDUnites_lanceException() {
        assertThatThrownBy(() -> service.creerBaie("B001", 43))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("42");
    }

    @Test
    void creerBaie_aucuneUnite_lanceException() {
        assertThatThrownBy(() -> service.creerBaie("B001", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── supprimerBaie ──────────────────────────────────────────

    @Test
    void supprimerBaie_unitesLibres_supprime() {
        Baie baie = new Baie(); baie.setId(1); baie.setReference("B001");
        Unite u = new Unite(); u.setLocataire(null);

        when(baieRepository.findById(1)).thenReturn(Optional.of(baie));
        when(uniteRepository.findByBaie(baie)).thenReturn(List.of(u));

        service.supprimerBaie(1);

        verify(baieRepository).deleteById(1);
    }

    @Test
    void supprimerBaie_uniteOccupee_lanceException() {
        Baie baie = new Baie(); baie.setId(1);
        org.example.wtg.entities.User loc = new org.example.wtg.entities.User();
        Unite u = new Unite(); u.setLocataire(loc);

        when(baieRepository.findById(1)).thenReturn(Optional.of(baie));
        when(uniteRepository.findByBaie(baie)).thenReturn(List.of(u));

        assertThatThrownBy(() -> service.supprimerBaie(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("louée");
    }

    @Test
    void supprimerBaie_idInexistant_lanceException() {
        when(baieRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.supprimerBaie(99))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("introuvable");
    }
}
