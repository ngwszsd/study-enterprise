package com.study

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 关键路径集成测试:注册→登陆→建文章→列表→详情(浏览量+1);跑在 Testcontainers MySQL+Redis 上。 */
@AutoConfigureMockMvc
class AuthArticleFlowIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun fullFlow() {
        mockMvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"alice","password":"secret123"}"""),
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("alice"))

        val loginBody = mockMvc.perform(
            post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"alice","password":"secret123"}"""),
        )
            .andExpect(status().isOk())
            .andReturn().response.contentAsString
        val token = objectMapper.readTree(loginBody).get("token").asText()

        // 无 token → 401
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())

        val createBody = mockMvc.perform(
            post("/api/articles").header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"集成测试标题","content":"正文"}"""),
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("集成测试标题"))
            .andExpect(jsonPath("$.authorUsername").value("alice"))
            .andReturn().response.contentAsString
        val id = objectMapper.readTree(createBody).get("id").asLong()

        mockMvc.perform(get("/api/articles").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))

        // 详情:Redis 浏览量 +1
        mockMvc.perform(get("/api/articles/$id").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.viewCount").value(1))
    }
}
