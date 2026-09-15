package com.example.flighttracker.telegram;

import com.example.flighttracker.notification.TelegramMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "telegram.bot-enabled", havingValue = "false")
public class MockTelegramBot implements TelegramMessageSender {

    private static final Logger log = LoggerFactory.getLogger(MockTelegramBot.class);

    @Override
    public boolean sendMessage(Long chatId, String htmlMessage) {
        log.info("MockTelegramBot: message for chatId={} -> {}", chatId, htmlMessage);
        return true;
    }
}
