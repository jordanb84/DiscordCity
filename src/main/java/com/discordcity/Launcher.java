package com.discordcity;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;

public class Launcher {

    public static void main(String[] args) {
        try {
            File configFile = new File("./config.json");
            JSONObject config = new JSONObject(FileUtils.readFileToString(configFile, Charset.defaultCharset()));

            String helpStatusInformation = (config.getString("BOT_PREFIX") + "help");

            Bot discordCityBot = new Bot(config, "Build your city! " + helpStatusInformation);
        } catch(IOException | LoginException | SQLException | ClassNotFoundException initializationException) {
            initializationException.printStackTrace();
        }
    }

}
