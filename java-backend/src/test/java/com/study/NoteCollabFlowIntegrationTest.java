package com.study;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** 协作笔记关键路径:注册→建笔记→签发协作 token→内部保存/读取 Yjs 快照。 */
@AutoConfigureMockMvc
class NoteCollabFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void noteCollabFlow() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"note_alice\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"note_alice\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginBody).get("token").asText();

        String createBody = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"协作笔记\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("协作笔记"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andReturn().getResponse().getContentAsString();
        long noteId = objectMapper.readTree(createBody).get("id").asLong();

        mockMvc.perform(post("/api/notes/" + noteId + "/collab-token")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.docName").value("note:" + noteId))
                .andExpect(jsonPath("$.role").value("OWNER"));

        mockMvc.perform(put("/api/notes/internal/" + noteId + "/document")
                        .header("X-Collab-Secret", "dev-collab-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"AQID\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notes/internal/" + noteId + "/document")
                        .header("X-Collab-Secret", "dev-collab-secret"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"state\":\"AQID\"}"));
    }
}
