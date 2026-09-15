package com.example.flighttracker.telegram;

import com.example.flighttracker.notification.TelegramMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
@ConditionalOnProperty(name = "telegram.bot-enabled", havingValue = "true", matchIfMissing = true)
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer, TelegramMessageSender {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    private final String botToken;
    private final TelegramUpdateHandler updateHandler;
    private final TelegramClient telegramClient;

    public TelegramBot(
            @Value("${telegram.bot-token:mock_token}") String botToken,
            TelegramUpdateHandler updateHandler
    ) {
        this.botToken = botToken;
        this.updateHandler = updateHandler;
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update != null && update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String text = update.getMessage().getText();

            log.info("Received Telegram update from chatId={}: {}", chatId, text);
            String responseText = updateHandler.handleIncomingMessage(chatId, text);
            sendMessage(chatId, responseText);
        }
    }

    @Override
    public boolean sendMessage(Long chatId, String htmlMessage) {
        if (botToken == null || botToken.equals("mock_token") || botToken.isBlank()) {
            log.info("Mock Telegram bot active: message for chatId={} not sent over wire: {}", chatId, htmlMessage);
            return true;
        }

        try {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(htmlMessage)
                    .parseMode("HTML")
                    .build();

            telegramClient.execute(sendMessage);
            log.info("Successfully sent Telegram message to chatId={}", chatId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send Telegram message to chatId={}: {}", chatId, e.getMessage(), e);
            return false;
        }
    }
}
