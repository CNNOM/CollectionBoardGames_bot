package org.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public class GameSession {
    public enum GameStatus {
        IN_PROGRESS, PLAYED
    }

    private String id;
    private String gameId;
    private String gameName;
    private LocalDateTime dateTime;
    private List<String> players;
    private String winner;
    private GameStatus status;

    public GameSession() {}

    @JsonCreator
    public GameSession(
            @JsonProperty("id") String id,
            @JsonProperty("gameId") String gameId,
            @JsonProperty("gameName") String gameName,
            @JsonProperty("dateTime") LocalDateTime dateTime,
            @JsonProperty("players") List<String> players,
            @JsonProperty("winner") String winner,
            @JsonProperty("status") GameStatus status) {
        this.id = id;
        this.gameId = gameId;
        this.gameName = gameName;
        this.dateTime = dateTime;
        this.players = players;
        this.winner = winner;
        this.status = status;
    }

    public String getId() { return id; }
    public String getGameId() { return gameId; }
    public String getGameName() { return gameName; }
    public LocalDateTime getDateTime() { return dateTime; }
    public List<String> getPlayers() { return players; }
    public String getWinner() { return winner; }
    public GameStatus getStatus() { return status; }

    public void setId(String id) { this.id = id; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public void setGameName(String gameName) { this.gameName = gameName; }
    public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
    public void setPlayers(List<String> players) { this.players = players; }
    public void setWinner(String winner) { this.winner = winner; }
    public void setStatus(GameStatus status) { this.status = status; }

    @JsonIgnore
    public String getPlayersAsString() {
        return players != null ? String.join(", ", players) : "";
    }

    @Override
    public String toString() {
        return String.format(
                "Игра: %s\nДата: %s\nИгроки: %s\nПобедитель: %s\nСтатус: %s",
                gameName,
                dateTime.toString(),
                getPlayersAsString(),
                winner,
                status == GameStatus.PLAYED ? "Завершена" : "В процессе"
        );
    }
}
