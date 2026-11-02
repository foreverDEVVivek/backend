package devPilot.backend.services.ai;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import devPilot.backend.dto.ChatMessageResponse;
import devPilot.backend.dto.CitationDto;
import devPilot.backend.entity.ChatMessage;
import devPilot.backend.entity.MessageRole;
import devPilot.backend.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.Exceptions;

/**
 * Generation step: call the LLM (Gemini) via Spring AI and stream tokens to the browser over SSE.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatStreamHandler {

    private final ChatModel chatModel;
    private final ChatMessageRepository chatMessageRepository;
    private final CitationMapper citationMapper;

    public SseEmitter stream(
            UUID sessionId,
            ChatMessageResponse savedUserMessage,
            List<CitationDto> citations,
            String systemPrompt,
            String userPrompt) {

        SseEmitter emitter = new SseEmitter(RagSettings.STREAM_TIMEOUT_MS);
        StringBuilder fullReply = new StringBuilder();

        try {
            emitter.send(SseEmitter.event()
                    .name("user_message")
                    .data(savedUserMessage));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
            return emitter;
        }

        Disposable subscription = ChatClient.builder(chatModel)
                .build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
                .subscribe(
                        token -> {
                            fullReply.append(token);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("token")
                                        .data(token, MediaType.APPLICATION_JSON));
                            } catch (IOException | IllegalStateException ex) {
                                // Client is gone; propagate so Reactor cancels the LLM stream
                                throw Exceptions.propagate(ex);
                            }
                        },
                        err -> handleError(emitter, err),
                        () -> completeStream(emitter, sessionId, fullReply, citations));

        // Cancel the LLM call if the client leaves, times out, or errors
        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(subscription::dispose);
        emitter.onError(e -> subscription.dispose());

        return emitter;
    }

    private void completeStream(
            SseEmitter emitter,
            UUID sessionId,
            StringBuilder fullReply,
            List<CitationDto> citations) {

        // Save first, so the reply survives even if the client has disconnected
        ChatMessage assistant;
        try {
            assistant = chatMessageRepository.save(ChatMessage.builder()
                    .sessionId(sessionId)
                    .role(MessageRole.ASSISTANT)
                    .content(fullReply.toString())
                    .citations(citationMapper.toJson(citations))
                    .build());
        } catch (Exception ex) {
            handleError(emitter, ex);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("assistant_message")
                    .data(toMessageResponse(assistant)));
            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
            emitter.complete();
        } catch (Exception ex) {
            log.debug("Client disconnected before final events; reply was already saved");
        }
    }

    private void handleError(SseEmitter emitter, Throwable err) {
        if (isClientAbort(err)) {
            log.debug("Client disconnected during stream");
            return;
        }
        log.error("Chat stream error", err);
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("The AI service failed. Please try again."));
        } catch (Exception ignored) {
            // connection already closed
        }
        emitter.complete();
    }

    private boolean isClientAbort(Throwable t) {
        while (t != null) {
            if (t instanceof IOException
                    || t instanceof AsyncRequestNotUsableException
                    || t instanceof IllegalStateException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                citationMapper.fromJson(message.getCitations()),
                message.getCreatedAt());
    }
}