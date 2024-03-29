package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.sourcecode;

import com.azure.ai.openai.models.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.ChatBot;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.Configuration;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.List;
import java.util.Stack;

@Getter
public class CurlSourceCodeGenerator implements ISourceCodeGenerator {
    private final String name = "Curl";
    private final String language = "bash";

    @SneakyThrows
    @Override
    public String generateCode(final ChatBot chatBot) {
        final String endpoint = chatBot.getDeployment().getEndpoint();
        final Configuration config = chatBot.getConfiguration();
        final Stack<ChatMessage> messages = chatBot.getChatMessages();
        final ObjectMapper mapper = new ObjectMapper();
        final List<ObjectNode> nodes = messages.stream().map(m -> {
            final ObjectNode node = mapper.createObjectNode();
            node.put("role", m.getRole().toString());
            node.put("content", m.getContent());
            return node;
        }).toList();
        //noinspection deprecation
        return String.format("""
                curl "%s" \\
                  -H "Content-Type: application/json" \\
                  -H "api-key: YOUR_API_KEY" \\
                  -d "{
                  \\"messages\\": %s,
                  \\"max_tokens\\": %d,
                  \\"temperature\\": %.2f,
                  \\"frequency_penalty\\": %.1f,
                  \\"presence_penalty\\": %.1f,
                  \\"top_p\\": %.2f,
                  \\"stop\\": %s
                }"
                """,
            endpoint,
            StringEscapeUtils.escapeJson(mapper.writeValueAsString(nodes)),
            config.getMaxResponse(),
            config.getTemperature(),
            config.getFrequencyPenalty(),
            config.getPresencePenalty(),
            config.getTopP(),
            mapper.writeValueAsString(config.getStopSequences()));
    }
}
