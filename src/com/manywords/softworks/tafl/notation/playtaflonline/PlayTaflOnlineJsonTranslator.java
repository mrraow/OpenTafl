package com.manywords.softworks.tafl.notation.playtaflonline;

import com.manywords.softworks.tafl.OpenTafl;
import com.manywords.softworks.tafl.engine.Game;
import com.manywords.softworks.tafl.engine.GameState;
import com.manywords.softworks.tafl.engine.MoveRecord;
import com.manywords.softworks.tafl.notation.RulesSerializer;
import com.manywords.softworks.tafl.rules.Coord;
import com.manywords.softworks.tafl.rules.Rules;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by jay on 9/15/16.
 */
public class PlayTaflOnlineJsonTranslator {

    public static Game readJsonFile(File f) {
        if(f == null) return null;
        if(!f.exists()) return null;

        BufferedInputStream bis = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(f));
            return readJsonInputStream(bis);
        }
        catch (FileNotFoundException e) {
            OpenTafl.logPrint(OpenTafl.LogLevel.NORMAL, "Failed to read file: " + f);
        }
        finally {
            if(bis != null) try {
                bis.close();
            }
            catch (IOException e) {
                // best effort
            }
        }

        return null;
    }

    public static Game readJsonInputStream(InputStream stream) {
        JsonReader reader = Json.createReader(stream);
        JsonObject gameObject = reader.readObject();

        JsonArray moveArray = gameObject.getJsonArray(PTOConstants.KEY_MOVES);
        List<MoveRecord> moves = parseMoveArray(moveArray);

        // Get starting layout
        String openTaflLayout = getLayoutForName(gameObject.getString(PTOConstants.KEY_LAYOUT));
        if(openTaflLayout == null) {
            OpenTafl.logPrintln(OpenTafl.LogLevel.NORMAL, "Unknown layout: " + gameObject.getString(PTOConstants.KEY_LAYOUT));
        }

        // Translate rules to OTNR
        String openTaflRules = "dim:" + getDimensionForName(gameObject.getString(PTOConstants.KEY_LAYOUT)) + " ";
        openTaflRules += "name:" + gameObject.getString(PTOConstants.KEY_LAYOUT) + " ";

        switch(gameObject.getInt(PTOConstants.KEY_OBJECTIVE)) {
            case PTOConstants.OBJECTIVE_CORNER: openTaflRules += "esc:c "; break;
            case PTOConstants.OBJECTIVE_EDGE: openTaflRules += "esc:e cor: "; break;
        }

        switch(gameObject.getInt((PTOConstants.KEY_KING_CAPTURE))) {
            case PTOConstants.KING_CUSTODIAN: openTaflRules += "ks:w "; break;
            case PTOConstants.KING_ENCLOSED: openTaflRules += "ks:s "; break;
            case PTOConstants.KING_CONFINED: openTaflRules += "ks:m "; break;
            case PTOConstants.KING_FLEXIBLE: openTaflRules += "ks:c "; break;
        }

        switch(gameObject.getInt((PTOConstants.KEY_KING_STRENGTH))) {
            case PTOConstants.KING_WEAPONLESS: openTaflRules += "ka:n "; break;
            case PTOConstants.KING_ARMED: openTaflRules += "ka:y "; break;
            case PTOConstants.KING_HAMMER: openTaflRules += "ka:h "; break;
            case PTOConstants.KING_ANVIL: openTaflRules += "ka:a "; break;
        }

        switch(gameObject.getInt((PTOConstants.KEY_THRONE))) {
            case PTOConstants.THRONE_EXCLUSIVE: openTaflRules += "cens:K cenp:tcnkTCNK "; break;
            case PTOConstants.THRONE_FORBIDDEN: openTaflRules += "cens: cenp:tcnkTCNK "; break;
            case PTOConstants.THRONE_BLOCK_PAWN: openTaflRules += "cens: cenp:K "; break;
            case PTOConstants.THRONE_BLOCK_ALL: openTaflRules += "cens: cenp: "; break;
            case PTOConstants.THRONE_NONE: openTaflRules += "cen: cens:tcnkTCNK cenp:tcnkTCNK "; break;
        }

        switch(gameObject.getInt((PTOConstants.KEY_HOSTILE))) {
            case PTOConstants.HOSTILE_NONE: openTaflRules += "cenh: cenhe: "; break;
            case PTOConstants.HOSTILE_THRONE: openTaflRules += "cenh:tcnk cenhe:tcnkTCNK "; break;
        }

        switch(gameObject.getInt((PTOConstants.KEY_SPEED))) {
            case PTOConstants.SPEED_KING: openTaflRules += "spd:-1,-1,-1,-1,-1,-1,-1,1 "; break;
            case PTOConstants.SPEED_PAWN: openTaflRules += "spd:1,1,1,1,1,1,1,-1 "; break;
            case PTOConstants.SPEED_ALL: openTaflRules += "spd:1 "; break;
        }

        switch(gameObject.getInt((PTOConstants.KEY_SURROUND), PTOConstants.SURROUND_ENABLED)) {
            case PTOConstants.SURROUND_ENABLED: openTaflRules += "surf:y "; break;
            case PTOConstants.SURROUND_DISABLED: openTaflRules += "surf:n "; break;
        }

        switch(gameObject.getInt((PTOConstants.KEY_EXIT_FORT))) {
            case PTOConstants.EXIT_FORT_ENABLED: openTaflRules += "efe:y "; break;
            case PTOConstants.EXIT_FORT_DISABLED: openTaflRules += "efen: n "; break;
        }

        switch(gameObject.getInt((PTOConstants.KEY_SHIELDWALL))) {
            case PTOConstants.SHIELDWALL_ENABLED: openTaflRules += "sw:s swf:y "; break;
            case PTOConstants.SHIELDWALL_DISABLED: openTaflRules += "sw:n "; break;
        }

        // Load rules, create game, set up some default tag values
        String otnrString = openTaflRules + openTaflLayout;
        OpenTafl.logPrintln(OpenTafl.LogLevel.NORMAL, "Generated rules from JSON: " + otnrString);
        Rules r = RulesSerializer.loadRulesRecord(otnrString);
        OpenTafl.logPrintln(OpenTafl.LogLevel.CHATTY, "Generated rules re-serialized: " + RulesSerializer.getRulesRecord(r));
        Game g = new Game(r, null);

        // Apply moves
        for(MoveRecord m : moves) {
            int moveResult = g.getCurrentState().makeMove(m);
            OpenTafl.logPrintln(OpenTafl.LogLevel.CHATTY, "Move: " + m + " Result: " + GameState.getStringForMoveResult(moveResult));
        }

        g.setDefaultTags();
        g.getTagMap().put(Game.Tag.ATTACKERS, gameObject.getString(PTOConstants.KEY_ATTACKER, ""));
        g.getTagMap().put(Game.Tag.DEFENDERS, gameObject.getString(PTOConstants.KEY_DEFENDER, ""));
        g.getTagMap().put(Game.Tag.SITE, "playtaflonline.com");

        if(gameObject.getInt(PTOConstants.KEY_START_DATE, -1) != -1) {
            long timestamp = gameObject.getJsonNumber(PTOConstants.KEY_START_DATE).longValue() * 1000;
            g.getTagMap().put(Game.Tag.DATE, new SimpleDateFormat("yyyy.MM.dd").format(new Date(timestamp)));
        }

        return g;
    }

    private static List<MoveRecord> parseMoveArray(JsonArray moveArray) {
        List<MoveRecord> moves = new ArrayList<>(moveArray.size());

        // Backwards in the game specification
        for(int i = moveArray.size() - 1; i >= 0; i--) {
            JsonObject moveObject = moveArray.getJsonObject(i);
            Coord startCoord = Coord.get(
                    moveObject.getInt(PTOConstants.KEY_MOVE_X_FROM),
                    moveObject.getInt(PTOConstants.KEY_MOVE_Y_FROM));
            Coord endCoord = Coord.get(
                    moveObject.getInt(PTOConstants.KEY_MOVE_X_TO),
                    moveObject.getInt(PTOConstants.KEY_MOVE_Y_TO));

            moves.add(new MoveRecord(startCoord, endCoord));
        }

        return moves;
    }

    private static int getDimensionForName(String name) {
        name = name.toLowerCase();
        if(name.equals("hhtablut") || name.equals("tablut")) return 9;
        else if(name.equals("hhgokstad") || name.equals("seabattlegokstad") || name.equals("gokstad2")) return 13;
        else if(name.equals("hhcoppergate") || name.equals("coppergate2")) return 15;
        else if(name.equals("hhbrandubh") || name.equals("brandubh")) return 7;
        else if(name.equals("hhtawlbwrdd") || name.equals("tawlbwrdd")) return 11;
        else if(name.equals("hhardri") || name.equals("ardri") || name.equals("magpie")) return 7;
        else if(name.equals("fetlar") || name.equals("copenhagen") || name.equals("hnefatafl")) return 11;
        else if(name.equals("seabattlecircle") || name.equals("jarlshofcircle")) return 9;
        else if(name.equals("seabattlecross") || name.equals("trondheimcross")) return 11;
        else if(name.equals("gokstad1")) return 13;
        else if(name.equals("coppergate1")) return 15;
        else if(name.equals("papillon")) return 9;
        else return -1;
    }

    private static String getLayoutForName(String name) {
        name = name.toLowerCase();
        if(name.equals("hhtablut") || name.equals("tablut")) return PTOConstants.TABLUT_LAYOUT;
        else if(name.equals("hhgokstad") || name.equals("seabattlegokstad") || name.equals("gokstad2")) return PTOConstants.PARLETT_LAYOUT;
        else if(name.equals("hhcoppergate") || name.equals("coppergate2")) return PTOConstants.COPPERGATE_II_LAYOUT;
        else if(name.equals("hhbrandubh") || name.equals("brandubh")) return PTOConstants.BRANDUB_LAYOUT;
        else if(name.equals("hhtawlbwrdd") || name.equals("tawlbwrdd")) return PTOConstants.TAWLBWRDD_LAYOUT;
        else if(name.equals("hhardri") || name.equals("ardri") || name.equals("magpie")) return PTOConstants.ARD_RI_LAYOUT;
        else if(name.equals("fetlar") || name.equals("copenhagen") || name.equals("hnefatafl")) return PTOConstants.COPENHAGEN_LAYOUT;
        else if(name.equals("seabattlecircle") || name.equals("jarlshofcircle")) return PTOConstants.JARLSHOF_LAYOUT;
        else if(name.equals("seabattlecross") || name.equals("trondheimcross")) return PTOConstants.SERIF_CROSS_11_LAYOUT;
        else if(name.equals("gokstad1")) return PTOConstants.SERIF_CROSS_13_LAYOUT;
        else if(name.equals("coppergate1")) return PTOConstants.SERIF_CROSS_15_LAYOUT;
        else if(name.equals("papillon")) return PTOConstants.PAPILLON_LAYOUT;
        else return null;
    }
}
