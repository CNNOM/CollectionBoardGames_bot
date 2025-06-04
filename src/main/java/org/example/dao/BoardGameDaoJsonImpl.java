package org.example.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.BoardGame;
import org.example.GameSession;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class BoardGameDaoJsonImpl implements BoardGameDao {
    private final ObjectMapper objectMapper;
    private final File gamesFile;
    private final File sessionsFile;

    public BoardGameDaoJsonImpl(String basePath) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        File dataDir = new File(basePath);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        this.gamesFile = new File(basePath + "/games.json");
        this.sessionsFile = new File(basePath + "/sessions.json");
    }

    @Override
    public List<BoardGame> getAllGames() {
        if (!gamesFile.exists()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(gamesFile, new TypeReference<List<BoardGame>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<GameSession> getGameHistory() {
        if (!sessionsFile.exists()) {
            return new ArrayList<>();
        }

        try {
            List<GameSession> sessions = objectMapper.readValue(sessionsFile, new TypeReference<List<GameSession>>() {});
            sessions.sort((s1, s2) -> s2.getDateTime().compareTo(s1.getDateTime()));
            return sessions;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public void addGame(BoardGame game) {
        List<BoardGame> games = getAllGames();
        if (game.getId() == null) {
            game.setId(UUID.randomUUID().toString());
        }
        games.add(game);
        saveGames(games);
    }

    @Override
    public void addGameSession(GameSession session) {
        List<GameSession> sessions = getGameHistory();
        if (session.getId() == null) {
            session.setId(UUID.randomUUID().toString());
        }
        sessions.add(session);
        saveSessions(sessions);
    }

    @Override
    public void updateGameSessionStatus(GameSession session) {
        List<GameSession> sessions = getGameHistory();
        sessions = sessions.stream()
                .map(s -> s.getId().equals(session.getId()) ? session : s)
                .collect(Collectors.toList());
        saveSessions(sessions);
    }

    @Override
    public void close() {
    }

    private void saveGames(List<BoardGame> games) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(gamesFile, games);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveSessions(List<GameSession> sessions) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(sessionsFile, sessions);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}