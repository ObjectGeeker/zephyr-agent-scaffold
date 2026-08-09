package com.object.ai.agent.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智能体对话请求体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentChatRequest {

    private String agentId;

    private String sessionId;

    private String userId;

    private String baseUrl;

    private String apiKey;

    private String model;

    private List<Content.Text> texts;

    private List<Content.File> files;

    private List<Content.InlineData> inlineDatas;

    @Data
    public static class Content {

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Text {
            private String message;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class File {
            private String fileUri;
            private String mimeType;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class InlineData {
            private byte[] bytes;
            private String mimeType;
        }

    }

}
