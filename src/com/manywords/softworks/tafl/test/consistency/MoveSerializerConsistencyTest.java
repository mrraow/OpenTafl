package com.manywords.softworks.tafl.test.consistency;

import com.manywords.softworks.tafl.engine.DetailedMoveRecord;
import com.manywords.softworks.tafl.notation.MoveSerializer;
import com.manywords.softworks.tafl.notation.RulesSerializer;
import com.manywords.softworks.tafl.rules.Coord;
import com.manywords.softworks.tafl.rules.Rules;
import com.manywords.softworks.tafl.rules.copenhagen.Copenhagen;
import com.manywords.softworks.tafl.test.TaflTest;

public class MoveSerializerConsistencyTest extends TaflTest {

    @Override
    public void run() {
        String move = "Ne6^=e8xce7/ne9/Kf8/d8";
        DetailedMoveRecord moveRecord = MoveSerializer.loadMoveRecord(9, move);
        String serializedMove = MoveSerializer.getMoveRecord(moveRecord);

        //System.out.println(move);
        //System.out.println(serializedMove);

        assert move.equals(serializedMove);
    }

}