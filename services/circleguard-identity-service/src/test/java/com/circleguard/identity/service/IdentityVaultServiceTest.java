package com.circleguard.identity.service;

import com.circleguard.identity.model.IdentityMapping;
import com.circleguard.identity.repository.IdentityMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityVaultServiceTest {

    @Mock
    private IdentityMappingRepository repository;

    @InjectMocks
    private IdentityVaultService vaultService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(vaultService, "hashSalt", "test-salt");
    }

    @Test
    void testGetOrCreateAnonymousId_ExistingUser_ReturnsExisting() {
        UUID existingAnonId = UUID.randomUUID();
        IdentityMapping existingMapping = IdentityMapping.builder()
                .anonymousId(existingAnonId)
                .realIdentity("juan@university.edu")
                .identityHash("abc123hash")
                .salt("salt123")
                .build();

        when(repository.findByIdentityHash(any())).thenReturn(Optional.of(existingMapping));

        UUID result = vaultService.getOrCreateAnonymousId("juan@university.edu");

        assertEquals(existingAnonId, result);
        verify(repository, never()).save(any());
    }

    @Test
    void testResolveRealIdentity_NotFound_Throws404() {
        UUID randomId = UUID.randomUUID();
        when(repository.findById(randomId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> vaultService.resolveRealIdentity(randomId)
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void testComputeHash_IsDeterministic() throws Exception {
        Method computeHashMethod = IdentityVaultService.class.getDeclaredMethod("computeHash", String.class);
        computeHashMethod.setAccessible(true);

        String input = "test@university.edu";
        String hash1 = (String) computeHashMethod.invoke(vaultService, input);
        String hash2 = (String) computeHashMethod.invoke(vaultService, input);

        assertEquals(hash1, hash2);
    }
}