package com.study;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** 关键路径集成测试:注册→登陆→建文章→列表→详情(浏览量+1);跑在 Testcontainers MySQL+Redis 上。 */
@AutoConfigureMockMvc
class AuthArticleFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void fullFlow() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"));

        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(loginBody).get("token").asText();

        // 无 token → 401
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        // 建文章
        String createBody = mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"集成测试标题\",\"content\":\"正文\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("集成测试标题"))
                .andExpect(jsonPath("$.authorUsername").value("alice"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(createBody).get("id").asLong();

        // 列表能查到
        mockMvc.perform(get("/api/articles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // 详情:Redis 浏览量 +1
        mockMvc.perform(get("/api/articles/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(1));
    }
}
