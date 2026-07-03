package com.study

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 协作笔记关键路径:注册→建笔记→签发协作 token→内部保存/读取 Yjs 快照。 */
@AutoConfigureMockMvc
class NoteCollabFlowIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun noteCollabFlow() {
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"note_alice","password":"secret123"}"""),
        )
            .andExpect(status().isCreated)

        val loginBody = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"note_alice","password":"secret123"}"""),
        )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        val token = objectMapper.readTree(loginBody).get("token").asText()

        val createBody = mockMvc.perform(
            post("/api/notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"协作笔记"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("协作笔记"))
            .andExpect(jsonPath("$.role").value("OWNER"))
            .andReturn().response.contentAsString
        val noteId = objectMapper.readTree(createBody).get("id").asLong()

        mockMvc.perform(
            post("/api/notes/$noteId/collab-token")
                .header("Authorization", "Bearer $token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.docName").value("note:$noteId"))
            .andExpect(jsonPath("$.role").value("OWNER"))

        mockMvc.perform(
            put("/api/notes/internal/$noteId/document")
                .header("X-Collab-Secret", "dev-collab-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"state":"AQID"}"""),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            get("/api/notes/internal/$noteId/document")
                .header("X-Collab-Secret", "dev-collab-secret"),
        )
            .andExpect(status().isOk)
            .andExpect(content().json("""{"state":"AQID"}"""))
    }
}
