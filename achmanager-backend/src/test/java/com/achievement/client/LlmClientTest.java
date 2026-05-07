package com.achievement.client;

import com.achievement.config.LlmProperties;
import com.achievement.constant.LlmUsage;
import com.achievement.domain.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class LlmClientTest {

    private LlmClient createClient(String baseUrl, String apiKey, String modelName) {
        LlmProperties props = new LlmProperties();
        LlmProperties.ModelConfig modelConfig = new LlmProperties.ModelConfig();
        modelConfig.setBaseUrl(baseUrl);
        modelConfig.setApiKey(apiKey);
        modelConfig.setModelName(modelName);
        modelConfig.setMaxTokens(1024);
        modelConfig.setTemperature(0.7);
        props.setModels(Map.of("default", modelConfig));
        props.setUsage(Map.of("default", "default"));
        return new LlmClient(props);
    }

    @Test
    void testChatCompletion() {
        // 从环境变量读取，未设置则跳过
        String baseUrl = System.getenv("LLM_TEST_BASE_URL");
        String apiKey = System.getenv("LLM_TEST_API_KEY");
        String model = System.getenv("LLM_TEST_MODEL");

        if (baseUrl == null || apiKey == null || model == null) {
            log.info("跳过 LLM 测试：未设置 LLM_TEST_BASE_URL / LLM_TEST_API_KEY / LLM_TEST_MODEL 环境变量");
            return;
        }

        LlmClient client = createClient(baseUrl, apiKey, model);

        List<ChatMessage> messages = List.of(
                ChatMessage.system("You are a helpful assistant. Reply concisely."),
                ChatMessage.user("Say 'hello world' and nothing else.")
        );

        String response = client.chatCompletion(LlmUsage.DEFAULT, messages);

        assertNotNull(response, "响应不应为 null");
        assertFalse(response.isBlank(), "响应不应为空");
        log.info("LLM 响应: {}", response);
    }
}
