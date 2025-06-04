package org.example.game_controller;

import org.example.BoardGame;
import org.example.GameSession;
import org.example.dao.BoardGameDao;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GameManager {
    private final BoardGameDao boardGameDao;
    private List<BoardGame> allGames;

    public GameManager(BoardGameDao boardGameDao) {
        this.boardGameDao = boardGameDao;
        refreshGames();
    }

    public void refreshGames() {
        this.allGames = boardGameDao.getAllGames();
    }

    // Основные методы управления играми

    public String addGame(String input) {
        try {
            String[] parts = input.split(";");
            if (parts.length < 6) {
                return "❌ Неверный формат. Используйте: /addgame Название;Описание;Категория;Мин_игроков;Макс_игроков;Среднее_время";
            }

            String name = parts[0].trim();
            String description = parts[1].trim();
            String category = parts[2].trim();
            int minPlayers = Integer.parseInt(parts[3].trim());
            int maxPlayers = Integer.parseInt(parts[4].trim());
            int avgTime = Integer.parseInt(parts[5].trim());

            addNewGame(name, description, category, minPlayers, maxPlayers, avgTime);
            return "✅ Игра \"" + name + "\" успешно добавлена в коллекцию!";
        } catch (NumberFormatException e) {
            return "❌ Ошибка: неверный формат числовых параметров (игроки/время)";
        } catch (IllegalArgumentException e) {
            return "❌ Ошибка: " + e.getMessage();
        } catch (Exception e) {
            return "❌ Произошла ошибка при добавлении игры";
        }
    }

    public void addNewGame(String name, String description, String category,
                           int minPlayers, int maxPlayers, int avgTime) {
        validateGameParameters(name, minPlayers, maxPlayers, avgTime);

        BoardGame newGame = new BoardGame(
                null,
                name,
                description,
                category,
                minPlayers,
                maxPlayers,
                avgTime
        );

        boardGameDao.addGame(newGame);
        refreshGames();
    }

    private void validateGameParameters(String name, int minPlayers, int maxPlayers, int avgTime) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Название игры обязательно");
        }
        if (minPlayers <= 0) {
            throw new IllegalArgumentException("Минимальное количество игроков должно быть больше 0");
        }
        if (minPlayers > maxPlayers) {
            throw new IllegalArgumentException("Минимум игроков не может быть больше максимума");
        }
        if (avgTime <= 0) {
            throw new IllegalArgumentException("Среднее время игры должно быть положительным");
        }
    }

    public String listAllGames() {
        if (allGames.isEmpty()) {
            return "🎲 В коллекции пока нет игр.";
        }

        StringBuilder sb = new StringBuilder("🎲 <b>Коллекция игр</b>:\n\n");
        for (BoardGame game : allGames) {
            sb.append(game.toFormattedString()).append("\n\n");
        }
        return sb.toString();
    }

    public String getGameInfo(String gameName) {
        if (gameName == null || gameName.isEmpty()) {
            return "ℹ️ Укажите название игры: /filtergame НазваниеИгры";
        }

        BoardGame game = findGameByName(gameName);
        if (game == null) {
            return "❌ Игра \"" + gameName + "\" не найдена в коллекции.";
        }

        return game.toDetailedString();
    }

    public BoardGame findGameByName(String gameName) {
        return allGames.stream()
                .filter(g -> g.getName().equalsIgnoreCase(gameName))
                .findFirst()
                .orElse(null);
    }

    public List<BoardGame> findGamesByCategory(String category) {
        return allGames.stream()
                .filter(g -> g.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public String listGamesByCategory(String category) {
        List<BoardGame> games = findGamesByCategory(category);
        if (games.isEmpty()) {
            return "❌ В категории \"" + category + "\" пока нет игр.";
        }

        StringBuilder sb = new StringBuilder("🎲 Игры в категории \"" + category + "\":\n\n");
        for (BoardGame game : games) {
            sb.append(game.toShortString()).append("\n");
        }
        return sb.toString();
    }

    public String removeGame(String gameName) {
        BoardGame game = findGameByName(gameName);
        if (game == null) {
            return "❌ Игра \"" + gameName + "\" не найдена в коллекции.";
        }

        refreshGames();
        return "✅ Игра \"" + gameName + "\" удалена из коллекции.";
    }

    public List<BoardGame> getGamesForPlayers(int playersCount) {
        return allGames.stream()
                .filter(g -> g.getMinPlayers() <= playersCount && g.getMaxPlayers() >= playersCount)
                .collect(Collectors.toList());
    }

    public String suggestGamesForPlayers(int playersCount) {
        List<BoardGame> suitableGames = getGamesForPlayers(playersCount);
        if (suitableGames.isEmpty()) {
            return "❌ Нет игр, подходящих для " + playersCount + " игроков.";
        }

        StringBuilder sb = new StringBuilder("🎲 Игры для " + playersCount + " игроков:\n\n");
        for (BoardGame game : suitableGames) {
            sb.append(game.toShortString()).append("\n");
        }
        return sb.toString();
    }


}