package org.example.dao;

import org.example.BoardGame;
import org.example.GameSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BoardGameDaoMemoryImpl implements BoardGameDao {
    private final List<BoardGame> games = new ArrayList<>();
    private final List<GameSession> sessions = new ArrayList<>();

    @Override
    public List<BoardGame> getAllGames() {
        return new ArrayList<>(games);
    }

    @Override
    public List<GameSession> getGameHistory() {
        return new ArrayList<>(sessions);
    }

    @Override
    public void addGame(BoardGame game) {
        if (game.getId() == null) {
            game.setId(UUID.randomUUID().toString());
        }
        games.add(game);
    }

    @Override
    public void addGameSession(GameSession session) {
        if (session.getId() == null) {
            session.setId(UUID.randomUUID().toString());
        }
        sessions.add(session);
    }


    @Override
    public void updateGameSessionStatus(GameSession session) {
        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i).getId().equals(session.getId())) {
                sessions.set(i, session);
                return;
            }
        }
    }

    @Override
    public void close() {
    }

}
