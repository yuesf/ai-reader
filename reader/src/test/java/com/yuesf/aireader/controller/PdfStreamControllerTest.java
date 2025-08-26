package com.yuesf.aireader.controller;

import com.yuesf.aireader.service.PdfStreamService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PdfStreamController.class)
class PdfStreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PdfStreamService pdfStreamService;

    @Test
    void health_ok() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void info_ok() throws Exception {
        Mockito.when(pdfStreamService.getPdfFileInfo("fid"))
                .thenReturn(Map.of(
                        "fileId", "fid",
                        "filename", "test.pdf",
                        "fileSize", 1024,
                        "totalChunks", 1,
                        "chunkSize", 1024,
                        "encryptionKey", "k",
                        "lastModified", "2025-01-01T00:00:00"
                ));

        mockMvc.perform(get("/v1/pdf/info/{fileId}", "fid"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileId").value("fid"))
                .andExpect(jsonPath("$.data.totalChunks").value(1));
    }

    @Test
    void chunk_ok() throws Exception {
        byte[] bytes = new byte[]{1,2,3};
        Mockito.when(pdfStreamService.getPdfChunk("fid", 0)).thenReturn(bytes);

        mockMvc.perform(get("/v1/pdf/chunk/{fileId}/{idx}", "fid", 0))
                .andExpect(status().isOk())
                .andExpect(header().string("X-File-Id", "fid"))
                .andExpect(header().string("X-Chunk-Index", "0"));
    }

//     @Test
//     void page_png_ok() throws Exception {
//         byte[] png = new byte[]{(byte)0x89, 'P', 'N', 'G'};
//         Mockito.when(pdfStreamService.renderPdfPageAsImage("fid", 1)).thenReturn(png);

//         mockMvc.perform(get("/v1/pdf/page/{fileId}/{page}", "fid", 1))
//                 .andExpect(status().isOk())
//                 .andExpect(header().string("X-File-Id", "fid"))
//                 .andExpect(header().string("X-Page", "1"))
//                 .andExpect(content().contentType(MediaType.IMAGE_PNG));
//     }
}
