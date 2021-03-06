package com.manywords.softworks.tafl.rules;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jay on 2/6/16.
 */
public class GenericBoard extends BoardImpl {
    private int mBoardDimension = 11;

    public GenericBoard(int boardSize) {
        super(boardSize);
        mBoardDimension = boardSize;
    }

    public GenericBoard(Rules rules) {
        super(rules.boardSize);
        mBoardDimension = rules.boardSize;
        setRules(rules);
    }

    public GenericBoard(Board b) {
        super(b);
        mBoardDimension = b.getBoardDimension();
        setRules(b.getRules());
    }

    @Override
    public int getBoardDimension() {
        return mBoardDimension;
    }

    @Override
    public Board deepCopy() {
        return new GenericBoard(this);
    }
}
