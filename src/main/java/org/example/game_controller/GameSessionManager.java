package org.example.game_controller;

import org.example.BoardGame;
import org.example.GameSession;
import org.example.PlayerStats;
import org.example.dao.BoardGameDao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameSessionManager {
    private BoardGameDao boardGameDao;
    private List<GameSession> gameHistory;
    private List<BoardGame> allGames;

    public GameSessionManager(BoardGameDao boardGameDao) {
        this.boardGameDao = boardGameDao;
        this.gameHistory = boardGameDao.getGameHistory();
        this.allGames = boardGameDao.getAllGames();
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
        gameHistory = boardGameDao.getGameHistory();
    }

    public void updateGameStatuses() {
        for (GameSession session : gameHistory) {
            if (session.getDateTime().toLocalDate().isBefore(LocalDate.now().minusDays(1)) &&
                    session.getStatus() != GameSession.GameStatus.PLAYED) {
                session.setStatus(GameSession.GameStatus.PLAYED);
                boardGameDao.updateGameSessionStatus(session);
            }
        }
    }

    public List<GameSession> getGameHistory() {
        return gameHistory;
    }

    public String getRecentSessions(int limit) {
        if (gameHistory.isEmpty()) {
            return "История игр пуста.";
        }

        gameHistory.sort((s1, s2) -> s2.getDateTime().compareTo(s1.getDateTime()));

        StringBuilder sb = new StringBuilder("Последние игры:\n");
        gameHistory.stream()
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
}