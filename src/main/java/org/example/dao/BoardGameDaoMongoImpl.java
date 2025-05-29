package org.example.dao;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.BoardGame;
import org.example.GameSession;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class BoardGameDaoMongoImpl implements BoardGameDao {
    private final MongoCollection<Document> gamesCollection;
    private final MongoCollection<Document> sessionsCollection;
    private final MongoClient mongoClient;

    public BoardGameDaoMongoImpl(String connectionString, String databaseName, String collectionPrefix) {
        this.mongoClient = MongoClients.create(connectionString);
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        this.gamesCollection = database.getCollection(collectionPrefix + "_games");
        this.sessionsCollection = database.getCollection(collectionPrefix + "_sessions");
    }

    @Override
    public List<BoardGame> getAllGames() {
        return gamesCollection.find()
                .into(new ArrayList<>())
                .stream()
                .map(this::documentToBoardGame)
                .collect(Collectors.toList());
    }

    @Override
    public List<GameSession> getGameHistory() {
        return sessionsCollection.find()
                .sort(Sorts.descending("date"))
                .into(new ArrayList<>())
                .stream()
                .map(this::documentToGameSession)
                .collect(Collectors.toList());
    }


    @Override
    public void addGame(BoardGame game) {
        Document doc = new Document()
                .append("name", game.getName())
                .append("description", game.getDescription())
                .append("category", game.getCategory())
                .append("minPlayers", game.getMinPlayers())
                .append("maxPlayers", game.getMaxPlayers())
                .append("averageTime", game.getAverageTime());
        gamesCollection.insertOne(doc);
    }

    @Override
    public void addGameSession(GameSession session) {
        String playersString = String.join(", ", session.getPlayers());

        Date date = Date.from(session.getDateTime().atZone(ZoneId.systemDefault()).toInstant());

        Document doc = new Document()
                .append("gameId", session.getGameId())
                .append("gameName", session.getGameName())
                .append("date", date)
                .append("players", playersString)
                .append("winner", session.getWinner())
                .append("status", session.getStatus().toString());

        sessionsCollection.insertOne(doc);
    }

    @Override
    public void close() {
        mongoClient.close();
    }

    private BoardGame documentToBoardGame(Document doc) {
        return new BoardGame(
                doc.getObjectId("_id").toString(),
                doc.getString("name"),
                doc.getString("description"),
                doc.getString("category"),
                doc.getInteger("minPlayers", 2),
                doc.getInteger("maxPlayers", 4),
                doc.getInteger("averageTime", 30)
        );
    }

    @Override
    public void updateGameSessionStatus(GameSession session) {
        Document update = new Document("$set", new Document()
                .append("status", session.getStatus().toString()));

        sessionsCollection.updateOne(Filters.eq("_id", new ObjectId(session.getId())), update);
    }
    private GameSession documentToGameSession(Document doc) {
        LocalDateTime dateTime = doc.getDate("date").toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        String playersString = doc.getString("players");
        List<String> players = Arrays.stream(playersString.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        String statusString = doc.getString("status");
        GameSession.GameStatus status = (statusString != null) ? GameSession.GameStatus.valueOf(statusString) : GameSession.GameStatus.PLAYED;

        return new GameSession(
                doc.getObjectId("_id").toString(),
                doc.getString("gameId"),
                doc.getString("gameName"),
                dateTime,
                players,
                doc.getString("winner"),
                status
        );
    }


    @Override
    public Map<String, Map<String, Object>> getWinStatisticsByGame(String gameName) {
        // 1. Получаем всех уникальных игроков, которые когда-либо играли в эту игру
        List<Document> allPlayersPipeline = Arrays.asList(
                new Document("$match",
                        new Document("gameName", gameName)
                                .append("status", "PLAYED")),
                new Document("$project",
                        new Document("players",
                                new Document("$split", Arrays.asList("$players", ",")))),
                new Document("$unwind", "$players"),
                new Document("$group",
                        new Document("_id",
                                new Document("$trim", new Document("input", "$players"))))
        );

        Set<String> allPlayers = new HashSet<>();
        sessionsCollection.aggregate(allPlayersPipeline)
                .forEach(doc -> allPlayers.add(doc.getString("_id")));

        // 2. Получаем общее количество игр
        long totalGames = sessionsCollection.countDocuments(
                new Document("gameName", gameName)
                        .append("status", "PLAYED"));

        // 3. Получаем статистику побед
        List<Document> winsPipeline = Arrays.asList(
                new Document("$match",
                        new Document("gameName", gameName)
                                .append("status", "PLAYED")),
                new Document("$group",
                        new Document("_id", "$winner")
                                .append("wins", new Document("$sum", 1))),
                new Document("$sort", new Document("wins", -1))
        );

        Map<String, Integer> winsStats = new HashMap<>();
        sessionsCollection.aggregate(winsPipeline)
                .forEach(doc -> winsStats.put(doc.getString("_id"), doc.getInteger("wins")));

        // 4. Собираем полную статистику
        Map<String, Map<String, Object>> fullStats = new LinkedHashMap<>();
        for (String player : allPlayers) {
            int wins = winsStats.getOrDefault(player, 0);
            double winPercentage = totalGames > 0 ? (wins * 100.0 / totalGames) : 0;

            Map<String, Object> playerStats = new HashMap<>();
            playerStats.put("wins", wins);
            playerStats.put("totalGames", totalGames);
            playerStats.put("percentage", winPercentage);

            fullStats.put(player, playerStats);
        }

        return fullStats;
    }

}