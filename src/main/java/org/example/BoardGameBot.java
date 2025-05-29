package org.example;

import org.example.dao.BoardGameDao;
import org.example.dao.BoardGameDaoJsonImpl;
import org.example.game_controller.GameManager;
import org.example.game_controller.GameSessionManager;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class BoardGameBot extends TelegramLongPollingBot {
    private final GameManager gameManager;
    private final GameSessionManager sessionManager;

    public BoardGameBot() {
        BoardGameDao dao = new BoardGameDaoJsonImpl("data");
        this.gameManager = new GameManager(dao);
        this.sessionManager = new GameSessionManager(dao);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String chatId = message.getChatId().toString();
            String text = message.getText();

            try {
                String response = processCommand(text);
                sendMessage(chatId, response);
            } catch (Exception e) {
                sendMessage(chatId, "Ошибка: " + e.getMessage());
            }
        }
    }

    private String processCommand(String command) {
        if (command.equals("/games")) {
            return gameManager.listAllGames();
        } else if (command.equals("/history")) {
            return sessionManager.getRecentSessions(5);
        }
        return "Неизвестная команда. Доступные команды: /games, /history";
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "YourBoardGameBot";
    }

    @Override
    public String getBotToken() {
        return "YOUR_BOT_TOKEN";
    }
}