package com.discordcity.city;

import com.discordcity.city.tile.CityTileType;
import com.discordcity.database.Sqlite;
import com.discordcity.util.TimeUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class City {

    private int population;

    private int funds;

    private int maxDensity;

    private int unemployment;

    private CityTileType[] tiles;
    private int tileGridWidth;
    private int tileGridHeight;

    private String ownerUserId;

    public City(int population, int funds, int maxDensity, CityTileType[] tiles, int tileGridWidth, int tileGridHeight, String ownerUserId) {
        this.population = population;
        this.funds = funds;
        this.maxDensity = maxDensity;
        this.tiles = tiles;
        this.tileGridWidth = tileGridWidth;
        this.tileGridHeight = tileGridHeight;
        this.ownerUserId = ownerUserId;
    }

    public void updateUnemployment() {
        int unemploymentPoints = 0;

        int industryRequiredPerHouse = 3;

        int houses = this.tileCountForType(CityTileType.House);
        int industry = this.tileCountForType(CityTileType.Industry);

        unemploymentPoints += houses;
        unemploymentPoints -= industry * industryRequiredPerHouse;

        if(unemploymentPoints <= 0) {
            this.unemployment = 0;
        } else {
            this.unemployment = unemploymentPoints * 100 / houses;
        }

        if(this.unemployment > 100) {
            this.unemployment = 100;
        }
    }

    public void updateCityForTime(int secondsSinceLastUpdate, Sqlite database) throws SQLException {
        for(CityTileType cityTileType : this.tiles) {

            boolean validTileUpdate = cityTileType.CITY_TILE.updateForTime(secondsSinceLastUpdate, this);

            if(validTileUpdate) {
                cityTileType.CITY_TILE.resetTileUpdateTime();
            }
        }

        this.updateUnemployment();
        this.updateDatabaseProperties(database);
    }

    public void modifyPopulation(int modification) {
        this.population += modification;

        if(this.population > this.getMaxPopulation()) {
            this.population = this.getMaxPopulation();
        }

        if(this.population < 0) {
            this.population = 0;
        }
    }

    public void setTile(int column, int row, CityTileType cityTileType, Sqlite database) throws SQLException {
        int tileIndex = (row * this.tileGridWidth + column);

        this.tiles[tileIndex] = cityTileType;

        this.writeTilemapToDatabase(database);
    }

    public void modifyFunds(int modification) {
        this.funds += modification;
    }

    private void writeTilemapToDatabase(Sqlite database) throws SQLException {
        String tilemapData = new CityBuilder().tilesToString(this.tiles);

        PreparedStatement updateTilemap = database.getStatement("UPDATE CityTiles SET tiles = ? WHERE ownerUserId = ?");
        updateTilemap.setString(1, tilemapData);
        updateTilemap.setString(2, this.ownerUserId);

        updateTilemap.execute();
    }

    public CityTileType getTileForIndex(int index) {
        return this.tiles[index];
    }

    public int getTileGridWidth() {
        return this.tileGridWidth;
    }

    public int getTileGridHeight() {
        return this.tileGridHeight;
    }

    public int getPopulation() {
        return this.population;
    }

    public int getFunds() {
        return this.funds;
    }

    private int tileCountForType(CityTileType cityTileType) {
        int tileCount = 0;

        for(CityTileType possibleMatch : this.tiles) {
            if(possibleMatch == cityTileType) {
                tileCount++;
            }
        }

        return tileCount;
    }

    public int getDensity() {
        try {
            int houses = this.tileCountForType(CityTileType.House);

            int density = this.population / houses;

            return density;
        } catch(ArithmeticException zeroPopulation) {
            return 0;
        }
    }

    public Timestamp getLastUpdated(Sqlite database) throws SQLException {
        PreparedStatement lastUpdatedQuery = database.getStatement("SELECT lastUpdated FROM CityProperties WHERE ownerUserId = ?");
        lastUpdatedQuery.setString(1, this.ownerUserId);

        ResultSet lastUpdatedResult = lastUpdatedQuery.executeQuery();
        lastUpdatedResult.next();

        return lastUpdatedResult.getTimestamp("lastUpdated");
    }

    public void updateDatabaseProperties(Sqlite database) throws SQLException {
        this.updateDatabaseTime(database);
        this.updateDatabasePopulation(database);
        this.updateDatabaseFunds(database);
        this.updateDatabaseMaxDensity(database);
    }

    public void updateDatabaseTime(Sqlite database) throws SQLException {
        Timestamp currentTime = TimeUtil.getInstance().getCurrentTime();

        PreparedStatement updateTime = database.getStatement("UPDATE CityProperties SET lastUpdated = ? WHERE ownerUserId = ?");
        updateTime.setTimestamp(1, currentTime);
        updateTime.setString(2, this.ownerUserId);

        updateTime.execute();
    }

    public void updateDatabasePopulation(Sqlite database) throws SQLException {
        PreparedStatement updatePopulation = database.getStatement("UPDATE CityProperties SET population = ? WHERE ownerUserId = ?");
        updatePopulation.setInt(1, this.population);
        updatePopulation.setString(2, this.ownerUserId);

        updatePopulation.execute();
    }

    public void updateDatabaseFunds(Sqlite database) throws SQLException {
        PreparedStatement updateFunds = database.getStatement("UPDATE CityProperties SET funds = ? WHERE ownerUserId = ?");
        updateFunds.setString(1, "" + this.funds);
        updateFunds.setString(2, this.ownerUserId);

        updateFunds.execute();
    }

    public void updateDatabaseMaxDensity(Sqlite database) throws SQLException {
        PreparedStatement updateMaxDensity = database.getStatement("UPDATE CityProperties SET maxDensity = ? WHERE ownerUserId = ?");
        updateMaxDensity.setInt(1, this.maxDensity);
        updateMaxDensity.setString(2, this.ownerUserId);

        updateMaxDensity.execute();
    }

    public int getSecondsSinceLastUpdate(Sqlite database) throws SQLException {
        Timestamp lastUpdatedTime = this.getLastUpdated(database);
        Timestamp currentTime = TimeUtil.getInstance().getCurrentTime();

        long differenceMillis = currentTime.getTime() - lastUpdatedTime.getTime();

        return (int) differenceMillis / 1000;
    }

    public int getMaxDensity() {
        return this.maxDensity;
    }

    public int getMaxPopulation() {
        return this.maxDensity * this.tileCountForType(CityTileType.House);
    }

    public String getOwnerUserId() {
        return this.ownerUserId;
    }

    public int getUnemployment() {
        return this.unemployment;
    }

}