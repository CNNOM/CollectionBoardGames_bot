package org.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.example.bot.BoardGameBot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BoardGameBotTests {
    private BoardGameBot bot;
    private static final String TEST_USER = "test_user";
    private static final String TEST_GAME = "ТестоваяИгра;Описание;Стратегия;2;4;60";
    private static final String TEST_SESSION = "ТестоваяИгра;Игрок1,Игрок2;Игрок1";
    private static final String INVALID_GAME = ";;Стратегия;2;6;120";
    private static final String INVALID_SESSION = "ТестоваяИгра;;Победитель";

    @BeforeEach
    public void setUp() {
        bot = new BoardGameBot();
        clearTestData();
    }

    @AfterEach
    public void tearDown() {
        clearTestData();
    }

    private void clearTestData() {
        try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase database = mongoClient.getDatabase("boardgames");
            database.getCollection("tasks_games").deleteMany(new Document("name", "ТестоваяИгра"));
            database.getCollection("tasks_sessions").deleteMany(new Document("gameName", "ТестоваяИгра"));
        } catch (Exception e) {
            System.err.println("Error cleaning test data: " + e.getMessage());
        }
    }

    @Test
    public void testAddGame() {
        String result = bot.processCommand(TEST_USER, "/addgame " + TEST_GAME);
        assertTrue(result.contains("Игра успешно добавлена"),
                "Ожидалось сообщение об успешном добавлении игры");
    }

    @Test
    public void testAddDuplicateGame() {
        bot.processCommand(TEST_USER, "/addgame " + TEST_GAME);
        String result = bot.processCommand(TEST_USER, "/addgame " + TEST_GAME);
        assertTrue(result.contains("Игра с таким названием существует"),
                "Ожидалось сообщение о существующей игре");
    }

    @Test
    public void testAddGameWithIncompleteData() {
        String result = bot.processCommand(TEST_USER, "/addgame " + INVALID_GAME);
        assertTrue(result.contains("Ошибка: не все обязательные поля заполнены"),
                "Ожидалось сообщение об ошибке из-за неполных данных");
    }

    @Test
    public void testViewGameHistory() {
        // Добавляем тестовые данные
        bot.processCommand(TEST_USER, "/addgame " + TEST_GAME);
        bot.processCommand(TEST_USER, "/addsession " + TEST_SESSION);

        String result = bot.processCommand(TEST_USER, "/history");
        assertTrue(result.contains("ТестоваяИгра") || result.contains("Игрок1"),
                "Ожидалось увидеть тестовую игру или игрока в истории");
    }

    @Test
    public void testViewGameStatistics() {
        // Подготовка данных
        bot.processCommand(TEST_USER, "/addgame " + TEST_GAME);
        bot.processCommand(TEST_USER, "/addsession " + TEST_SESSION);

        // Даем время на обновление данных в БД
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String result = bot.processCommand(TEST_USER, "/stats ТестоваяИгра");
        assertTrue(result.contains("Игрок1: 1 побед") || result.contains("Статистика побед"),
                "Ожидалось увидеть статистику по игроку Игрок1");
    }

    @Test
    public void testViewGameHistoryWithNoSessions() {
        String result = bot.processCommand(TEST_USER, "/history");
        assertTrue(result.contains("нет записей") || result.contains("пуст"),
                "Ожидалось сообщение об отсутствии сессий");
    }

    @Test
    public void testAddGameSession() {
        // Сначала добавляем игру
        bot.processCommand(TEST_USER, "/addgame " + TEST_GAME);

        String result = bot.processCommand(TEST_USER, "/addsession " + TEST_SESSION);
        assertTrue(result.contains("Сессия успешно добавлена"),
                "Ожидалось сообщение об успешном добавлении сессии");
    }

    @Test
    public void testAddGameSessionWithIncompleteData() {
        bot.processCommand(TEST_USER, "/addgame " + TEST_GAME);
        String result = bot.processCommand(TEST_USER, "/addsession " + INVALID_SESSION);
        assertTrue(result.contains("Ошибка: не все обязательные поля заполнены"),
                "Ожидалось сообщение об ошибке из-за неполных данных");
    }

    @Test
    public void testSortGamesByName() {
        // Добавляем тестовую игру
        bot.processCommand(TEST_USER, "/addgame " + TEST_GAME);

        String result = bot.processCommand(TEST_USER, "/games");
        assertTrue(result.contains("ТестоваяИгра"),
                "Ожидалось увидеть тестовую игру в списке");
    }

    @Test
    public void testSortGamesWithEmptyCollection() {
        String result = bot.processCommand(TEST_USER, "/games");
        assertTrue(result.contains("пуст") || result.contains("нет игр"),
                "Ожидалось сообщение о пустой коллекции");
    }
}