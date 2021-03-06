package com.manywords.softworks.tafl.network.server.task;

import com.manywords.softworks.tafl.network.packet.ingame.GameChatPacket;
import com.manywords.softworks.tafl.network.packet.pregame.CreateGamePacket;
import com.manywords.softworks.tafl.network.server.GameRole;
import com.manywords.softworks.tafl.network.server.NetworkServer;
import com.manywords.softworks.tafl.network.server.ServerClient;
import com.manywords.softworks.tafl.network.server.thread.PriorityTaskQueue;

/**
 * Created by jay on 5/28/16.
 */
public class GameChatTask implements Runnable {
    private NetworkServer mServer;
    private ServerClient mClient;
    private GameChatPacket mPacket;

    public GameChatTask(NetworkServer server, ServerClient client, GameChatPacket packet) {
        mServer = server;
        mClient = client;
        mPacket = packet;
    }

    @Override
    public void run() {
        if(mClient.getGame() != null) {
            if(mClient.getGame().isChatCombined()) {
                mServer.sendPacketToClients(mClient.getGame().getAllClients(), mPacket, PriorityTaskQueue.Priority.LOW);
            }
            else {
                if(mClient.getGameRole() == GameRole.KIBBITZER) {
                    mServer.sendPacketToClients(mClient.getGame().getSpectators(), mPacket, PriorityTaskQueue.Priority.LOW);
                }
                else {
                    mServer.sendPacketToClients(mClient.getGame().getAllClients(), mPacket, PriorityTaskQueue.Priority.LOW);
                }
            }
        }
    }
}
