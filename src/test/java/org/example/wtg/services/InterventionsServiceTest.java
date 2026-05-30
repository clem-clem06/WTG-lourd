package org.example.wtg.services;

import org.example.wtg.entities.Intervention;
import org.example.wtg.repositories.InterventionRepository;
import org.example.wtg.repositories.UniteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterventionsServiceTest {

    @Mock InterventionRepository interventionRepository;
    @Mock UniteRepository uniteRepository;
    @InjectMocks InterventionsService service;

    // ── creerIntervention ──────────────────────────────────────

    @Test
    void creerIntervention_champsValides_sauvegarde() {
        Intervention saved = new Intervention();
        saved.setId(1); saved.setType("Maintenance"); saved.setEtat("Planifiée");
        when(interventionRepository.save(any())).thenReturn(saved);

        Intervention result = service.creerIntervention(
                "Maintenance", "Remplacement disque", "Planifiée", LocalDateTime.now(), List.of());

        assertThat(result.getType()).isEqualTo("Maintenance");
        verify(interventionRepository).save(any(Intervention.class));
    }

    @Test
    void creerIntervention_typeVide_lanceException() {
        assertThatThrownBy(() ->
                service.creerIntervention("", "desc", "Planifiée", null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
    }

    @Test
    void creerIntervention_etatVide_lanceException() {
        assertThatThrownBy(() ->
                service.creerIntervention("Maintenance", "desc", "", null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("état");
    }

    // ── changerEtat ────────────────────────────────────────────

    @Test
    void changerEtat_versTerminee_renseigneDateFin() {
        Intervention i = new Intervention(); i.setId(1); i.setEtat("En cours");
        when(interventionRepository.findById(1)).thenReturn(Optional.of(i));
        when(interventionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Intervention result = service.changerEtat(1, "Terminée");

        assertThat(result.getEtat()).isEqualTo("Terminée");
        assertThat(result.getDateFin()).isNotNull();
    }

    @Test
    void changerEtat_idInexistant_lanceException() {
        when(interventionRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changerEtat(99, "Terminée"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("introuvable");
    }

    // ── supprimerIntervention ──────────────────────────────────

    @Test
    void supprimerIntervention_idExistant_supprime() {
        Intervention i = new Intervention(); i.setId(1); i.setType("Inspection");
        when(interventionRepository.findById(1)).thenReturn(Optional.of(i));

        service.supprimerIntervention(1);

        verify(interventionRepository).deleteById(1);
    }

    @Test
    void supprimerIntervention_idInexistant_lanceException() {
        when(interventionRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.supprimerIntervention(99))
                .isInstanceOf(IllegalStateException.class);
    }
}
