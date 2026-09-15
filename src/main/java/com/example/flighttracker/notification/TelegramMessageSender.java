package com.example.flighttracker.notification;

public interface TelegramMessageSender {

    /**
     * Send an HTML formatted Telegram message to the specified chat ID.
     *
     * @param chatId      recipient chat ID
     * @param htmlMessage HTML formatted message string
     * @return true if successfully delivered, false otherwise
     */
    boolean sendMessage(Long chatId, String htmlMessage);
}
