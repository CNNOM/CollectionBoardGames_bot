package org.example.dao;

import org.example.BoardGame;
import org.example.GameSession;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BoardGameDaoMemoryImpl implements BoardGameDao {
    private final Map<String, BoardGame> games = new ConcurrentHashMap<>();
    private final List<GameSession> sessions = Collections.synchronizedList(new ArrayList<>());

    @Override
    public List<BoardGame> getAllGames() {
        return new ArrayList<>(games.values());
    }

    @Override
    public List<GameSession> getGameHistory() {
        // Возвращаем копию, отсортированную по дате (новые сначала)
        return sessions.stream()
                .sorted((s1, s2) -> s2.getDateTime().compareTo(s1.getDateTime()))
                .collect(Collectors.toList());
    }

    @Override
    public void addGame(BoardGame game) {
        if (game.getId() == null) {
            game.setId(UUID.randomUUID().toString());
        }
        games.put(game.getId(), game);
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
        synchronized (sessions) {
            for (int i = 0; i < sessions.size(); i++) {
                if (sessions.get(i).getId().equals(session.getId())) {
                    sessions.set(i, session);
                    break;
                }
            }
        }
    }

    @Override
    public void close() {
        games.clear();
        sessions.clear();
    }
}