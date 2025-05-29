package org.example.game_controller;

import org.example.BoardGame;
import org.example.dao.BoardGameDao;

import java.util.List;

public class GameManager {
    private BoardGameDao boardGameDao;
    private List<BoardGame> allGames;

    public GameManager(BoardGameDao boardGameDao) {
        this.boardGameDao = boardGameDao;
        this.allGames = boardGameDao.getAllGames();
    }

    public void addNewGame(String name, String description, String category, int minPlayers, int maxPlayers, int avgTime) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Название игры обязательно");
        }

        if (minPlayers > maxPlayers) {
            throw new IllegalArgumentException("Минимум игроков не может быть больше максимума");
        }

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
        allGames = boardGameDao.getAllGames();
    }

    public List<BoardGame> getAllGames() {
        return allGames;
    }

    public BoardGame findGameByName(String gameName) {
        return allGames.stream()
                .filter(g -> g.getName().equalsIgnoreCase(gameName))
                .findFirst()
                .orElse(null);
    }

    public String listAllGames() {
        if (allGames.isEmpty()) {
            return "Список игр пуст.";
        }

        StringBuilder sb = new StringBuilder("Доступные игры:\n");
        for (BoardGame game : allGames) {
            sb.append("- ").append(game.toShortString()).append("\n");
        }
        return sb.toString();
    }
}