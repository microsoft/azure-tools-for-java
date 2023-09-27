package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.sourcecode;

import com.azure.ai.openai.models.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.ChatBot;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.Configuration;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Stack;
import java.util.stream.Collectors;

@Getter
public class JavaSourceCodeGenerator implements ISourceCodeGenerator {
    private final String name = "Java";
    private final String language = "java";

    @SneakyThrows
    @SuppressWarnings("deprecation")
    @Override
    public String generateCode(final ChatBot chatBot) {
        final CognitiveDeployment deployment = chatBot.getDeployment();
        final String endpoint = deployment.getParent().getEndpoint();
        final Configuration config = chatBot.getConfiguration();
        final Stack<ChatMessage> messages = chatBot.getChatMessages();
        final ObjectMapper mapper = new ObjectMapper();
        //noinspection deprecation
        final String msgs = messages.stream()
            .map(m -> String.format("        chatMessages.add(new ChatMessage(ChatRole.%s, \"%s\"));",
                m.getRole().toString().toUpperCase(), StringEscapeUtils.escapeJava(m.getContent())))
            .collect(Collectors.joining("\n"));
        final String content = "import com.azure.ai.openai.OpenAIClient;\n" +
                "import com.azure.ai.openai.OpenAIClientBuilder;\n" +
                "import com.azure.ai.openai.models.ChatChoice;\n" +
                "import com.azure.ai.openai.models.ChatCompletions;\n" +
                "import com.azure.ai.openai.models.ChatCompletionsOptions;\n" +
                "import com.azure.ai.openai.models.ChatMessage;\n" +
                "import com.azure.ai.openai.models.ChatRole;\n" +
                "import com.azure.core.credential.AzureKeyCredential;\n" +
                "            \n" +
                "import java.util.Arrays;\n" +
                "import java.util.ArrayList;\n" +
                "import java.util.List;\n" +
                "            \n" +
                "/**\n" +
                " * Note: The Azure OpenAI client library for Java is in preview.\n" +
                " * Add the library dependency in Maven:\n" +
                " * <pre>\n" +
                " * &lt;dependency&gt;\n" +
                " *     &lt;groupId&gt;com.azure&lt;/groupId&gt;\n" +
                " *     &lt;artifactId&gt;azure-ai-openai&lt;/artifactId&gt;\n" +
                " *     &lt;version&gt;1.0.0-beta.3&lt;/version&gt;\n" +
                " * &lt;/dependency&gt;\n" +
                " * </pre>\n" +
                " */\n" +
                "public class Example {\n" +
                "    public static void main(String[] args) {\n" +
                "        String endpoint = \"%s\";\n" +
                "        String azureOpenaiKey = \"YOUR_KEY\";\n" +
                "        String deploymentOrModelId = \"%s\";\n" +
                "            \n" +
                "        OpenAIClient client = new OpenAIClientBuilder()\n" +
                "            .endpoint(endpoint)\n" +
                "            .credential(new AzureKeyCredential(azureOpenaiKey))\n" +
                "            .buildClient();\n" +
                "            \n" +
                "        List<ChatMessage> chatMessages = new ArrayList<>();\n" +
                "%s\n" +
                "\n" +
                "        final ChatCompletionsOptions options = new ChatCompletionsOptions(chatMessages);\n" +
                "        options.setMaxTokens(%d);\n" +
                "        options.setTemperature(%.2f);\n" +
                "        options.setFrequencyPenalty(%.1f);\n" +
                "        options.setPresencePenalty(%.1f);\n" +
                "        options.setTopP(%.2f);\n" +
                "        options.setStop(Arrays.asList(%s));\n" +
                "        ChatCompletions chatCompletions = client.getChatCompletions(deploymentOrModelId, options);\n" +
                "            \n" +
                "        for (ChatChoice choice : chatCompletions.getChoices()) {\n" +
                "            ChatMessage message = choice.getMessage();\n" +
                "            System.out.println(\"Message:\");\n" +
                "            System.out.println(message.getContent());\n" +
                "        }\n" +
                "    }\n" +
                "}";
        return String.format(content, endpoint, deployment.getName(), msgs,
            config.getMaxResponse(),
            config.getTemperature(),
            config.getFrequencyPenalty(),
            config.getPresencePenalty(),
            config.getTopP(),
            config.getStopSequences().stream().map(s -> "\"" + StringEscapeUtils.escapeJava(s) + "\"")
                .collect(Collectors.joining(",")));
    }
}
