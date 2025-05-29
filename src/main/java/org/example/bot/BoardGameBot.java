package org.example.bot;

import org.example.dao.BoardGameDao;
import org.example.dao.BoardGameDaoJsonImpl;
import org.example.dao.DaoFactory;
import org.example.game_controller.GameManager;
import org.example.game_controller.GameSessionManager;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

public class BoardGameBot extends TelegramLongPollingBot {
    private final GameManager gameManager;
    private final GameSessionManager sessionManager;
    private final BoardGameDao dao;

    public BoardGameBot() {
        this.dao = DaoFactory.createTaskDao("mongodb");
        this.gameManager = new GameManager(dao);
        this.sessionManager = new GameSessionManager(dao);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String chatId = message.getChatId().toString();
            String text = message.getText().trim();

            try {
                String response = processCommand(chatId, text);
                sendMessage(chatId, response);
            } catch (Exception e) {
                sendMessage(chatId, "⚠️ Ошибка: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private String processCommand(String chatId, String command) {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        try {
            switch (cmd) {
                case "/start":
                    return getWelcomeMessage();
                case "/help":
                    return getHelpMessage();
                case "/games":
                    return gameManager.listAllGames();
                case "/addgame":
                    return gameManager.addGame(args);
                case "/gameinfo":
                    return gameManager.getGameInfo(args);
                case "/history":
                    // Теперь всегда получаем свежие данные
                    return sessionManager.getRecentSessions(5);
                case "/addsession":
                    String result = sessionManager.addSession(args);
                    // Принудительно обновляем статусы после добавления
                    sessionManager.updateGameStatuses();
                    return result;
                case "/stats":
                    // Статистика теперь всегда актуальная
                    return sessionManager.getWinStatistics(args);
                default:
                    return "❌ Неизвестная команда. Введите /help для списка команд.";
            }
        } catch (Exception e) {
            System.err.println("Error processing command: " + command);
            e.printStackTrace();
            return "⚠️ Произошла ошибка при обработке команды";
        }
    }

    private String getWelcomeMessage() {
        return "🎲 Добро пожаловать в BoardGameBot!\n\n" +
                "Я помогу вам управлять коллекцией настольных игр и отслеживать игровые сессии.\n\n" +
                getHelpMessage();
    }

    private String getHelpMessage() {
        return "📋 Доступные команды:\n\n" +
                "🎮 Игры:\n" +
                "/games - Список всех игр\n" +
                "/gameinfo [название] - Информация об игре\n" +
                "/addgame [название;описание;категория;мин_игроков;макс_игроков;время] - Добавить игру\n\n" +
                "📅 Сессии:\n" +
                "/history - Последние 5 игровых сессий\n" +
                "/addsession [игра;игроки;победитель] - Добавить сессию\n" +
                "/stats [название] - Статистика побед по игре\n\n" +
                "❓ Помощь:\n" +
                "/help - Справка по командам";
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.enableHtml(true);

        // Добавляем клавиатуру для основных команд
        if (text.equals(getWelcomeMessage())){
            message.setReplyMarkup(createMainKeyboard());
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private ReplyKeyboardMarkup createMainKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Первый ряд кнопок
        KeyboardRow row1 = new KeyboardRow();
        row1.add("/games");
        row1.add("/gameinfo");
        row1.add("/history");

        // Второй ряд кнопок
        KeyboardRow row2 = new KeyboardRow();
        row2.add("/addgame");
        row2.add("/addsession");
        row2.add("/stats");

        // Третий ряд кнопок
        KeyboardRow row3 = new KeyboardRow();
        row3.add("/help");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        return keyboardMarkup;
    }

    @Override
    public String getBotUsername() {
        return "CollectionBoardGames_bot";
    }

    @Override
    public String getBotToken() {
        return "7978010225:AAGIC6g0LSgsdUzXfVwhMjL9N3DX2E3RfdU";
    }

    @Override
    public void onClosing() {
        try {
            dao.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onClosing();
    }
}