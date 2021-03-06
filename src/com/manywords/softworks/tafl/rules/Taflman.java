package com.manywords.softworks.tafl.rules;

import com.manywords.softworks.tafl.engine.DetailedMoveRecord;
import com.manywords.softworks.tafl.engine.GameState;
import com.manywords.softworks.tafl.engine.MoveRecord;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Taflman {
    public static final char EMPTY = 0;

    public static final char ID_MASK = 255; // Eight low bits
    public static final char TYPE_MASK = 256 + 512 + 1024; // Three bits
    public static final char SIDE_MASK = 2048; // One bit
    public static final char DEVELOPED_MASK = 4096; // One bit

    public static final char TYPE_TAFLMAN = 256;
    public static final char TYPE_COMMANDER = 512;
    public static final char TYPE_KNIGHT = 256 + 512;
    public static final char TYPE_KING = 1024;
    public static final char TYPE_MERCENARY = 1024 + 256;
    public static final char TYPE_GUARD = 1024 + 512;

    public static final char SIDE_ATTACKERS = 2048;
    public static final char SIDE_DEFENDERS = 0;

    public static final char DEVELOPED = 4096;
    public static final char UNDEVELOPED = 0;

    public static final char[] ALL_TAFLMAN_TYPES = new char[] {
            TYPE_TAFLMAN | SIDE_ATTACKERS,
            TYPE_COMMANDER | SIDE_ATTACKERS,
            TYPE_KNIGHT | SIDE_ATTACKERS,
            TYPE_KING | SIDE_ATTACKERS,
            TYPE_MERCENARY | SIDE_ATTACKERS,
            TYPE_GUARD | SIDE_ATTACKERS,
            TYPE_TAFLMAN | SIDE_DEFENDERS,
            TYPE_COMMANDER | SIDE_DEFENDERS,
            TYPE_KNIGHT | SIDE_DEFENDERS,
            TYPE_KING | SIDE_DEFENDERS,
            TYPE_MERCENARY | SIDE_DEFENDERS,
            TYPE_GUARD | SIDE_DEFENDERS,
    };
    public static final int TYPES_BY_PIECE_AND_SIDE = ALL_TAFLMAN_TYPES.length;
    public static final int TYPES_BY_PIECE = ALL_TAFLMAN_TYPES.length / 2;


    public static char encode(TaflmanImpl taflman) {
        char packedTaflman = 0;
        packedTaflman = (char) (packedTaflman | (char) taflman.getImplId());

        if (taflman.isKing()) {
            packedTaflman = (char) (packedTaflman | TYPE_KING);

        } else if (taflman.isKnight()) packedTaflman = (char) (packedTaflman | TYPE_KNIGHT);
        else if (taflman.isCommander()) packedTaflman = (char) (packedTaflman | TYPE_COMMANDER);
        else if (taflman.isMercenary()) packedTaflman = (char) (packedTaflman | TYPE_MERCENARY);
        else if (taflman.isGuard()) packedTaflman = (char) (packedTaflman | TYPE_GUARD);
        else packedTaflman = (char) (packedTaflman | TYPE_TAFLMAN);

        if (taflman.getSide().isAttackingSide()) {
            packedTaflman = (char) (packedTaflman | SIDE_ATTACKERS);
        }

        return packedTaflman;
    }

    public static char encode(char id, char type, char side) {
        char packedTaflman = 0;
        packedTaflman = (char) (packedTaflman | id);
        packedTaflman = (char) (packedTaflman | type);
        packedTaflman = (char) (packedTaflman | side);

        return packedTaflman;
    }

    public static byte getPackedId(char packed) {
        return (byte) (packed & ID_MASK);
    }

    public static char getPackedType(char packed) {
        return (char) (packed & TYPE_MASK);
    }

    public static char getPackedSide(char packed) {
        return (char) (packed & SIDE_MASK);
    }

    public static boolean getDeveloped(char taflman) { return (taflman & DEVELOPED_MASK) == DEVELOPED; }

    public static Board getBoard(GameState state) {
        return state.getBoard();
    }


    /**
     * Return an 8-bit identifier for this piece, unique on its side.
     *
     * @return
     */
    public static byte getId(char taflman) {
        return getPackedId(taflman);
    }

    /**
     * Is this piece a king?
     *
     * @return
     */
    public static boolean isKing(char taflman) {
        return (char) (getPackedType(taflman) & TYPE_MASK) == TYPE_KING;
    }

    public static boolean isKnight(char taflman) {
        return (char) (getPackedType(taflman) & TYPE_MASK) == TYPE_KNIGHT;
    }

    public static boolean isCommander(char taflman) {
        return (char) (getPackedType(taflman) & TYPE_MASK) == TYPE_COMMANDER;
    }

    public static boolean isMercenary(char taflman) {
        return (char) (getPackedType(taflman) & TYPE_MERCENARY) == TYPE_MERCENARY;
    }

    public static boolean isGuard(char taflman) {
        return (char) (getPackedType(taflman) & TYPE_GUARD) == TYPE_GUARD;
    }

    /**
     * To which side does this piece belong?
     *
     * @return
     */
    public static Side getSide(GameState state, char taflman) {
        if (getPackedSide(taflman) == SIDE_ATTACKERS) {
            return state.getAttackers();
        } else {
            return state.getDefenders();
        }
    }

    public static Coord getCurrentSpace(GameState state, char taflman) {
        return getBoard(state).findTaflmanSpace(taflman);
    }

    public static List<Coord> getAllowableMoves(GameState state, char taflman) {
        Rules rules = getBoard(state).getRules();

        List<Coord> cachedMoves = state.getCachedAllowableMovesForTaflman(taflman);
        if (cachedMoves != null) return cachedMoves;

        List<Coord> moves = getAllowableMovesFrom(state, taflman, getCurrentSpace(state, taflman));
        state.setCachedAllowableMovesForTaflman(taflman, moves);
        return moves;
    }

    public static List<Coord> getAllowableMovesFrom(GameState state, char taflman, Coord space) {
        return getAllowableMovesFrom(state, taflman, space, true);
    }

    public static List<Coord> getAllowableMovesFrom(GameState state, char taflman, Coord space, boolean withJump) {
        Board b = getBoard(state);
        Rules rules = b.getRules();
        List<Coord> allowableMoves = new ArrayList<Coord>(b.getBoardDimension() * 2);

        // Moves on the same rank (row)
//        int x = space.x;
//        int y = space.y;
//        int boardSize = getBoard(state).getBoardDimension();

        int speedLimit = -1;
        if(rules.getSpeedLimitMode() != Rules.SPEED_LIMITS_NONE) {
            speedLimit = getBoard(state).getRules().getTaflmanSpeedLimit(taflman);
        }

        for(List<Coord> direction : Coord.getRankAndFileCoords(b.getBoardDimension(), space)) {
            int distance = 1;
            for(Coord potentialMove : direction) {
                boolean canPass = rules.canTaflmanMoveThrough(getBoard(state), taflman, potentialMove);
                if (getBoard(state).getOccupier(potentialMove) != EMPTY || !canPass) {
                    break;
                }
                else if (speedLimit > 0 && distance > speedLimit) {
                    break;
                }
                else {
                    allowableMoves.add(potentialMove);
                    distance++;
                }
            }
        }

        // Defer caring about jumps to allowableDestinations: a piece can only jump
        // to a space on which it is allowed to stop, so we don't need to worry about
        // it here.

        return allowableMoves;
    }

    public static List<Coord> getCapturingMoves(GameState state, char taflman) {
        List<Coord> cachedMoves = state.getCachedCapturingMovesForTaflman(taflman);
        if (cachedMoves != null) return cachedMoves;

        List<Coord> moves = getAllowableDestinations(state, taflman);
        List<Coord> capturingMoves = getCapturingMovesFromDestinations(state, taflman, getCurrentSpace(state, taflman), moves);

        state.setCachedCapturingMovesForTaflman(taflman, capturingMoves);
        return capturingMoves;
    }

    public static List<Coord> getCapturingMovesFromDestinations(GameState state, char taflman, Coord start, List<Coord> moves) {
        Rules rules = getBoard(state).getRules();
        // Seems very unlikely we'll need to worry about more than
        // five captures all that often.
        List<Coord> capturingMoves = new ArrayList<Coord>(5);

        if (getJumpMode(rules, taflman) == Taflman.JUMP_CAPTURE) {
            capturingMoves.addAll(getJumpsFrom(state, taflman, start));
        }

        for (Coord move : moves) {
            List<Coord> adjacentSpaces = getBoard(state).getAdjacentSpaces(move);
            for (Coord adjacentSpace : adjacentSpaces) {
                if (getBoard(state).getOccupier(adjacentSpace) != EMPTY &&
                        Taflman.isCapturedBy(state, getBoard(state).getOccupier(adjacentSpace), taflman, move, false) &&
                        !capturingMoves.contains(move)) {
                    capturingMoves.add(move);
                }
            }
        }

        return capturingMoves;
    }

    public static List<Coord> getAllowableDestinations(GameState state, char taflman) {
        List<Coord> cachedMoves = state.getCachedAllowableDestinationsForTaflman(taflman);
        if (cachedMoves != null) return cachedMoves;

        List<Coord> allowableDestinations = getAllowableDestinationsFrom(state, taflman, getCurrentSpace(state, taflman));

        state.setCachedAllowableDestinationsForTaflman(taflman, allowableDestinations);

        return allowableDestinations;
    }

    public static List<Coord> getAllowableDestinationsFrom(GameState state, char taflman, Coord space) {
        return getAllowableDestinationsFrom(state, taflman, space, true);
    }

    public static List<Coord> getAllowableDestinationsFrom(GameState state, char taflman, Coord space, boolean withJumps) {
        Rules rules = getBoard(state).getRules();

        List<Coord> allowableMoves = getAllowableMovesFrom(state, taflman, space, withJumps);
        List<Coord> allowableDestinations = new ArrayList<Coord>(allowableMoves.size());

        for (Coord move : allowableMoves) {
            if (rules.canTaflmanStopOn(getBoard(state), taflman, move)) {
                allowableDestinations.add(move);
            }
        }

        if (withJumps) {
            allowableDestinations.addAll(getJumpsFrom(state, taflman, space));
        }

        return allowableDestinations;
    }

    public static List<Coord> getJumpsFor(GameState state, char taflman) {
        List<Coord> cachedJumps = state.getCachedJumpsForTaflman(taflman);
        if(cachedJumps != null) return cachedJumps;

        cachedJumps = getJumpsFrom(state, taflman, getCurrentSpace(state, taflman));
        state.setCachedJumpsForTaflman(taflman, cachedJumps);
        return cachedJumps;
    }

    public static List<Coord> getJumpsFrom(GameState state, char taflman, Coord space) {
        Rules rules = getBoard(state).getRules();

        List<Coord> jumps = new ArrayList<Coord>(4);
        boolean isStartSpecial = false;
        boolean canJump = false;

        if (getJumpMode(rules, taflman) == Taflman.JUMP_RESTRICTED) {
            if (getBoard(state).getSpaceTypeFor(space) != SpaceType.NONE) {
                isStartSpecial = true;
            } else {
                isStartSpecial = false;
            }
        }

        if (getJumpMode(rules, taflman) > 0) {
            canJump = true;
        } else {
            canJump = false;
        }

        // If no jump, return empty list.
        if (!canJump) {
            return jumps;
        }

        for (char neighbor : getBoard(state).getAdjacentNeighbors(space)) {
            if (!Taflman.isKing(neighbor) && !Taflman.isKnight(neighbor) && !Taflman.isCommander(neighbor) && Taflman.getPackedSide(neighbor) != Taflman.getPackedSide(taflman)) {
                int xDif = Taflman.getCurrentSpace(state, neighbor).x - space.x;
                int yDif = Taflman.getCurrentSpace(state, neighbor).y - space.y;

                boolean neighborOnHorizontalEdge =
                        (Taflman.getCurrentSpace(state, neighbor).y == 0 || Taflman.getCurrentSpace(state, neighbor).y == getBoard(state).getBoardDimension() - 1);

                boolean neighborOnVerticalEdge =
                        (Taflman.getCurrentSpace(state, neighbor).x == 0 || Taflman.getCurrentSpace(state, neighbor).x == getBoard(state).getBoardDimension() - 1);

                if (xDif != 0) { // Horizontal jump
                    // Can't horizontal jump a piece on a vertical edge
                    if (neighborOnVerticalEdge) continue;

                    if (xDif > 0) {
                        // Jumping left to right
                        Coord destination = Coord.get(space.x + 2, space.y);
                        destination = checkJumpDestination(state, taflman, destination, isStartSpecial);

                        if (destination != null) jumps.add(destination);
                    } else {
                        // Jumping right to left
                        Coord destination = Coord.get(space.x - 2, space.y);
                        destination = checkJumpDestination(state, taflman, destination, isStartSpecial);

                        if (destination != null) jumps.add(destination);
                    }
                } else if (yDif != 0) { // Vertical jump
                    // Can't vertical jump a piece on a horizontal edge
                    if (neighborOnHorizontalEdge) continue;

                    if (yDif > 0) {
                        // Jumping top to bottom
                        Coord destination = Coord.get(space.x, space.y + 2);
                        destination = checkJumpDestination(state, taflman, destination, isStartSpecial);

                        if (destination != null) jumps.add(destination);
                    } else {
                        // Jumping bottom to top
                        Coord destination = Coord.get(space.x, space.y - 2);
                        destination = checkJumpDestination(state, taflman, destination, isStartSpecial);

                        if (destination != null) jumps.add(destination);
                    }
                }
            }
        }

        return jumps;
    }

    private static Coord checkJumpDestination(GameState state, char taflman, Coord destination, boolean isStartSpecial) {
        Rules rules = getBoard(state).getRules();
        SpaceType group = getBoard(state).getSpaceTypeFor(destination);

        if (getBoard(state).getOccupier(destination) != EMPTY) {
            // Can't jump to an occupied space.
        } else if (group == SpaceType.NONE && !isStartSpecial && getJumpMode(rules, taflman) == Taflman.JUMP_RESTRICTED) {
            // If group is null and start is special and this is a restricted-jump taflman,
            // then we can't jump. If we are able to jump, fall through to standard
            // case.
        } else if (!rules.canTaflmanStopOn(getBoard(state), taflman, destination)) {
            // If the destination is a special space on which this taflman can't stop,
            // then no jump is possible.
        } else {
            // If the space is unoccupied, we're allowed to jump, and we can land on the
            // space, then it's an allowable jump.

            return destination;
        }

        return null;
    }

    public static boolean hasCachedReachableSpaces(char taflman) {
        return false;
    }

    public static List<Coord> getReachableSpaces(GameState state, char taflman) {
        return getReachableSpaces(state, taflman, true);
    }

    public static List<Coord> getReachableSpaces(GameState state, char taflman, boolean withJump) {
        List<Coord> cachedMoves = state.getCachedReachableSpacesForTaflman(taflman);
        if (cachedMoves != null) return cachedMoves;

        // For sanity in surrounding checking, a piece can always reach the space it's already on
        Set<Coord> reachableSpaces = new HashSet<Coord>();
        reachableSpaces.add(Taflman.getCurrentSpace(state, taflman));

        Set<Coord> unvisitedSpaces = new HashSet<Coord>();
        Set<Coord> temporarySpaces = new HashSet<Coord>();
        temporarySpaces.addAll(getAllowableDestinationsFrom(state, taflman, Taflman.getCurrentSpace(state, taflman), withJump));

        while (true) {
            unvisitedSpaces = temporarySpaces;
            temporarySpaces = new HashSet<Coord>();

            for (Coord space : unvisitedSpaces) {
                // For any situation, as far as I can tell, if one taflman can reach a second taflman which meets these
                // conditions:
                // 1. On our side
                // 2. Has zero or one (neighbors + impassable squares) adjacent
                //
                // Then the two taflmen share the same span of reachable spaces.
                // TODO: reimplement this? check if neighbor can jump, and if so, this is only safe if we also jump.
                /*
                for(char neighborPiece : getBoard().getAdjacentNeighbors(space)) {
                    Coord neighbor = Taflman.getCurrentSpace(neighborPiece);
                    if(Taflman.getSide(neighborPiece) == Taflman.getSide(taflman) && neighborPiece.hasCachedReachableSpaces()) {
                        if(getBoard().getAdjacentNeighbors(neighbor).size() + getBoard().getAdjacentImpassableSpaces(neighborPiece).size() < 2) {
                            mCachedReachableSpaces = neighborPiece.getReachableSpaces();
                            return mCachedReachableSpaces;
                        }
                    }
                }
                */

                if (space != reachableSpaces) {
                    reachableSpaces.add(space);
                }

                List<Coord> newSpaces = getAllowableDestinationsFrom(state, taflman, space, withJump);
                for (Coord newSpace : newSpaces) {
                    if (!reachableSpaces.contains(newSpace)) {
                        reachableSpaces.add(newSpace);
                        temporarySpaces.add(newSpace);
                    }
                }
            }

            if (temporarySpaces.size() == 0) break;
        }

        List<Coord> spaces = new ArrayList<Coord>(reachableSpaces);
        state.setCachedReachableSpacesForTaflman(taflman, spaces);
        return spaces;
    }

    // Returns null, or a list of spaces captured
    public static MoveRecord moveTo(GameState state, char taflman, Coord destination, boolean detailed) {
        Rules rules = getBoard(state).getRules();

        boolean wasJump = false;
        boolean wasBerserk = state.getBerserkingTaflman() == taflman;
        List<Coord> captures = new ArrayList<Coord>(4);
        List<Character> capturedTaflmen = new ArrayList<Character>(4);

        List<Coord> allowableDestinations = getAllowableDestinations(state, taflman);
        List<Coord> capturingMoves = null;
        List<Coord> jumps = null;
        Coord start = getCurrentSpace(state, taflman);

        if (allowableDestinations.contains(destination)) {
            taflman |= DEVELOPED;
            capturingMoves = getCapturingMoves(state, taflman);
            jumps = getJumpsFor(state, taflman);


            getBoard(state).setOccupier(destination, taflman);

            if (capturingMoves.contains(destination)) {
                if (getJumpMode(rules, taflman) != Taflman.JUMP_NONE && jumps.contains(destination)) {
                    wasJump = true;
                    int jumpedX = (start.x + destination.x) / 2;
                    int jumpedY = (start.y + destination.y) / 2;
                    char jumpedPiece = getBoard(state).getOccupier(Coord.get(jumpedX, jumpedY));

                    if (jumpedPiece != EMPTY && Taflman.isCapturedBy(state, jumpedPiece, taflman, destination, true)) {
                        captures.add(Taflman.getCurrentSpace(state, jumpedPiece));
                        capturedTaflmen.add(jumpedPiece);
                        Taflman.capturedBy(state, jumpedPiece, taflman, destination, true);
                    }
                }

                for (Coord space : getBoard(state).getAdjacentSpaces(destination)) {
                    char occupier = getBoard(state).getOccupier(space);
                    if (occupier != EMPTY && Taflman.isCapturedBy(state, occupier, taflman, destination, false)) {
                        capturedTaflmen.add(occupier);
                        captures.add(space);
                        Taflman.capturedBy(state, occupier, taflman, destination, false);
                    }
                }
            }
        }

        if(detailed) {
            DetailedMoveRecord m = new DetailedMoveRecord(getBoard(state).getBoardDimension(), start, destination, taflman, captures, capturedTaflmen, wasJump, wasBerserk);
            if(state.mGame.getClock() != null && m.getTimeRemaining() == null) {
                m.setTimeRemaining(state.mGame.getClock().getClockEntry(state.getCurrentSide()).toTimeSpec());
            }
            return m;
        }
        else {
            return new MoveRecord(start, destination, captures);
        }
    }

    /**
     * Will this piece be captured by a piece moving to
     * the given square?
     *
     * @param move
     * @return
     */
    public static boolean isCapturedBy(GameState state, char taflman, char capturer, Coord move, boolean byJump) {
        Rules rules = getBoard(state).getRules();

        // If we're on the same side, don't even worry about it...
        if (Taflman.getPackedSide(capturer) == Taflman.getPackedSide(taflman)) return false;

        Board board = getBoard(state);

        if (byJump) {
            if (Taflman.isKing(taflman) || Taflman.isKnight(taflman) || Taflman.isCommander(taflman)) {
                // Kings, knights, and commanders can't be captured by jump.
                return false;
            } else {
                // Otherwise, we're captured if and only if the capturer
                // can capture by jumping.
                return Taflman.getJumpMode(rules, capturer) == Taflman.JUMP_CAPTURE;
            }
        }

        // Guards can't capture or be captured.
        if (Taflman.isGuard(taflman) || Taflman.isGuard(capturer)) {
            return false;
        }

        // If the capturing piece is an unarmed king, don't check anything
        // else.
        if (Taflman.isKing(capturer)
                && rules.getKingArmedMode() != Rules.KING_ARMED
                && rules.getKingArmedMode() != Rules.KING_HAMMER_ONLY) {
            return false;
        }

        boolean isKingStrong = false;
        boolean fourSpacesRequired = true;
        if(Taflman.isKing(taflman)) {
            if(rules.getKingStrengthMode() == Rules.KING_STRONG) isKingStrong = true;
            else if(rules.getKingStrengthMode() == Rules.KING_STRONG_CENTER) {
                Coord current = getCurrentSpace(state, taflman);
                List<Coord> centerAndAdjacent = getBoard(state).getCenterAndAdjacentSpaces();

                if(centerAndAdjacent.contains(current)) isKingStrong = true;
            }
            else if(rules.getKingStrengthMode() == Rules.KING_MIDDLEWEIGHT) {
                isKingStrong = true;
                fourSpacesRequired = false;
            }
        }

        // If this piece is a strong king, we need to check all four squares
        // around us for hostility.
        //
        // Strong kings can be captured by commander sandwich, or by commander
        // sandwich against the corners, but not against the throne. If the
        // other side has commanders or knights, we can't shortcut here.
        if (Taflman.isKing(taflman) && isKingStrong && !Taflman.getSide(state, capturer).hasCommanders() && !Taflman.getSide(state, capturer).hasKnights()) {
            int hostileAdjacentSpaces = 0;
            List<Coord> adjacentSpaces = board.getAdjacentSpaces(Taflman.getCurrentSpace(state, taflman));

            // Check all four spaces around us for hostility. If all four are hostile,
            // good job, black, you just won the game.
            for (Coord space : adjacentSpaces) {
                if (move == space) {
                    // A hostile piece is asking about moving to this adjacent space,
                    // so although it isn't hostile now, it would be if this move
                    // were made.
                    hostileAdjacentSpaces++;
                } else if (board.isSpaceHostileTo(space, taflman)) {
                    hostileAdjacentSpaces++;
                } else {
                    // Non-hostile adjacent space.
                }
            }

            if (hostileAdjacentSpaces == 4) {
                return true;
            } else if (!fourSpacesRequired && hostileAdjacentSpaces == adjacentSpaces.size()) {
                return true;
            } else {
                return false;
            }
        }

        // Otherwise, this is a normal piece, captured by being sandwiched between two
        // hostile squares.

        // The attempted capture puts a piece on the same file as us (a north-south
        // capture attempt).
        if (Taflman.getCurrentSpace(state, taflman).x == move.x) {

            // If we're at the top or bottom of the board, we can't be captured by
            // sandwich, and we've already accounted for shield walls (see above).
            if (Taflman.getCurrentSpace(state, taflman).y == 0 ||
                    Taflman.getCurrentSpace(state, taflman).y == getBoard(state).getBoardDimension() - 1) {
                return false;
            }

            Coord oneUp = Coord.get(Taflman.getCurrentSpace(state, taflman).x, Taflman.getCurrentSpace(state, taflman).y - 1);
            Coord oneDown = Coord.get(Taflman.getCurrentSpace(state, taflman).x, Taflman.getCurrentSpace(state, taflman).y + 1);

            return checkSandwichCapture(state, taflman, capturer, move, oneUp, oneDown);
        }
        // The attempted capture puts a piece on the same rank as us (an east-west
        // capture attempt).
        else {
            // If we're at the left or right of the board, we can't be captured by
            // sandwich, and we've already accounted for shield walls (see above).
            if (Taflman.getCurrentSpace(state, taflman).x == 0 ||
                    Taflman.getCurrentSpace(state, taflman).x == getBoard(state).getBoardDimension() - 1) {
                return false;
            }
            Coord oneLeft = Coord.get(Taflman.getCurrentSpace(state, taflman).x - 1, Taflman.getCurrentSpace(state, taflman).y);
            Coord oneRight = Coord.get(Taflman.getCurrentSpace(state, taflman).x + 1, Taflman.getCurrentSpace(state, taflman).y);

            return checkSandwichCapture(state, taflman, capturer, move, oneLeft, oneRight);
        }
    }

    private static boolean checkSandwichCapture(GameState state, char taflman, char capturer, Coord move, Coord adjacentOne, Coord adjacentTwo) {
        Rules rules = getBoard(state).getRules();
        Board board = getBoard(state);

        if ((board.isSpaceHostileTo(adjacentOne, taflman) || adjacentOne == move)
                && (board.isSpaceHostileTo(adjacentTwo, taflman) || adjacentTwo == move)) {

            // A piece which can jump can't capture a piece against itself.
            //
            // If one side of the sandwich is the destination, and the other side of the
            // sandwich is the capturer's current location, and there are no special
            // spaces in play, this *is* a jump (conditions 1 and 2) and *can't* be
            // a capture (condition 3).
            SpaceType adjacentOneGroup = getBoard(state).getSpaceTypeFor(adjacentOne);
            SpaceType adjacentTwoGroup = getBoard(state).getSpaceTypeFor(adjacentTwo);
            if (adjacentOne == move && getBoard(state).getOccupier(adjacentTwo) == capturer && adjacentTwoGroup == SpaceType.NONE) {
                return false;
            }

            if (adjacentTwo == move && getBoard(state).getOccupier(adjacentOne) == capturer && adjacentOneGroup == SpaceType.NONE) {
                return false;
            }

            if (Taflman.isKing(taflman)
                    && (rules.getKingStrengthMode() == Rules.KING_STRONG || rules.getKingStrengthMode() == Rules.KING_STRONG_CENTER)
                    && (Taflman.getSide(state, capturer).hasCommanders() || Taflman.getSide(state, capturer).hasKnights())) {
                // If the other side has commanders or knights and this is a strong
                // king, we need to check for commander/knight sandwich.
                if (board.getSpaceTypeFor(adjacentOne) == SpaceType.CENTER || board.getSpaceTypeFor(adjacentTwo) == SpaceType.CENTER) {
                    // If one of the squares in question is a center space, we're safe. Commanders can't capture the king against the
                    // throne.
                    return false;
                } else if (getBoard(state).getSpaceTypeFor(Taflman.getCurrentSpace(state, taflman)) == SpaceType.CENTER) {
                    // Can't be commander-sandwiched on the throne.
                    return false;
                } else {
                    int hostileCorner = 0;
                    int commanderCount = 0;

                    // If one of the adjacent spaces is a corner, that's half of
                    // a capture.

                    if (adjacentOneGroup != SpaceType.NONE || adjacentTwoGroup != SpaceType.NONE) {
                        if (adjacentOneGroup == SpaceType.CORNER && getBoard(state).isSpaceHostileTo(adjacentOne, taflman)) {
                            hostileCorner += 1;
                        }
                        if (adjacentTwoGroup == SpaceType.CORNER && getBoard(state).isSpaceHostileTo(adjacentTwo, taflman)) {
                            hostileCorner += 1;
                        }
                    }

                    // If one adjacent space is occupied by a commander or knight, that's
                    // half a capture.
                    if (getBoard(state).getOccupier(adjacentOne) != EMPTY) {
                        if (Taflman.isCommander(getBoard(state).getOccupier(adjacentOne))
                                || Taflman.isKnight(getBoard(state).getOccupier(adjacentOne))) {
                            commanderCount += 1;
                        }
                    }
                    if (getBoard(state).getOccupier(adjacentTwo) != EMPTY) {
                        if (Taflman.isCommander(getBoard(state).getOccupier(adjacentTwo))
                                || Taflman.isKnight(getBoard(state).getOccupier(adjacentTwo))) {
                            commanderCount += 1;
                        }
                    }
                    // If the moving piece is a commander or knight, that's half a capture.
                    if (Taflman.isCommander(capturer) || Taflman.isKnight(capturer)) commanderCount += 1;

                    // If we have 1 or 2 commanders, and 1 or 0 hostile corners, this is a commander
                    // sandwich capture.
                    if (commanderCount + hostileCorner >= 2) return true;
                    else return false;
                }
            } else if (Taflman.isKing(taflman) && rules.getKingStrengthMode() == Rules.KING_MIDDLEWEIGHT){
                int hostileAdjacentSpaces = 0;
                List<Coord> adjacentSpaces = board.getAdjacentSpaces(Taflman.getCurrentSpace(state, taflman));

                // Check all adjacent spaces for hostility.
                for (Coord space : adjacentSpaces) {
                    if (move == space) {
                        // A hostile piece is asking about moving to this adjacent space,
                        // so although it isn't hostile now, it would be if this move
                        // were made.
                        hostileAdjacentSpaces++;
                    } else if (board.isSpaceHostileTo(space, taflman)) {
                        hostileAdjacentSpaces++;
                    } else {
                        // Non-hostile adjacent space
                        return false;
                    }
                }

                // If no spaces are non-hostile, all spaces are hostile, and we can say that the king is captured.
                return true;
            } else {
                // If we are a strong king with no commanders or knights, we've already left.
                // We've just handled the middleweight king.
                // So, we're either a weak king or a regular piece, and we don't care whether
                // the guys on either side are commanders or knights.
                return true;
            }
        }

        // If none of the above conditions are true, this is not a capture.
        return false;
    }

    /**
     * Capture this piece as a result of a move to the
     * given square.
     *
     * @param capturingMove
     * @return
     */
    public static boolean capturedBy(GameState state, char taflman, char capturer, Coord capturingMove, boolean byJump) {
        Coord originalSpace = null;
        if(isMercenary(taflman)) {
            originalSpace = Taflman.getCurrentSpace(state, taflman);
        }

        getBoard(state).unsetOccupier(taflman);

        if(isMercenary(taflman)) {
            char newTaflman = taflman;
            if(Taflman.getPackedSide(newTaflman) == Taflman.SIDE_ATTACKERS) {
                newTaflman = (char)(newTaflman & ~Taflman.SIDE_ATTACKERS);
            }
            else {
                newTaflman = (char)(newTaflman | Taflman.SIDE_ATTACKERS);
            }

            getBoard(state).setOccupier(originalSpace, newTaflman);
        }
        return true;
    }

    /**
     * Piece cannot jump at all.
     */
    public static final int JUMP_NONE = 0;

    /**
     * Piece can make standard jumps over enemy pieces,
     * provided that it is allowed to stop on the destination.
     */
    public static final int JUMP_STANDARD = 1;

    /**
     * Piece can make jumps as standard, but also captures
     * when jumping.
     */
    public static final int JUMP_CAPTURE = 2;

    /**
     * Piece can make jumps, but does not capture, and can
     * only jump to and from the throne, the corners, or
     * friendly citadels.
     */
    public static final int JUMP_RESTRICTED = 3;

    public static int getJumpMode(Rules r, char taflman) {
        if (isKing(taflman)) return r.getKingJumpMode();
        else if (isKnight(taflman)) return r.getKnightJumpMode();
        else if (isCommander(taflman)) return r.getCommanderJumpMode();
        else if (isMercenary(taflman)) return r.getMercenaryJumpMode();
        else return JUMP_NONE;
    }

    public static String getStringSymbol(char taflman) {
        return getStringSymbol(taflman, 3);
    }

    public static String getStringSymbol(char taflman, int size) {
        String edge1 = "";
        String edge2 = "";
        String symbol = "-";
        if (getPackedSide(taflman) == SIDE_ATTACKERS) {
            edge1 = "[";
            edge2 = "]";
        } else {
            edge1 = "(";
            edge2 = ")";
        }

        if (isKing(taflman)) symbol = "+";
        if (isCommander(taflman)) symbol = "o";
        if (isKnight(taflman)) symbol = "?";
        if (isMercenary(taflman)) symbol = "m";
        if (isGuard(taflman)) symbol = "g";

        if(size == 1) return symbol;
        else if(size == 2) return symbol + symbol;
        else if(size == 3) return edge1 + symbol + edge2;
        else if(size == 4) return edge1 + symbol + symbol + edge2;
        else if(size == 5) return edge1 + symbol + symbol + symbol + edge2;
        else return edge1 + symbol + edge2;
    }

    public static String getOtnStringSymbol(char taflman) {
        String symbol = "";
        if(isKing(taflman)) symbol = "k";
        else if(isCommander(taflman)) symbol = "c";
        else if(isKnight(taflman)) symbol = "n";
        else if(isMercenary(taflman)) symbol = "m";
        else if(isGuard(taflman)) symbol = "g";
        else symbol = "t";

        if(getPackedSide(taflman) == SIDE_ATTACKERS) symbol = symbol.toLowerCase();
        else symbol = symbol.toUpperCase();

        return symbol;
    }
}
