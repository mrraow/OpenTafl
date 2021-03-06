package com.manywords.softworks.tafl.rules.fetlar.eleven.test;

import com.manywords.softworks.tafl.rules.*;

import java.util.ArrayList;
import java.util.List;

public class FetlarTestAttackers extends Side {
    public FetlarTestAttackers(Board board) {
        super(board);
    }

    public FetlarTestAttackers(Board board, List<TaflmanHolder> taflmen) {
        super(board, taflmen);
    }

    @Override
    public boolean isAttackingSide() {
        return true;
    }

    @Override
    public boolean hasKnights() {
        return false;
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
        return new FetlarTestAttackers(board, getStartingTaflmen());
    }

    public List<TaflmanHolder> generateTaflmen() {
        List<TaflmanImpl> taflmen = new ArrayList<TaflmanImpl>(24);

        // Right side
        taflmen.add(new TaflmanImpl((byte) 0, Taflman.TYPE_TAFLMAN, Coord.get(0, 3), this, getBoard(), getBoard().getRules()));
        taflmen.add(new TaflmanImpl((byte) 1, Taflman.TYPE_TAFLMAN, Coord.get(0, 5), this, getBoard(), getBoard().getRules()));
        taflmen.add(new TaflmanImpl((byte) 2, Taflman.TYPE_TAFLMAN, Coord.get(0, 6), this, getBoard(), getBoard().getRules()));
        taflmen.add(new TaflmanImpl((byte) 3, Taflman.TYPE_TAFLMAN, Coord.get(0, 7), this, getBoard(), getBoard().getRules()));


        // Top
        taflmen.add(new TaflmanImpl((byte) 4, Taflman.TYPE_TAFLMAN, Coord.get(6, 0), this, getBoard(), getBoard().getRules()));
        taflmen.add(new TaflmanImpl((byte) 5, Taflman.TYPE_TAFLMAN, Coord.get(7, 0), this, getBoard(), getBoard().getRules()));

        // Left side
        taflmen.add(new TaflmanImpl((byte) 6, Taflman.TYPE_TAFLMAN, Coord.get(10, 3), this, getBoard(), getBoard().getRules()));
        taflmen.add(new TaflmanImpl((byte) 7, Taflman.TYPE_TAFLMAN, Coord.get(10, 4), this, getBoard(), getBoard().getRules()));
        taflmen.add(new TaflmanImpl((byte) 8, Taflman.TYPE_TAFLMAN, Coord.get(10, 5), this, getBoard(), getBoard().getRules()));
        taflmen.add(new TaflmanImpl((byte) 9, Taflman.TYPE_TAFLMAN, Coord.get(10, 6), this, getBoard(), getBoard().getRules()));
        taflmen.add(new TaflmanImpl((byte) 10, Taflman.TYPE_TAFLMAN, Coord.get(10, 7), this, getBoard(), getBoard().getRules()));
        taflmen.add(new TaflmanImpl((byte) 11, Taflman.TYPE_TAFLMAN, Coord.get(9, 5), this, getBoard(), getBoard().getRules()));

        // Bottom
        taflmen.add(new TaflmanImpl((byte) 12, Taflman.TYPE_TAFLMAN, Coord.get(3, 10), this, getBoard(), getBoard().getRules()));
        taflmen.add(new TaflmanImpl((byte) 13, Taflman.TYPE_TAFLMAN, Coord.get(4, 10), this, getBoard(), getBoard().getRules()));
        taflmen.add(new TaflmanImpl((byte) 14, Taflman.TYPE_TAFLMAN, Coord.get(5, 10), this, getBoard(), getBoard().getRules()));
        taflmen.add(new TaflmanImpl((byte) 15, Taflman.TYPE_TAFLMAN, Coord.get(6, 10), this, getBoard(), getBoard().getRules()));
        taflmen.add(new TaflmanImpl((byte) 16, Taflman.TYPE_TAFLMAN, Coord.get(7, 10), this, getBoard(), getBoard().getRules()));
        taflmen.add(new TaflmanImpl((byte) 17, Taflman.TYPE_TAFLMAN, Coord.get(5, 9), this, getBoard(), getBoard().getRules()));

        return createHolderListFromTaflmanList(taflmen);
    }
}
