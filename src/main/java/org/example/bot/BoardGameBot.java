package org.example.bot;

import org.example.GameSession;
import org.example.dao.BoardGameDao;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BoardGameBot extends TelegramLongPollingBot {

    private GameManager gameManager;
    private GameSessionManager sessionManager;
    private BoardGameDao dao;
    private String currentStorageType = "mongodb"; // default storage
    private List<GameSession> gameHistory;

    public BoardGameBot() {
        initDao(currentStorageType);
        this.gameHistory = dao.getGameHistory();
    }

    private void initDao(String storageType) {
        this.dao = DaoFactory.createTaskDao(storageType);
        this.gameManager = new GameManager(dao);
        this.sessionManager = new GameSessionManager(dao);
        this.currentStorageType = storageType;
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

    public String processCommand(String chatId, String command) {
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
                case "/history":
                    return sessionManager.getRecentSessions(5);
                case "/addsession":
                    String result = sessionManager.addSession(args);
                    sessionManager.updateGameStatuses();
                    return result;
                case "/stats":
                    return sessionManager.getWinStatistics(args);
                case "/setstorage":
                    return setStorageType(args);
                case "/filterdate":
                    return filterByDate(args);
                case "/filterstatus":
                    return filterByStatus(args);
                case "/filtergame":
                    return gameManager.getGameInfo(args);

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
        return "🎲 Добро пожаловать в Tabletop Game Advisor!\n\n" +
                "Я помогу вам управлять коллекцией настольных игр и отслеживать игровые сессии.\n\n" +
                getHelpMessage();
    }

    private String getHelpMessage() {
        return "📋 Доступные команды:\n\n" +
                "🎮 Игры:\n" +
                "/games - Список всех игр\n" +
                "/addgame [название;описание; категория;мин_игроков;макс_игроков;время] - Добавить игру\n\n" +
                "📅 Сессии:\n" +
                "/history - Последние 5 игровых сессий\n" +
                "/addsession [игра;игроки;победитель] - Добавить сессию\n" +
                "/stats [название] - Статистика побед по игре\n\n" +
                "⚙️ Настройки:\n" +
                "/setstorage [memory|mongodb|json] - Изменить источник данных\n\n" +
                "Текущее хранилище: " + currentStorageType + "\n\n" +
                "🔍 Фильтры:\n" +
                "/filterdate [начало;конец] - Фильтр по дате (формат: ГГГГ-ММ-ДД)\n" +
                "/filterstatus [STATUS] - Фильтр по статусу (IN_PROGRESS, PLAYED)\n" +
                "/filtergame [название] - Фильтр по названию игры\n" +
                "❓ Помощь:\n" +
                "/help - Справка по командам";
    }

    private String setStorageType(String storageType) {
        try {
            initDao(storageType);
            return "✅ Источник данных изменен на: " + storageType +
                    "\nТекущее хранилище: " + currentStorageType;
        } catch (Exception e) {
            return "❌ Ошибка при смене хранилища: " + e.getMessage() +
                    "\nДоступные варианты: memory, mongodb, json";
        }
    }

    private String filterByDate(String dateArgs) {
        try {
            String[] dates = dateArgs.split(";");
            LocalDate fromDate = dates.length > 0 && !dates[0].isEmpty() ? LocalDate.parse(dates[0]) : null;
            LocalDate toDate = dates.length > 1 && !dates[1].isEmpty() ? LocalDate.parse(dates[1]) : null;

            List<GameSession> filteredSessions = gameHistory.stream()
                    .filter(session -> fromDate == null || !session.getDateTime().toLocalDate().isBefore(fromDate))
                    .filter(session -> toDate == null || !session.getDateTime().toLocalDate().isAfter(toDate))
                    .collect(Collectors.toList());

            return formatFilteredSessions("📅 Сессии по дате:", filteredSessions);
        } catch (Exception e) {
            return "⚠️ Ошибка формата. Используйте: /filterdate [начальная_дата;конечная_дата]\n" +
                    "Пример: /filterdate 2023-01-01;2023-12-31";
        }
    }

    private String filterByStatus(String statusArg) {
        try {
            GameSession.GameStatus status = GameSession.GameStatus.valueOf(statusArg.trim().toUpperCase());
            List<GameSession> filteredSessions = gameHistory.stream()
                    .filter(session -> session.getStatus() == status)
                    .collect(Collectors.toList());

            return formatFilteredSessions("🏆 Сессии по статусу '" + status + "':", filteredSessions);
        } catch (IllegalArgumentException e) {
            return "⚠️ Неверный статус. Доступные статусы:\n" +
                    Arrays.stream(GameSession.GameStatus.values())
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")) +
                    "\nПример: /filterstatus IN_PROGRESS";
        }
    }

    private String formatFilteredSessions(String header, List<GameSession> sessions) {
        if (sessions.isEmpty()) {
            return "🔍 Не найдено сессий по указанным критериям.";
        }

        StringBuilder result = new StringBuilder(header).append("\n\n");
        for (GameSession session : sessions) {
            result.append(session.toString()).append("\n\n");
        }
        return result.toString();
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.enableHtml(true);

        if (text.equals(getWelcomeMessage())) {
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

        // Первый ряд - основные команды
        KeyboardRow row1 = new KeyboardRow();
        row1.add("🎮 Список игр");
        row1.add("🔍 Поиск игры");
        row1.add("📊 Статистика");

        // Второй ряд - управление сессиями
        KeyboardRow row2 = new KeyboardRow();
        row2.add("📅 История игр");
        row2.add("➕ Добавить игру");
        row2.add("🎲 Добавить сессию");

        // Третий ряд - фильтры
        KeyboardRow row3 = new KeyboardRow();
        row3.add("📅 Фильтр по дате");
        row3.add("🏷 Фильтр по статусу");

        // Четвертый ряд - настройки и помощь
        KeyboardRow row4 = new KeyboardRow();
        row4.add("⚙️ Настройки");
        row4.add("❓ Помощь");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        keyboardMarkup.setSelective(true);

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
