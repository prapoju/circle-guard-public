package com.circleguard.form.service;

import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.repository.QuestionnaireRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionnaireServiceTest {

    @Mock
    private QuestionnaireRepository repository;

    @InjectMocks
    private QuestionnaireService service;

    private Questionnaire activeQuestionnaire;
    private Questionnaire inactiveQuestionnaire;

    @BeforeEach
    void setUp() {
        activeQuestionnaire = Questionnaire.builder()
                .id(UUID.randomUUID())
                .title("Health Survey v2")
                .version(2)
                .isActive(true)
                .build();

        inactiveQuestionnaire = Questionnaire.builder()
                .id(UUID.randomUUID())
                .title("Health Survey v1")
                .version(1)
                .isActive(false)
                .build();
    }

    @Test
    void testGetActiveQuestionnaire_ReturnsActiveQuestionnaire() {
        when(repository.findFirstByIsActiveTrueOrderByVersionDesc())
                .thenReturn(Optional.of(activeQuestionnaire));

        Optional<Questionnaire> result = service.getActiveQuestionnaire();

        assertTrue(result.isPresent());
        assertEquals("Health Survey v2", result.get().getTitle());
        assertTrue(result.get().getIsActive());
        assertEquals(2, result.get().getVersion());
        verify(repository).findFirstByIsActiveTrueOrderByVersionDesc();
    }

    @Test
    void testActivateQuestionnaire_DeactivatesOthers() {
        UUID targetId = activeQuestionnaire.getId();
        
        when(repository.findAll()).thenReturn(List.of(inactiveQuestionnaire, activeQuestionnaire));
        when(repository.findById(targetId)).thenReturn(Optional.of(activeQuestionnaire));
        when(repository.save(any(Questionnaire.class))).thenAnswer(inv -> inv.getArgument(0));

        service.activateQuestionnaire(targetId);

        verify(repository, times(2)).save(argThat(q -> {
            if (q.getId().equals(targetId)) {
                assertTrue(q.getIsActive(), "Target questionnaire should be activated");
            } else {
                assertFalse(q.getIsActive(), "Other questionnaires should be deactivated");
            }
            return true;
        }));
    }
}