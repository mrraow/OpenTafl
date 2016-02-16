package com.manywords.softworks.tafl.test;

import com.manywords.softworks.tafl.engine.Game;
import com.manywords.softworks.tafl.engine.GameState;
import com.manywords.softworks.tafl.engine.MoveRecord;
import com.manywords.softworks.tafl.ui.UiCallback;
import com.manywords.softworks.tafl.engine.ai.AiWorkspace;
import com.manywords.softworks.tafl.rules.Rules;
import com.manywords.softworks.tafl.rules.Side;
import com.manywords.softworks.tafl.rules.brandub.Brandub;
import com.manywords.softworks.tafl.ui.command.CommandResult;
import com.manywords.softworks.tafl.ui.player.Player;

class AIMatchingZobristTest extends TaflTest implements UiCallback {

    @Override
    public void gameStarting() {

    }

    @Override
    public void awaitingMove(Player currentPlayer, boolean isAttackingSide) {

    }

    @Override
    public void moveResult(CommandResult result, MoveRecord move) {

    }

    @Override
    public void statusText(String text) {

    }

    @Override
    public void gameStateAdvanced() {
        // TODO Auto-generated method stub

    }

    @Override
    public void victoryForSide(Side side) {
        // TODO Auto-generated method stub

    }

    @Override
    public void gameFinished() {

    }

    @Override
    public MoveRecord waitForHumanMoveInput() {
        return null;
    }

    @Override
    public boolean inGame() {
        return false;
    }

    @Override
    public void run() {
        Rules rules = Brandub.newAiMoveRepetitionTest();
        Game game = new Game(rules, null);
        GameState state = game.getCurrentState();


        state = game.getCurrentState();
        AiWorkspace workspace = new AiWorkspace(this, game, state, 5);
        workspace.explore(3);
        MoveRecord nextMove = workspace.getTreeRoot().getBestChild().getEnteringMove();
        long zobrist = workspace.getTreeRoot().getBestChild().getZobrist();
        state.makeMove(nextMove);

        state = game.getCurrentState();
        assert zobrist == state.mZobristHash;
    }

}
