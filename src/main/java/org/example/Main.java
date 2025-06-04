package org.example;

import org.example.bot.BoardGameBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    private static final String BOT_TOKEN = "8146130795:AAHDBCf6xfVsblYlK1-BZKAcFdS1aUPCEa8";
    private static final String BOT_USERNAME = "TabletopGameAdvisorVSTU_bot";

    public static void main(String[] args) {
        try {
            // Создаем экземпляр TelegramBotsApi
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            // Создаем и регистрируем нашего бота
            botsApi.registerBot(new BoardGameBot() {
                @Override
                public String getBotUsername() {
                    return BOT_USERNAME;
                }

                @Override
                public String getBotToken() {
                    return BOT_TOKEN;
                }
            });

            System.out.println("Бот успешно запущен! Имя бота: " + BOT_USERNAME);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.err.println("Ошибка при запуске бота: " + e.getMessage());
        }
    }
}