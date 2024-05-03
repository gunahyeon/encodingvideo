package com.gu.encodingvideo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Controller;

@Slf4j
@RequiredArgsConstructor
@Controller
public class StompChannelInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String simpleDestination = (String) message.getHeaders().get("simpDestination");

        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            log.info("구독 {}", simpleDestination);
        } else if (StompCommand.DISCONNECT == accessor.getCommand()) {
            log.info("연결해제");
        } else if (StompCommand.CONNECT == accessor.getCommand()) {
            /* 세션 ID를 사용자 식별자로 사용 */
            log.info("연결성공");
        } else if (StompCommand.SEND == accessor.getCommand()) {
            log.info("전송");
        }

        return message;
    }
}