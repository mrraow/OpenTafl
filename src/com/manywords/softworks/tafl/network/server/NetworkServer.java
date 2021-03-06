package com.manywords.softworks.tafl.network.server;

import com.manywords.softworks.tafl.Log;
import com.manywords.softworks.tafl.OpenTafl;
import com.manywords.softworks.tafl.engine.clock.TimeSpec;
import com.manywords.softworks.tafl.network.PasswordHasher;
import com.manywords.softworks.tafl.network.packet.NetworkPacket;
import com.manywords.softworks.tafl.network.packet.pregame.LobbyChatPacket;
import com.manywords.softworks.tafl.network.server.database.PlayerDatabase;
import com.manywords.softworks.tafl.network.server.database.file.FileBackedPlayerDatabase;
import com.manywords.softworks.tafl.network.server.task.SendPacketTask;
import com.manywords.softworks.tafl.network.server.task.interval.BucketedIntervalTaskHolder;
import com.manywords.softworks.tafl.network.server.task.interval.IntervalTask;
import com.manywords.softworks.tafl.network.server.thread.PriorityTaskQueue;
import com.manywords.softworks.tafl.network.server.thread.ServerTickThread;
import com.manywords.softworks.tafl.rules.Rules;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The main class for the OpenTafl server. Handles starting up things and initial reception of network packets.
 *
 * The TCP listeners here instantiate a ServerClient object,
 *
 * The path of a network packet:
 *
 * 1. Received here, task created.
 * 2. Task entered into appropriate queue.
 * 3. A network thread is notified, if necessary.
 * 4. The thread handles the request, including any required database/state updates &c.
 * 5. The thread responds to the client, if necessary.
 *
 * n.b. everything must be thread-safe.
 */
public class NetworkServer {
    private PriorityTaskQueue mTaskQueue;
    private ServerTickThread mTickThread;

    private BucketedIntervalTaskHolder mGameClockTasks;
    private BucketedIntervalTaskHolder mGameListUpdateTasks;

    private ServerSocket mServerSocket;
    private final List<ServerClient> mClients;
    private final List<ServerClient> mLobbyClients;
    private final List<ServerGame> mGames;

    private PlayerDatabase mPlayerDatabase;

    private boolean mRunning = true;

    public NetworkServer(int threadCount) {
        this(threadCount, true);
    }

    public NetworkServer(int threadCount, boolean chatty) {
        mTaskQueue = new PriorityTaskQueue(threadCount);
        mClients = new ArrayList<>(64);
        mLobbyClients = new ArrayList<>(64);
        mGames = new ArrayList<>(32);

        mPlayerDatabase = new FileBackedPlayerDatabase(this, new File("server", "player.db"));
        mPlayerDatabase.updateDatabase();

        mTickThread = new ServerTickThread();

        mGameListUpdateTasks = new BucketedIntervalTaskHolder(mTaskQueue, 1000, 5, PriorityTaskQueue.Priority.LOW);
        mGameClockTasks = new BucketedIntervalTaskHolder(mTaskQueue, 1000, 5, PriorityTaskQueue.Priority.HIGH);

        mTickThread.addTaskHolder(mGameListUpdateTasks);
        mTickThread.addTaskHolder(mGameClockTasks);

        mPlayerDatabase.addUpdateTasks(mTickThread, mTaskQueue);
    }

    public void chattyPrint(String message) { Log.println(Log.Level.VERBOSE, message); }

    public void standardPrint(String message) { Log.println(Log.Level.NORMAL, message); }

    public void start() {
        startServer();
    }

    public void stop() {
        mRunning = false;
        try {
            mServerSocket.close();
        } catch (IOException e) {
            // best effort
        }
    }

    public PriorityTaskQueue getTaskQueue() {
        return mTaskQueue;
    }

    public boolean hasClientNamed(String username) {
        synchronized (mClients) {
            for (ServerClient c : mClients) {
                if (username.equals(c.getUsername())) return true;
            }
        }

        return false;
    }

    public PlayerDatabase getPlayerDatabase() {
        return mPlayerDatabase;
    }

    public void sendPacketToAllClients(NetworkPacket packet, PriorityTaskQueue.Priority priority) {
        for(ServerClient client : mClients) {
            sendPacketToClient(client, packet, priority);
        }
    }

    public void sendPacketToClients(List<ServerClient> clients, NetworkPacket packet, PriorityTaskQueue.Priority priority) {
        for(ServerClient client : clients) {
            sendPacketToClient(client, packet, priority);
        }
    }

    public void sendPacketToClient(ServerClient client, NetworkPacket packet, PriorityTaskQueue.Priority priority) {
        mTaskQueue.pushTask(new SendPacketTask(packet, client), priority);
    }

    private void startServer() {
        standardPrint("Starting server with network protocol version " + OpenTafl.NETWORK_PROTOCOL_VERSION);
        mTaskQueue.start();
        mTickThread.start();

        try {
            mServerSocket = new ServerSocket(11541);

            while(mRunning) {
                Socket clientSocket = mServerSocket.accept();
                new ServerClient(this, clientSocket);
            }
        } catch (IOException e) {
            chattyPrint("Server socket exception");
            //System.exit(-1);
        } finally {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                // best effort
            }
        }

        standardPrint("Server stopping.");
    }

    public List<ServerClient> getClients() {
        synchronized (mClients) {
            return new ArrayList<>(mClients);
        }
    }

    public List<ServerClient> getLobbyClients() {
        synchronized (mLobbyClients) {
            return new ArrayList<>(mLobbyClients);
        }
    }

    /**
     * Returns a copy of the games list.
     * @return
     */
    public List<ServerGame> getGames() {
        synchronized (mGames) {
            return new ArrayList<>(mGames);
        }
    }

    public ServerGame getGame(UUID gameUUID) {
        ServerGame g = null;

        synchronized (mGames) {
            for(ServerGame game : mGames) {
                if(game.uuid.equals(gameUUID)) {
                    g = game;
                    break;
                }
            }
        }

        return g;
    }

    public boolean createGame(ServerClient client, UUID gameUUID, String password, Rules rules, boolean attackingSide, boolean combineChat, boolean allowReplay) {
        return createGame(client, gameUUID, password, rules, attackingSide, combineChat, allowReplay, null);
    }

    public boolean createGame(ServerClient client, UUID gameUUID, String password, Rules rules, boolean attackingSide, boolean combineChat, boolean allowReplay, TimeSpec clockSetting) {
        if(client.getGame() != null) {
            return false;
        }

        ServerGame g = new ServerGame(this, gameUUID);
        g.setRules(rules);
        g.setChatCombined(combineChat);
        g.setReplayAllowed(allowReplay);
        if(!password.equals(PasswordHasher.NO_PASSWORD)) {
            g.setPassword(password);
        }

        if(clockSetting != null) {
            g.setClock(clockSetting);
        }

        if(attackingSide) {
            g.tryJoinGame(client, password, true, false);
        }
        else {
            g.tryJoinGame(client, password, false, true);
        }

        synchronized (mGames) {
            mGames.add(g);
        }

        return true;
    }

    public void startGame(ServerGame game) {
        IntervalTask clockUpdateTask = game.getClockUpdateTask();
        if(clockUpdateTask != null) {
            mGameClockTasks.addBucketTask(clockUpdateTask);
        }

        game.startGame();
    }

    /**
     * Called not when the actual game is done, but when both clients have exited the game.
     * @return
     */
    public void removeGame(ServerGame g) {
        synchronized (mGames) {
            mGames.remove(g);
        }

        IntervalTask clockUpdateTask = g.getClockUpdateTask();
        if(clockUpdateTask != null) {
            mGameClockTasks.removeBucketTask(clockUpdateTask);
        }

        g.shutdown();
    }

    public void onConnect(ServerClient c) {
        synchronized (mClients) {
            mClients.add(c);
        }
    }

    public void onDisconnect(ServerClient c) {
        synchronized (mClients) {
            mClients.remove(c);
        }

        // If a party to the game leaves the server, stop the game.
        if(c.getGame() != null) {
            c.getGame().removeClient(c);
        }
        clientExitingLobby(c);

        if(c.getUsername() != null) {
            sendPacketToClients(getLobbyClients(), new LobbyChatPacket("Server", c.getUsername() + " has disconnected."), PriorityTaskQueue.Priority.LOW);
        }
    }

    public void clientEnteringLobby(ServerClient c) {

        sendPacketToClients(getLobbyClients(), new LobbyChatPacket("Server", c.getUsername() + " has connected."), PriorityTaskQueue.Priority.LOW);

        synchronized (mLobbyClients) {
            mLobbyClients.add(c);
        }

        for(IntervalTask t : c.getLobbyTasks()) {
            mGameListUpdateTasks.addBucketTask(t);
        }
    }

    private void clientExitingLobby(ServerClient c) {
        synchronized (mLobbyClients) {
            mLobbyClients.remove(c);
        }

        for(IntervalTask t : c.getLobbyTasks()) {
            mGameListUpdateTasks.removeBucketTask(t);
        }
    }
}
