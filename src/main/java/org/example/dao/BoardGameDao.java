package org.example.dao;

import org.example.BoardGame;
import org.example.GameSession;

import java.util.List;

public interface BoardGameDao extends AutoCloseable {
    List<BoardGame> getAllGames();
    List<GameSession> getGameHistory();

    void addGame(BoardGame game);
    void addGameSession(GameSession session);
    void updateGameSessionStatus(GameSession session);
    void close();
}