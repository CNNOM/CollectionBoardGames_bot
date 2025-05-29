package org.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BoardGame {
    private String id;
    private String name;
    private String description;
    private String category;
    private int minPlayers;
    private int maxPlayers;
    private int averageTime;

    @JsonCreator
    public BoardGame(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("category") String category,
            @JsonProperty("minPlayers") int minPlayers,
            @JsonProperty("maxPlayers") int maxPlayers,
            @JsonProperty("averageTime") int averageTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.averageTime = averageTime;
    }

    // Геттеры
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getAverageTime() { return averageTime; }

    // Сеттеры
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setMinPlayers(int minPlayers) { this.minPlayers = minPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public void setAverageTime(int averageTime) { this.averageTime = averageTime; }

    @Override
    public String toString() {
        return String.format(
                "%s (Категория: %s, Игроки: %d-%d, Время: %d мин)\nОписание: %s",
                name, category, minPlayers, maxPlayers, averageTime, description
        );
    }

    public String toShortString() {
        return String.format(
                "%s (%d-%d игроков, %d мин)",
                name, minPlayers, maxPlayers, averageTime
        );
    }
}