package com.manywords.softworks.tafl.network.client;

import com.manywords.softworks.tafl.Log;
import com.manywords.softworks.tafl.OpenTafl;
import com.manywords.softworks.tafl.command.player.NetworkClientPlayer;
import com.manywords.softworks.tafl.engine.DetailedMoveRecord;
import com.manywords.softworks.tafl.engine.Game;
import com.manywords.softworks.tafl.engine.GameState;
import com.manywords.softworks.tafl.engine.MoveRecord;
import com.manywords.softworks.tafl.engine.clock.TimeSpec;
import com.manywords.softworks.tafl.network.packet.ClientInformation;
import com.manywords.softworks.tafl.network.packet.GameInformation;
import com.manywords.softworks.tafl.network.packet.ingame.*;
import com.manywords.softworks.tafl.network.packet.pregame.*;
import com.manywords.softworks.tafl.network.packet.utility.ErrorPacket;
import com.manywords.softworks.tafl.network.packet.utility.SuccessPacket;
import com.manywords.softworks.tafl.network.server.GameRole;
import com.manywords.softworks.tafl.rules.Rules;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.UUID;

/**
 * Created by jay on 5/23/16.
 */
public class ClientServerConnection {
    public interface ClientServerCallback {
        public void onStateChanged(State newState);
        public void onChatMessageReceived(ChatType type, String sender, String message);
        public void onSuccessReceived(String message);
        public void onErrorReceived(String message);
        public void onGameListReceived(List<GameInformation> games);
        public void onClientListReceived(List<ClientInformation> clients);
        public void onDisconnect(boolean planned);
        public Game getGame();
        public void onStartGame(Rules r, List<DetailedMoveRecord> history);
        public void onHistoryReceived(List<DetailedMoveRecord> moves);
        public void onServerMoveReceived(MoveRecord move);
        public void onClockUpdateReceived(TimeSpec attackerClock, TimeSpec defenderClock);
        public void onVictory(VictoryPacket.Victory victory);
    }

    public enum ChatType {
        LOBBY,
        GAME,
        SPECTATOR
    }

    public enum State {
        DISCONNECTED,
        LOGGED_IN,
        CREATING_GAME,
        JOINING_GAME,
        IN_PREGAME,
        IN_GAME,
        IN_POSTGAME,
    }

    private State mCurrentState = State.DISCONNECTED;

    private final String hostname;
    private final int port;

    private Socket mServer;
    private PrintWriter mServerWriter;
    private Thread mReadThread;

    private String mUsername;
    private UUID mServerGameUUID;
    private GameRole mGameRole = GameRole.OUT_OF_GAME;
    private NetworkClientPlayer mNetworkPlayer;
    private GameInformation mLastJoinedGame = null;
    private List<DetailedMoveRecord> mLastHistory = null;

    private boolean mPlannedDisconnect = false;

    private ClientServerCallback mExternalCallback;
    private ClientServerCallback mInternalCallback = new InternalCallback();

    public ClientServerConnection(String hostname, int port, ClientServerCallback callback) {
        this.hostname = hostname;
        this.port = port;
        mExternalCallback = callback;
    }

    /* Test code */

    ClientServerConnection(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    void setTestCallback(TestClientServerConnection.TestClientServerCallback callback) {
        mExternalCallback = callback;
    }

    void chattyPrint(String message) {
        Log.println(Log.Level.VERBOSE, message);
    }

    void standardPrint(String message) {
        Log.println(Log.Level.NORMAL, message);
    }

    /**/


    public void setCallback(ClientServerCallback callback) {
        mExternalCallback = callback;
    }

    public boolean connect(String username, String hashedPassword) {
        try {
            mPlannedDisconnect = false;
            mServer = new Socket(hostname, port);
            mServerWriter = new PrintWriter(new OutputStreamWriter(mServer.getOutputStream()), true);

            mReadThread = new ReadThread();
            mReadThread.start();

            mUsername = username;
            sendRegistrationMessage(username, hashedPassword);
            return true;
        } catch (IOException e) {
            standardPrint("Failed to connect: " + e);
            return false;
        }
    }

    public void disconnect() {
        try {
            mPlannedDisconnect = true;
            if(mServer != null) mServer.close();

            mServer = null;
        } catch (IOException e) {
            // Best effort
        }
    }

    public NetworkClientPlayer getNetworkPlayer() {
        mNetworkPlayer = new NetworkClientPlayer(this);
        return mNetworkPlayer;
    }

    public void setNetworkPlayer(NetworkClientPlayer player) {
        mNetworkPlayer = player;
    }

    public State getCurrentState() {
        return mCurrentState;
    }

    public GameRole getGameRole() {
        return mGameRole;
    }

    public boolean hasHistory() { return mLastHistory != null; }

    public List<DetailedMoveRecord> consumeHistory() {
        List<DetailedMoveRecord> history = mLastHistory;
        mLastHistory = null;
        return history;
    }

    public void sendCreateGameMessage(CreateGamePacket packet) {
        setState(State.CREATING_GAME);
        mLastJoinedGame = packet.toGameInformation();
        mServerGameUUID = packet.uuid;
        mServerWriter.println(packet);
    }

    public void sendHistoryRequest() {
        mServerWriter.println(HistoryPacket.PREFIX);
    }

    public void sendHistory(List<GameState> history, int dimension) {
        HistoryPacket packet = HistoryPacket.parseHistory(history, dimension);
        mServerWriter.println(packet);
    }

    public void sendHistoryByMoveRecords(List<DetailedMoveRecord> history, int dimension) {
        HistoryPacket packet = new HistoryPacket(history, dimension);
        mServerWriter.println(packet);
    }

    public void sendClockUpdateRequest() {
        mServerWriter.println(ClockUpdatePacket.PREFIX);
    }

    public void sendClockUpdate(TimeSpec attackerClock, TimeSpec defenderClock) {
        ClockUpdatePacket packet = new ClockUpdatePacket(attackerClock, defenderClock);
        mServerWriter.println(packet);
    }

    public void sendLeaveGameMessage() {
        if(mServerGameUUID != null) {
            mServerWriter.println(new LeaveGamePacket(mServerGameUUID));
        }
        if(mCurrentState != State.DISCONNECTED && mCurrentState != State.LOGGED_IN) {
            setState(State.LOGGED_IN);
        }
    }

    public String getUsername() {
        return mUsername;
    }

    public GameInformation getLastJoinedGameInfo() {
        return mLastJoinedGame;
    }

    public TimeSpec getLastClockSetting() {
        if(mLastJoinedGame != null) {
            return mLastJoinedGame.clockSetting;
        }
        else return null;
    }

    public void sendRegistrationMessage(String username, String hashedPassword) {
        mServerWriter.println(new LoginPacket(username, hashedPassword, OpenTafl.NETWORK_PROTOCOL_VERSION));
    }

    public void sendChatMessage(ChatType type, String sender, String message) {
        if(type == ChatType.LOBBY) mServerWriter.println(new LobbyChatPacket(sender, message));
        else if(type == ChatType.GAME) mServerWriter.println(new GameChatPacket(sender, message));
        else if(type == ChatType.SPECTATOR) mServerWriter.println(new GameChatPacket(sender, message));
    }

    public void sendJoinGameMessage(GameInformation gameInfo, JoinGamePacket packet) {
        if(mCurrentState != State.LOGGED_IN) {
            mInternalCallback.onErrorReceived(ErrorPacket.ALREADY_HOSTING);
        }
        else if(gameInfo.started && gameInfo.hasFreeSide()) {
            mInternalCallback.onErrorReceived(ErrorPacket.GAME_ENDED);
        }
        else {
            mLastJoinedGame = gameInfo;
            setState(State.JOINING_GAME);
            mServerGameUUID = packet.uuid;
            mServerWriter.println(packet);
        }
    }

    public void sendMoveDecidedMessage(MoveRecord move) {
        mServerWriter.println(new MovePacket(move));
    }

    public void sendGameEndedMessage() {
        mServerWriter.println(new GameEndedPacket());
    }

    public void requestGameUpdate() {
        mServerWriter.println("game-list");
        mServerWriter.println("client-list");
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            standardPrint("Connecting to server: " + mServer.getInetAddress());
            String inputData = "";
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(mServer.getInputStream()));
            ) {
                while((inputData = in.readLine()) != null) {
                    try {
                        chattyPrint("Client received: " + inputData);
                        ClientCommandParser.handlePacket(mInternalCallback, inputData);
                    }
                    catch(Exception e) {
                        chattyPrint("Encountered exception reading from server: ");
                        Log.stackTrace(Log.Level.VERBOSE, e);
                    }
                }
            }
            catch(IOException e) {
                chattyPrint("Server connection error: " + e);
            }

            standardPrint("Disconnected from server");
            mInternalCallback.onDisconnect(mPlannedDisconnect);
        }
    }

    void setState(State newState) {
        if(mCurrentState != newState) {
            chattyPrint("State change: " + mCurrentState + " to " + newState);
            mCurrentState = newState;
            mInternalCallback.onStateChanged(newState);
        }
    }

    private class InternalCallback implements ClientServerCallback {

        @Override
        public void onStateChanged(State newState) {
            switch(newState) {
                case LOGGED_IN:
                    mGameRole = GameRole.OUT_OF_GAME;
                case IN_PREGAME:
                    requestGameUpdate();
                    break;
            }
            mExternalCallback.onStateChanged(newState);
        }

        @Override
        public void onChatMessageReceived(ChatType type, String sender, String message) {
            mExternalCallback.onChatMessageReceived(type, sender, message);
        }

        @Override
        public void onSuccessReceived(String message) {
            switch(mCurrentState) {

                case DISCONNECTED:
                    setState(State.LOGGED_IN);
                    break;
                case LOGGED_IN:
                    break;
                case JOINING_GAME:
                case CREATING_GAME:
                case IN_PREGAME:
                case IN_GAME:
                    // If we joined as the attackers, the other player is the defender.
                    if(message.equals(SuccessPacket.JOINED_ATTACKERS)) {
                        mGameRole = GameRole.ATTACKER;
                    }
                    else if(message.equals(SuccessPacket.JOINED_DEFENDERS)) {
                        mGameRole = GameRole.DEFENDER;
                    }
                    else if(message.equals(SuccessPacket.JOINED_SPECTATOR)) {
                        mGameRole = GameRole.KIBBITZER;
                    }
                    chattyPrint("Joined game as " + mGameRole);
                    setState(State.IN_PREGAME);
                    mExternalCallback.onSuccessReceived(message);
                    break;
            }
        }

        @Override
        public void onErrorReceived(String message) {
            if(message.equals(ErrorPacket.GAME_CANCELED)) {
                requestGameUpdate();
            }
            else if(message.equals(ErrorPacket.VERSION_MISMATCH) || message.equals(ErrorPacket.LOGIN_FAILED)) {
                mExternalCallback.onErrorReceived(message);
                disconnect();

                return;
            }
            // These errors don't break anything, they just mean we can't do stuff.
            else if(message.equals(ErrorPacket.ALREADY_HOSTING)
                    || message.equals(ErrorPacket.GAME_ENDED)
                    || message.equals(ErrorPacket.OPPONENT_LEFT)) {
                // A joining player goes back to loggged in, a hosting player stays as host
                if(mCurrentState == State.JOINING_GAME) {
                    setState(State.LOGGED_IN);
                }
                mExternalCallback.onErrorReceived(message);
                return;
            }
            else if(message.equals(ErrorPacket.BAD_SAVE)) {
                // A player joining, hosting, or in pregame should go back to logged in.
                if(mCurrentState == State.JOINING_GAME
                        || mCurrentState == State.CREATING_GAME
                        || mCurrentState == State.IN_PREGAME) {
                    setState(State.LOGGED_IN);
                }
                mExternalCallback.onErrorReceived(message);
                return;
            }

            switch(mCurrentState) {

                case DISCONNECTED:
                    mExternalCallback.onErrorReceived(message);
                    break;
                case LOGGED_IN:
                    if(message.equals(ErrorPacket.VERSION_MISMATCH)) {
                        setState(State.DISCONNECTED);
                    }
                    break;
                case JOINING_GAME:
                case CREATING_GAME:
                    setState(State.LOGGED_IN);
                    mExternalCallback.onErrorReceived(message);
                    break;
                case IN_PREGAME:
                    break;
            }
        }

        @Override
        public void onGameListReceived(List<GameInformation> games) {
            mExternalCallback.onGameListReceived(games);
        }

        @Override
        public void onClientListReceived(List<ClientInformation> clients) {
            mExternalCallback.onClientListReceived(clients);
        }

        @Override
        public void onDisconnect(boolean planned) {
            setState(State.DISCONNECTED);
            mExternalCallback.onDisconnect(planned);
        }

        @Override
        public Game getGame() {
            return mExternalCallback.getGame();
        }

        @Override
        public void onStartGame(Rules r, List<DetailedMoveRecord> history) {
            setState(State.IN_GAME);
            mLastHistory = history;
            mExternalCallback.onStartGame(r, history);
        }

        @Override
        public void onHistoryReceived(List<DetailedMoveRecord> moves) {
            mLastHistory = moves;
            mExternalCallback.onHistoryReceived(moves);
        }

        @Override
        public void onServerMoveReceived(MoveRecord move) {
            if(mGameRole != GameRole.KIBBITZER) {
                mNetworkPlayer.onMoveDecided(move);
            }
            else {
                mExternalCallback.onServerMoveReceived(move);
            }
        }

        @Override
        public void onClockUpdateReceived(TimeSpec attackerClock, TimeSpec defenderClock) {
            mExternalCallback.onClockUpdateReceived(attackerClock, defenderClock);
        }

        @Override
        public void onVictory(VictoryPacket.Victory victory) {
            mExternalCallback.onVictory(victory);
        }
    }
}
