package org.example.dao;

public class DaoFactory {
    public static BoardGameDao createTaskDao(String type) {
        switch (type.toLowerCase()) {
            case "memory":
                return new BoardGameDaoMemoryImpl();
            case "mongodb":
                return new BoardGameDaoMongoImpl(
                        "mongodb://localhost:27017/board_games",
                        "boardgames",
                        "tasks");
            case "json":
                return new BoardGameDaoJsonImpl("data");
            default:
                throw new IllegalArgumentException("Unknown DAO type: " + type);
        }
    }
}