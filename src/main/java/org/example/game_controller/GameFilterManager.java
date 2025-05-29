package org.example.game_controller;

import org.example.GameSession;
import org.example.dao.BoardGameDao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GameFilterManager {
    private BoardGameDao boardGameDao;
    private List<GameSession> gameHistory;

    public GameFilterManager(BoardGameDao boardGameDao) {
        this.boardGameDao = boardGameDao;
        this.gameHistory = boardGameDao.getGameHistory();
    }

    public List<GameSession> applyFilter(LocalDate fromDate, LocalDate toDate, String gameName, GameSession.GameStatus status) {
        List<GameSession> filteredSessions = new ArrayList<>(gameHistory);

        if (fromDate != null) {
            filteredSessions = filteredSessions.stream()
                    .filter(s -> !s.getDateTime().toLocalDate().isBefore(fromDate))
                    .collect(Collectors.toList());
        }

        if (toDate != null) {
            filteredSessions = filteredSessions.stream()
                    .filter(s -> !s.getDateTime().toLocalDate().isAfter(toDate))
                    .collect(Collectors.toList());
        }

        if (gameName != null && !gameName.equals("Все игры")) {
            filteredSessions = filteredSessions.stream()
                    .filter(s -> s.getGameName().equals(gameName))
                    .collect(Collectors.toList());
        }

        if (status != null) {
            filteredSessions = filteredSessions.stream()
                    .filter(s -> s.getStatus() == status)
                    .collect(Collectors.toList());
        }

        filteredSessions.sort((s1, s2) -> s2.getDateTime().compareTo(s1.getDateTime()));

        return filteredSessions;
    }
}

