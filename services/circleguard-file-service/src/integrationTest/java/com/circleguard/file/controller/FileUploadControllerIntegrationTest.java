package com.circleguard.file.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica el endpoint de upload del file-service end-to-end levantando el
 * contexto Spring completo (sin S3 real, ya que el servicio actualmente usa
 * almacenamiento local). El test confirma que el multipart se acepta y el
 * servicio persiste el archivo devolviendo el nombre asignado.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FileUploadControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadsFileAndReturnsAssignedFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "medical-certificate.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/files/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value(notNullValue()));
    }

    @Test
    void rejectsRequestWithoutFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/files/upload"))
                .andExpect(status().is4xxClientError());
    }
}
