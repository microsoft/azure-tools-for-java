package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.sourcecode;

import com.azure.ai.openai.models.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.ChatBot;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.Configuration;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Stack;

@Getter
public class JsonSourceCodeGenerator implements ISourceCodeGenerator {
    private final String name = "JSON";
    private final String language = "json";

    @SneakyThrows
    @Override
    public String generateCode(final ChatBot chatBot) {
        final Configuration config = chatBot.getConfiguration();
        final Stack<ChatMessage> messages = chatBot.getChatMessages();
        final ObjectMapper mapper = new ObjectMapper();
        final List<ObjectNode> nodes = messages.stream().map(m -> {
            final ObjectNode node = mapper.createObjectNode();
            node.put("role", m.getRole().toString());
            node.put("content", m.getContent());
            return node;
        }).toList();
        return String.format("""
                {
                  "messages": %s,
                  "temperature": %.2f,
                  "top_p": %.2f,
                  "frequency_penalty": %.1f,
                  "presence_penalty": %.1f,
                  "max_tokens": %d,
                  "stop": %s
                }
                """,
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(nodes),
            config.getTemperature(),
            config.getTopP(),
            config.getFrequencyPenalty(),
            config.getPresencePenalty(),
            config.getMaxResponse(),
            mapper.writeValueAsString(config.getStopSequences()));
    }
}
