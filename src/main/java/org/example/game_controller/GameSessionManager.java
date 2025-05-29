package org.example.game_controller;

import org.example.BoardGame;
import org.example.GameSession;
import org.example.dao.BoardGameDao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class GameSessionManager {
    private final BoardGameDao boardGameDao;
    private final List<BoardGame> allGames;

    public GameSessionManager(BoardGameDao boardGameDao) {
        this.boardGameDao = boardGameDao;
        this.allGames = boardGameDao.getAllGames(); // Кэшируем только игры, так как они редко меняются
    }

    public void addPlayedGame(String gameName, String winner, List<String> players) {
        if (gameName == null || gameName.isEmpty()) {
            throw new IllegalArgumentException("Выберите игру");
        }

        if (winner == null || winner.isEmpty()) {
            throw new IllegalArgumentException("Укажите победителя");
        }

        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("Укажите игроков");
        }

        if (!players.contains(winner)) {
            throw new IllegalArgumentException("Победитель должен быть в списке игроков");
        }

        BoardGame game = allGames.stream()
                .filter(g -> g.getName().equalsIgnoreCase(gameName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Игра не найдена"));

        GameSession session = new GameSession(
                null,
                game.getId(),
                game.getName(),
                LocalDateTime.now(),
                players,
                winner,
                GameSession.GameStatus.IN_PROGRESS
        );

        boardGameDao.addGameSession(session);
    }

    public void updateGameStatuses() {
        // Получаем свежие данные из БД каждый раз
        List<GameSession> sessions = boardGameDao.getGameHistory();

        for (GameSession session : sessions) {
            if (session.getDateTime().toLocalDate().isBefore(LocalDate.now().minusDays(1)) &&
                    session.getStatus() != GameSession.GameStatus.PLAYED) {
                session.setStatus(GameSession.GameStatus.PLAYED);
                boardGameDao.updateGameSessionStatus(session);
            }
        }
    }

    public String getRecentSessions(int limit) {
        List<GameSession> history = boardGameDao.getGameHistory();

        if (history.isEmpty()) {
            return "История игр пуста.";
        }

        history.sort((s1, s2) -> s2.getDateTime().compareTo(s1.getDateTime()));

        StringBuilder sb = new StringBuilder("Последние игры:\n");
        history.stream()
                .limit(limit)
                .forEach(session -> sb.append(session.toString()).append("\n\n"));

        return sb.toString();
    }

    public String addSession(String input) {
        if (input == null || input.isEmpty()) {
            return "ℹ️ Укажите параметры: /addsession Игра;Игроки (через запятую);Победитель";
        }
        try {
            String[] parts = input.split(";");
            if (parts.length < 3) {
                return "❌ Неверный формат. Используйте: /addsession Игра;Игроки (через запятую);Победитель";
            }

            String gameName = parts[0].trim();
            String playersStr = parts[1].trim();
            String winner = parts[2].trim();

            List<String> players = Arrays.stream(playersStr.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());

            addPlayedGame(gameName, winner, players);
            return "✅ Сессия игры \"" + gameName + "\" успешно добавлена!";
        } catch (IllegalArgumentException e) {
            return "❌ Ошибка: " + e.getMessage();
        } catch (Exception e) {
            return "❌ Произошла ошибка при добавлении сессии";
        }
    }

    public String getWinStatistics(String gameName) {
        if (gameName == null || gameName.isEmpty()) {
            return "ℹ️ Укажите название игры: /stats [название игры]";
        }

        // Получаем все сессии по игре
        List<GameSession> gameSessions = boardGameDao.getGameHistory().stream()
                .filter(session -> session.getGameName().equalsIgnoreCase(gameName))
                .collect(Collectors.toList());

        if (gameSessions.isEmpty()) {
            return "ℹ️ Нет данных по игре \"" + gameName + "\"";
        }

        // Собираем всех уникальных игроков
        Set<String> allPlayers = new HashSet<>();
        gameSessions.forEach(session -> allPlayers.addAll(session.getPlayers()));

        // Статистика побед
        Map<String, Integer> winStats = new HashMap<>();
        allPlayers.forEach(player -> winStats.put(player, 0)); // Инициализируем всех игроков с 0 побед

        gameSessions.forEach(session -> {
            String winner = session.getWinner();
            winStats.put(winner, winStats.getOrDefault(winner, 0) + 1);
        });

        // Дополнительная статистика
        long totalGames = gameSessions.size();
        long completedGames = gameSessions.stream()
                .filter(s -> s.getStatus() == GameSession.GameStatus.PLAYED)
                .count();

        // Сортировка по количеству побед (по убыванию)
        List<Map.Entry<String, Integer>> sortedStats = new ArrayList<>(winStats.entrySet());
        sortedStats.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        // Формируем результат
        StringBuilder sb = new StringBuilder();
        sb.append("🏆 Статистика по игре \"").append(gameName).append("\":\n\n");
        sb.append("Всего сессий: ").append(totalGames).append("\n");
        sb.append("Завершённых: ").append(completedGames).append("\n");
        sb.append("В процессе: ").append(totalGames - completedGames).append("\n\n");
        sb.append("Все игроки:\n");

        for (Map.Entry<String, Integer> entry : sortedStats) {
            String player = entry.getKey();
            int wins = entry.getValue();
            double winPercentage = totalGames > 0 ? (double) wins / totalGames * 100 : 0;

            sb.append(String.format("• %s: %d побед (%.1f%%)%n",
                    player, wins, winPercentage));
        }

        return sb.toString();
    }
}