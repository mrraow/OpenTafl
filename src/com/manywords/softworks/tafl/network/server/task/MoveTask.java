package com.manywords.softworks.tafl.network.server.task;

import com.manywords.softworks.tafl.network.packet.ingame.AwaitMovePacket;
import com.manywords.softworks.tafl.network.packet.ingame.MovePacket;
import com.manywords.softworks.tafl.network.packet.ingame.MoveResultPacket;
import com.manywords.softworks.tafl.network.server.NetworkServer;
import com.manywords.softworks.tafl.network.server.ServerClient;
import com.manywords.softworks.tafl.network.server.ServerGame;
import com.manywords.softworks.tafl.network.server.thread.PriorityTaskQueue;

import java.util.List;

/**
 * Created by jay on 5/27/16.
 */
public class MoveTask implements Runnable {
    /*
     * See NetworkServerPlayer for the path of a move through the server.
     *
     */
    private final NetworkServer server;
    private final ServerClient movingClient;
    private final MovePacket movePacket;

    public MoveTask(NetworkServer server, ServerClient movingClient, MovePacket movePacket) {
        this.server = server;
        this.movingClient = movingClient;
        this.movePacket = movePacket;
    }

    @Override
    public void run() {
        ServerGame game = movingClient.getGame();

        game.getPlayerForClient(movingClient).onMoveDecided(movePacket.move);
        
        server.sendPacketToClients(game.getSpectators(), movePacket, PriorityTaskQueue.Priority.LOW);
    }
}
