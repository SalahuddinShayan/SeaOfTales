package com.seaoftales.content;

import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.service.NovelService;
import com.seaoftales.content.service.PublicChapterReaderService;
import com.seaoftales.content.service.PublicNovelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NovelCreationAuthorizationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NovelService novelService;

    @MockitoBean
    private PublicNovelService publicNovelService;

    @MockitoBean
    private PublicChapterReaderService publicChapterReaderService;

    @Test
    void anonymousUserCannotCreateNovel() throws Exception {
        mockMvc.perform(createNovelRequest())
                .andExpect(status().isForbidden());

        verifyNoInteractions(novelService);
    }

    @Test
    @WithMockUser(roles = "READER")
    void readerCannotCreateNovel() throws Exception {
        assertCreationForbidden();
    }

    @Test
    @WithMockUser(roles = "AUTHOR")
    void authorCannotCreateNovel() throws Exception {
        assertCreationForbidden();
    }

    @Test
    @WithMockUser(roles = "EDITOR")
    void editorCannotCreateNovel() throws Exception {
        assertCreationForbidden();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateNovel() throws Exception {
        when(novelService.createNovel(any(Novel.class), any(MultipartFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(createNovelRequest())
                .andExpect(status().isCreated());

        verify(novelService).createNovel(any(Novel.class), any(MultipartFile.class));
    }

    @Test
    void publicNovelReadingEndpointsRemainAccessible() throws Exception {
        mockMvc.perform(get("/api/novels/1/public"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/novels/1/chapters/1/public"))
                .andExpect(status().isOk());
    }

    private void assertCreationForbidden() throws Exception {
        mockMvc.perform(createNovelRequest())
                .andExpect(status().isForbidden());

        verifyNoInteractions(novelService);
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder createNovelRequest() {
        return multipart("/api/novels")
                .file(new MockMultipartFile("coverImage", new byte[0]))
                .param("title", "Test novel")
                .param("authorUsername", "TestAuthor")
                .param("sourceLanguage", "JAPANESE")
                .param("description", "<script>alert(1)</script>")
                .param("status", "ONGOING")
                .contentType(MediaType.MULTIPART_FORM_DATA);
    }
}
