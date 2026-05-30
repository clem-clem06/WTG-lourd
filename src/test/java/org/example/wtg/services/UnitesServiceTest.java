package org.example.wtg.services;

import org.example.wtg.entities.Unite;
import org.example.wtg.repositories.UniteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnitesServiceTest {

    @Mock UniteRepository uniteRepository;
    @InjectMocks UnitesService service;

    @Test
    void changerEtat_idValide_metAJour() {
        Unite u = new Unite(); u.setId(1); u.setNumero("U01"); u.setEtat("OK");
        when(uniteRepository.findById(1)).thenReturn(Optional.of(u));
        when(uniteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Unite result = service.changerEtat(1, "Maintenance");

        assertThat(result.getEtat()).isEqualTo("Maintenance");
        verify(uniteRepository).save(u);
    }

    @Test
    void changerEtat_idInexistant_lanceException() {
        when(uniteRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changerEtat(99, "OK"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void changerEtat_etatVide_lanceException() {
        assertThatThrownBy(() -> service.changerEtat(1, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vide");
    }

    @Test
    void totalUnites_delegueAuRepository() {
        when(uniteRepository.count()).thenReturn(1260L);
        assertThat(service.totalUnites()).isEqualTo(1260L);
    }
}
