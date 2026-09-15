package com.fiapx.api.infrastructure.messaging;

import com.fiapx.api.infrastructure.messaging.dto.VideoProcessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.video}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.video-process}")
    private String routingKey;

    public void publishVideoProcessEvent(VideoProcessEvent event) {
        log.info("Publicando evento de processamento para o vídeo ID: {}", event.getVideoId());
        rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
        log.info("Evento publicado com sucesso para o vídeo ID: {}", event.getVideoId());
    }
}
