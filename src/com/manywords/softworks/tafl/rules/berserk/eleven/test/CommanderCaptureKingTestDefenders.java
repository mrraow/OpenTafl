package com.manywords.softworks.tafl.rules.berserk.eleven.test;

import com.manywords.softworks.tafl.rules.Board;
import com.manywords.softworks.tafl.rules.Coord;
import com.manywords.softworks.tafl.rules.Side;
import com.manywords.softworks.tafl.rules.TaflmanImpl;
import com.manywords.softworks.tafl.rules.taflmen.King;
import com.manywords.softworks.tafl.rules.taflmen.Knight;

import java.util.ArrayList;
import java.util.List;

public class CommanderCaptureKingTestDefenders extends Side {
    public CommanderCaptureKingTestDefenders(Board board) {
        super(board);
    }

    public CommanderCaptureKingTestDefenders(Board board, List<TaflmanHolder> taflmen) {
        super(board, taflmen);
    }

    @Override
    public boolean isAttackingSide() {
        return false;
    }

    @Override
    public boolean hasKnights() {
        return true;
    }

    @Override
    public boolean hasMercenaries() {
        return false;
    }

    @Override
    public boolean hasCommanders() {
        return false;
    }

    @Override
    public boolean hasGuards() {
        return false;
    }

    @Override
    public Side deepCopy(Board board) {
        return new CommanderCaptureKingTestDefenders(board, getStartingTaflmen());
    }

    public List<TaflmanHolder> generateTaflmen() {
        List<TaflmanImpl> taflmen = new ArrayList<TaflmanImpl>(13);

        // 5,5 is center
        taflmen.add(new King((byte) 4, Coord.get(5, 5), this, getBoard(), getBoard().getRules()));

        // Adjacent spaces to 5,5
        taflmen.add(new Knight((byte) 5, Coord.get(4, 4), this, getBoard(), getBoard().getRules()));

        // The 'point spaces'

        return createHolderListFromTaflmanList(taflmen);
    }
}
