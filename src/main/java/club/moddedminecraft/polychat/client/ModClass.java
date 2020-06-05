/*
 *  This file is part of PolyChat Client.
 *  *
 *  * Copyright © 2018 DemonScythe45
 *  *
 *  * PolyChat Client is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Lesser General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * PolyChat Client is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public License
 *  * along with PolyChat Client. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package club.moddedminecraft.polychat.client;

import club.moddedminecraft.polychat.client.threads.ActivePlayerThread;
import club.moddedminecraft.polychat.client.threads.ReattachThread;
import club.moddedminecraft.polychat.networking.io.AbstractMessage;
import club.moddedminecraft.polychat.networking.io.MessageBus;
import club.moddedminecraft.polychat.networking.io.ServerStatusMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Mod("polychat-client")
public class ModClass {
    public static final String MODID = "polychat-client";
    public static final String NAME = "Poly Chat Client";
    public static final String VERSION = "1.2.3";
    //Used to determine whether the server cleanly shutdown or crashed
    public static boolean shutdownClean = false;
    //Used to determine whether to send a connection lost warning in game
    public static MinecraftServer server;
    public static Properties properties;
    public static MessageBus messageBus = null;
    public static ReattachThread reattachThread;
    public static ActivePlayerThread playerThread;
    public static String id = null;
    public static String idFormatted = null;

    public ModClass() {
        // Register setup methods
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
        MinecraftForge.EVENT_BUS.register(this);
    }

    //Contains null pointer exceptions from a failed connection to the main server
    public static void sendMessage(AbstractMessage message) {
        try {
            messageBus.sendMessage(message);
        } catch (NullPointerException ignored) {
        }
    }

    //Initiates the connection to the main polychat server and sets up the message callback
    public static void handleClientConnection() {
        try {
            messageBus = new MessageBus(new Socket(properties.getProperty("address"), Integer.parseInt(properties.getProperty("port"))), EventListener::handleMessage);
            messageBus.start();
        } catch (IOException e) {
            System.err.println("Failed to establish polychat connection!");
            e.printStackTrace();
        }
    }

    //Forces the server to allow clients to join without the mod installed on their client
//    public boolean checkClient(Map<String, String> map, Side side) {
//        return true;
//    }

    private void preInit(final FMLCommonSetupEvent event) {
        //Registers game event listener class
        MinecraftForge.EVENT_BUS.register(new EventListener());

        // setup config
        handleConfiguration(FMLPaths.CONFIGDIR.get().toFile());
        handlePrefix();

        reattachThread = new ReattachThread(5000);
        playerThread = new ActivePlayerThread(30000, id);

        //Registers the shutdown hook
        // TODO: doesn't work for some reason?
//        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        server = event.getServer();
    }

    @SubscribeEvent
    public void onStarted(FMLServerStartedEvent event) {
        //Connects to the main polychat server
        handleClientConnection();
        reattachThread.start();
        reattachThread.sendServerOnline();
        playerThread.start();
    }

    @SubscribeEvent
    public void onStopped(FMLServerStoppingEvent event) {
        shutdownClean = true;
        shutdownHook();
    }

    //Makes sure that the server offline message gets sent
    public void shutdownHook() {
        reattachThread.interrupt();
        playerThread.interrupt();
        short exitVal;
        //Sends either crashed or offline depending on if shutdown happened cleanly
        if (shutdownClean) {
            exitVal = 2;
        } else {
            exitVal = 3;
        }
        ServerStatusMessage statusMessage = new ServerStatusMessage(id, idFormatted, exitVal);
        ModClass.sendMessage(statusMessage);
        try {
            //Makes sure message has time to send
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
        messageBus.stop();
    }

    //Sets the color to use for the server prefix in chat
    public void handlePrefix() {
        String serverId = properties.getProperty("server_id");
        if (!(serverId.equals("empty"))) {
            int code = Integer.parseInt(properties.getProperty("id_color"));
            if ((code < 0) || (code > 15)) {
                code = 15;
            }
            id = serverId;
            idFormatted = String.format("§%01x%s", code, serverId);
        }
    }

    public void handleConfiguration(File modConfigDir) {
        ModClass.properties = new Properties();
        File config = new File(modConfigDir, "polychat.properties");

        //Loads config if it exists or creates a default one if not
        if (config.exists() && config.isFile()) {
            try (FileInputStream istream = new FileInputStream(config)) {
                ModClass.properties.load(istream);
            } catch (IOException e) {
                System.err.println("Error loading configuration file!");
                e.printStackTrace();
            }
        } else {
            ModClass.properties.setProperty("address", "127.0.0.1");
            ModClass.properties.setProperty("port", "25566");
            ModClass.properties.setProperty("server_id", "empty");
            ModClass.properties.setProperty("server_name", "empty");
            ModClass.properties.setProperty("server_address", "empty");
            ModClass.properties.setProperty("id_color", "15"); //Default to white color
            try (FileOutputStream ostream = new FileOutputStream(config)) {
                ModClass.properties.store(ostream, null);
            } catch (IOException e) {
                System.err.println("Error saving new configuration file!");
                e.printStackTrace();
            }
        }
    }

}
