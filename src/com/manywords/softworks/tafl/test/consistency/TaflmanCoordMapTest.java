package com.manywords.softworks.tafl.test.consistency;

import com.manywords.softworks.tafl.engine.collections.TaflmanCoordListMap;
import com.manywords.softworks.tafl.engine.collections.TaflmanCoordMap;
import com.manywords.softworks.tafl.rules.Coord;
import com.manywords.softworks.tafl.rules.Taflman;
import com.manywords.softworks.tafl.test.TaflTest;

import java.util.ArrayList;
import java.util.List;

public class TaflmanCoordMapTest extends TaflTest {
    @Override
    public void run() {
        TaflmanCoordMap map = new TaflmanCoordMap(19, 127, 127);

        for(int i = 0; i < 127; i++) {
            char packed = Taflman.encode((char) i, Taflman.SIDE_ATTACKERS, Taflman.TYPE_TAFLMAN);
            map.put(packed, Coord.getCoordForIndex(19, i));
        }

        for(int i = 127; i < 254; i++) {
            char packed = Taflman.encode((char) i, Taflman.SIDE_DEFENDERS, Taflman.TYPE_TAFLMAN);
            map.put(packed, Coord.getCoordForIndex(19, i));
        }

        char packed = Taflman.encode((char) 129, Taflman.SIDE_DEFENDERS, Taflman.TYPE_TAFLMAN);


        //println(map);
        assert map.getCoord(packed) != null;

        for(int i = 0; i < 127; i++) {
            packed = Taflman.encode((char) i, Taflman.SIDE_ATTACKERS, Taflman.TYPE_TAFLMAN);
            assert Coord.getCoordForIndex(19, i).equals(map.getCoord(packed));
        }

        for(int i = 127; i < 254; i++) {
            packed = Taflman.encode((char) i, Taflman.SIDE_DEFENDERS, Taflman.TYPE_TAFLMAN);
            assert Coord.getCoordForIndex(19, i).equals(map.getCoord(packed));
        }

        TaflmanCoordListMap listMap = new TaflmanCoordListMap(19, 127, 127);

        for(int i = 0; i < 127; i++) {
            packed = Taflman.encode((char) i, Taflman.SIDE_ATTACKERS, Taflman.TYPE_TAFLMAN);
            List<Coord> list = new ArrayList<>();
            list.add(Coord.getCoordForIndex(19, i));
            listMap.put(packed, list);
        }

        for(int i = 127; i < 254; i++) {
            packed = Taflman.encode((char) i, Taflman.SIDE_DEFENDERS, Taflman.TYPE_TAFLMAN);
            List<Coord> list = new ArrayList<>();
            list.add(Coord.getCoordForIndex(19, i));
            listMap.put(packed, list);
        }

        for(int i = 0; i < 127; i++) {
            packed = Taflman.encode((char) i, Taflman.SIDE_ATTACKERS, Taflman.TYPE_TAFLMAN);
            List<Coord> list = new ArrayList<>();
            list.add(Coord.getCoordForIndex(19, i));
            assert list.equals(listMap.get(packed));
        }

        for(int i = 127; i < 254; i++) {
            packed = Taflman.encode((char) i, Taflman.SIDE_DEFENDERS, Taflman.TYPE_TAFLMAN);
            List<Coord> list = new ArrayList<>();
            list.add(Coord.getCoordForIndex(19, i));
            assert list.equals(listMap.get(packed));
        }

    }
}
