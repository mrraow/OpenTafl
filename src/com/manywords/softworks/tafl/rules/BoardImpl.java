package com.manywords.softworks.tafl.rules;

import com.manywords.softworks.tafl.engine.GameState;
import com.manywords.softworks.tafl.engine.collections.TaflmanCoordMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class BoardImpl extends Board {
    public BoardImpl(int dimension) {

    }

    public BoardImpl(Board board) {
        if (board.getCachedTaflmanLocations() == null && getState() != null) {
            setupTaflmen(getState().getAttackers(), getState().getDefenders());
        } else if(board.getCachedTaflmanLocations() != null){
            mCachedTaflmanLocations = new TaflmanCoordMap(board.getCachedTaflmanLocations());
        }
    }

    private TaflmanCoordMap mCachedTaflmanLocations = null;
    private Rules mRules;
    private GameState mState;

    @Override
    public void setupTaflmen(Side attackers, Side defenders) {
        initializeTaflmanLocations(attackers, defenders);
    }

    private void initializeTaflmanLocations(Side attackers, Side defenders) {
        short defenderCount = (short) defenders.getStartingTaflmen().size();
        short attackerCount = (short) attackers.getStartingTaflmen().size();
        mCachedTaflmanLocations = new TaflmanCoordMap(getBoardDimension(), attackerCount, defenderCount);

        for(Side.TaflmanHolder t : attackers.getStartingTaflmen()) {
            mCachedTaflmanLocations.put(t.packed, t.coord);
        }

        for(Side.TaflmanHolder t : defenders.getStartingTaflmen()) {
            mCachedTaflmanLocations.put(t.packed, t.coord);
        }
    }

    public Coord findTaflmanSpace(char taflman) {
        return mCachedTaflmanLocations.getCoord(taflman);
    }

    public List<Character> getTaflmenWithMask(char mask, char value) {
        List<Character> taflmen = new ArrayList<Character>();

        for(char taflman : mCachedTaflmanLocations.getTaflmen()) {
            if(taflman != Taflman.EMPTY && (taflman & mask) == value) {
                taflmen.add(taflman);
            }
        }

        return taflmen;
    }

    @Override
    public abstract int getBoardDimension();

    @Override
    public char[][] getBoardArray() {
        char[][] boardArray = new char[getBoardDimension()][getBoardDimension()];

        for(char taflman : getCachedTaflmanLocations().getTaflmen()) {
            Coord c = getCachedTaflmanLocations().getCoord(taflman);

            if(c == null) continue;
            boardArray[c.y][c.x] = taflman;
        }

        return boardArray;
    }

    @Override
    public boolean isEdgeSpace(Coord space) {
        if ((space.x == 0) || (space.x == getBoardDimension() - 1) ||
                ((space.y == 0 || space.y == getBoardDimension() - 1))) {
            return true;
        }

        return false;
    }

    @Override
    public List<Coord> getCenterAndAdjacentSpaces() {
        Set<Coord> spaces = new HashSet<>(5);
        for(Coord space : getRules().getCenterSpaces()) {
            spaces.add(space);
            spaces.addAll(getAdjacentSpaces(space));
        }

        return new ArrayList<>(spaces);
    }

    @Override
    public SpaceType getSpaceTypeFor(Coord space) {
        return getRules().getSpaceTypeFor(space);
    }

    @Override
    public List<Coord> getAdjacentSpaces(Coord space) {
        return Coord.getAdjacentSpaces(getBoardDimension(), space);
    }

    @Override
    public List<Character> getAdjacentNeighbors(Coord space) {
        List<Coord> spaces = getAdjacentSpaces(space);
        List<Character> neighbors = new ArrayList<Character>(4);

        for (Coord adjacent : spaces) {
            char occupier = getOccupier(adjacent);
            if (occupier != Taflman.EMPTY) neighbors.add(occupier);
        }

        return neighbors;
    }

    @Override
    public List<Coord> getAdjacentImpassableSpaces(char taflman) {
        List<Coord> impassableSpaces = new ArrayList<Coord>(4);

        if (Taflman.getCurrentSpace(mState, taflman) != null) {
            for (Coord adjacent : getAdjacentSpaces(Taflman.getCurrentSpace(mState, taflman))) {
                if (!getRules().canTaflmanMoveThrough(this, taflman, adjacent)) {
                    impassableSpaces.add(adjacent);
                }
            }
        }

        return impassableSpaces;
    }

    @Override
    public List<Coord> getDiagonalSpaces(Coord space) {
        return Coord.getDiagonalSpaces(getBoardDimension(), space);
    }

    @Override
    public List<Character> getDiagonalNeighbors(Coord space) {
        List<Coord> spaces = getDiagonalSpaces(space);
        List<Character> neighbors = new ArrayList<Character>(4);

        for (Coord diagonal : spaces) {
            char occupier = getOccupier(diagonal);
            if (occupier != 0) neighbors.add(occupier);
        }

        return neighbors;
    }

    @Override
    public void setState(GameState state) {
        mState = state;
    }

    @Override
    public GameState getState() {
        return mState;
    }

    @Override
    public Rules getRules() {
        return mRules;
    }

    @Override
    public void setRules(Rules rules) {
        mRules = rules;
    }

    @Override
    public void setOccupier(Coord space, char taflman) {
        if(taflman == Taflman.EMPTY) throw new IllegalArgumentException("Must be called with nonempty taflman");
        mCachedTaflmanLocations.put(taflman, space);
    }

    @Override
    public void unsetOccupier(char taflman) {
        mCachedTaflmanLocations.remove(taflman);
    }

    @Override
    public char getOccupier(int x, int y) {
        return getOccupier(Coord.get(x, y));
    }

    @Override
    public char getOccupier(Coord coord) {
        return mCachedTaflmanLocations.getTaflman(coord);
    }

    @Override
    public TaflmanCoordMap getCachedTaflmanLocations() {
        return mCachedTaflmanLocations;
    }

    @Override
    public boolean isSpaceHostileTo(Coord space, char taflman) {
        // If the space contains a piece from a different side, it's
        // hostile.
        char occupier = getOccupier(space);
        boolean isHostile = getRules().isSpaceHostileToSide(this, space, Taflman.getSide(getState(), taflman));
        if (occupier != 0 && Taflman.getPackedSide(occupier) != Taflman.getPackedSide(taflman)) {

            // The exceptions are if the hostile piece is a king, and that king
            // is unarmed, or if that piece is a guard.
            if (Taflman.isKing(occupier)
                    && getRules().getKingArmedMode() != Rules.KING_ARMED
                    && getRules().getKingArmedMode() != Rules.KING_ANVIL_ONLY) {
                return isHostile;
            } else return !Taflman.isGuard(occupier);
        }

        // If this space isn't naturally hostile, then this isn't a hostile space.
        return isHostile;
    }

    /**
     * Encircled in tafl means prevented from reaching the edges by the other
     * side.
     *
     * @return
     */

    @Override
    public boolean isSideEncircled(Side side) {
        for(char taflman : side.getTaflmen()) {
            for(Coord c : Taflman.getAllowableDestinations(getState(), taflman)) {
                if(isEdgeSpace(c)) return false;
            }
        }
        // Start at the edges.
        List<Coord> edges = getEdgesFlat();
        Set<Coord> considered = new HashSet<Coord>(getBoardDimension() * getBoardDimension());
        Set<Coord> toExplore = new HashSet<Coord>(getBoardDimension() * 4);
        toExplore.addAll(edges);

        // Flow inward.
        while (toExplore.size() > 0) {
            Coord consider = null;

            // get the first element.
            for (Coord coord : toExplore) {
                consider = coord;
                break;
            }

            toExplore.remove(consider);
            considered.add(consider);
            char taflman = getOccupier(consider);

            // If this space contains a taflman, then we don't need to consider any of its neighbors.
            // All of its neighbors outside the potential encirclement will be snagged by the spaces
            // adjacent to the one that added this space in the first place.
            if (taflman != Taflman.EMPTY) {
                // If this space contains a taflman of the side in question, then the side can be
                // reached from an edge, and isn't surrounded.
                if (Taflman.getSide(getState(), taflman).isAttackingSide() == side.isAttackingSide()) return false;
            } else {
                // If this space does not contain a taflman, add all of its neighbors which aren't scheduled
                // for consideration and haven't already been considered.
                List<Coord> neighbors = getAdjacentSpaces(consider);
                for (Coord neighbor : neighbors) {
                    if (!considered.contains(neighbor)) {
                        toExplore.add(neighbor);
                    }
                }
            }
        }

        return true;
    }

    @Override
    public List<ShieldwallPosition> detectShieldwallPositionsForSide(Side surrounders, Side surrounded) {
        // Algorithm thought: the simplest way to find shieldwalls is to search
        // exhaustively.
        //
        // 1. For each edge space from 0 to dimension:
        //     2. Is occupied by side or corner, and no start position?
        //     3. Then mark as start position and continue.
        //     4. Is occupied by side and start position not null?
        //         5. Is positive neighbor *and* edge-adjacent space occupied by side?
        //         6. Then add space to surrounded list and continue
        //         7. Special case: if in a shieldwall and king,
        //         7. Else mark end space and stop
        //             8. Set start position to end space
        //             9. Is surrounded list size > 1?
        //            10. Then add to shieldwall positions list
        //    11. Is edge-adjacent space occupied by side?
        //    12. Then add space to surrounded list and continue.
        //    13. Else clear start space and surrounded spaces list.
        List<ShieldwallPosition> shieldwallPositions = new ArrayList<ShieldwallPosition>();

        // Some short-circuit optimizations: I need at least two pieces on the edge to form a shieldwall.
        // The other side needs at least two pieces on the edge to be shieldwalled.
        int attackersOnEdge = 0;
        int defendersOnEdge = 0;

        // Check defenders first: they're less likely to be on the edge
        if(surrounders.isAttackingSide()) {
            for(char taflman : surrounded.getTaflmen()) {
                Coord space = findTaflmanSpace(taflman);
                if(space != null && isEdgeSpace(findTaflmanSpace(taflman))) defendersOnEdge++;

                if(defendersOnEdge >= 2) break;
            }
            if(defendersOnEdge < 2) return shieldwallPositions;

            for(char taflman : surrounders.getTaflmen()) {
                Coord space = findTaflmanSpace(taflman);
                if(space != null && isEdgeSpace(findTaflmanSpace(taflman))) attackersOnEdge++;

                if(attackersOnEdge >= 2) break;
            }
            if(attackersOnEdge < 2) return shieldwallPositions;
        }
        else {
            for(char taflman : surrounders.getTaflmen()) {
                Coord space = findTaflmanSpace(taflman);
                if(space != null && isEdgeSpace(findTaflmanSpace(taflman))) defendersOnEdge++;

                if(defendersOnEdge >= 2) break;
            }
            if(defendersOnEdge < 2) return shieldwallPositions;

            for(char taflman : surrounded.getTaflmen()) {
                Coord space = findTaflmanSpace(taflman);
                if(space != null && isEdgeSpace(findTaflmanSpace(taflman))) attackersOnEdge++;

                if(attackersOnEdge >= 2) break;
            }
            if(attackersOnEdge < 2) return shieldwallPositions;
        }

        Coord startPosition = null;
        Coord endPosition = null;
        int direction = DIRECTION_X;
        int index = 0;
        List<Coord> surroundedSpaces = new ArrayList<Coord>();
        List<Character> surroundingTaflmen = new ArrayList<Character>();

        List<List<Coord>> edges = getEdges();

        for (List<Coord> edge : edges) {
            startPosition = null;
            endPosition = null;
            if (edge.contains(Coord.get(0, 1)) || edge.contains(Coord.get(getBoardDimension() - 1, 1))) {
                direction = DIRECTION_Y;
            } else {
                direction = DIRECTION_X;
            }
            index = 0;

            for (Coord space : edge) {
                // If the space is a corner and start position is null, this is a potential
                // start position.
                if (startPosition == null && getRules().isCornerSpace(space)
                        && getRules().allowShieldWallCaptures() == Rules.STRONG_SHIELDWALL) {
                    startPosition = space;
                    continue;
                }
                // If start position is null and this space is occupied, this is a potential
                // start position.
                else if (startPosition == null && getOccupier(space) != 0 && Taflman.getPackedSide(getOccupier(space)) == surrounders.getSideChar()) {
                    startPosition = space;
                    surroundingTaflmen.add(getOccupier(space));
                    continue;
                }

                // If start position isn't null, check to see if the position has ended.
                if (startPosition != null) {
                    // STRONG_SHIELDWALL means corners can cap shieldwalls.
                    if (getRules().isCornerSpace(space)
                            && getRules().allowShieldWallCaptures() == Rules.STRONG_SHIELDWALL) {
                        endPosition = space;

                        if (surroundedSpaces.size() > 1) {
                            shieldwallPositions.add(new ShieldwallPosition(surroundedSpaces, surroundingTaflmen));
                        }

                        break;
                    }

                    // If this space is occupied by a friendly, it's close to the end
                    // of the shieldwall, for our purposes.
                    // There are special cases for shieldwalls of this form, with stars being pieces:
                    // ** *
                    //  **
                    // (That is, shieldwalls surrounding two spaces, one of which contains a friendly
                    // piece.) We don't need to catch bigger arrangements like that; any larger
                    // 'imperfect shieldwall' of this sort just look like the minimum size shieldwall,
                    // and that's okay from a rules perspective.
                    if (getOccupier(space) != 0 && Taflman.getPackedSide(getOccupier(space)) == surrounders.getSideChar()) {
                        if (getOccupier(edge.get(index + 1)) != 0
                                && Taflman.getPackedSide(getOccupier(edge.get(index + 1))) == surrounders.getSideChar()
                                && getOccupier(getEdgeAdjacentSpace(direction, space)) != 0
                                && Taflman.getPackedSide(getOccupier(getEdgeAdjacentSpace(direction, space))) == surrounders.getSideChar()) {
                            surroundingTaflmen.add(getOccupier(getEdgeAdjacentSpace(direction, space)));
                            surroundedSpaces.add(space);
                            continue;
                        } else if (getOccupier(space) != 0 && Taflman.isKing(getOccupier(space))
                                && getOccupier(getEdgeAdjacentSpace(direction, space)) != 0
                                && Taflman.getPackedSide(getOccupier(getEdgeAdjacentSpace(direction, space))) == surrounders.getSideChar()) {
                            surroundingTaflmen.add(getOccupier(getEdgeAdjacentSpace(direction, space)));
                            surroundedSpaces.add(space);
                            continue;
                        } else {
                            endPosition = space;
                            if (surroundedSpaces.size() > 1) {
                                surroundingTaflmen.add(getOccupier(space));
                                shieldwallPositions.add(new ShieldwallPosition(surroundedSpaces, surroundingTaflmen));
                            }

                            startPosition = space;
                            endPosition = null;
                            surroundingTaflmen = new ArrayList<Character>();
                            surroundedSpaces = new ArrayList<Coord>();
                            continue;
                        }
                    }

                    if (getOccupier(getEdgeAdjacentSpace(direction, space)) != 0
                            && Taflman.getPackedSide(getOccupier(getEdgeAdjacentSpace(direction, space))) == surrounders.getSideChar()) {
                        surroundingTaflmen.add(getOccupier(getEdgeAdjacentSpace(direction, space)));
                        surroundedSpaces.add(space);
                    } else {
                        startPosition = null;
                        endPosition = null;
                        surroundingTaflmen = new ArrayList<Character>();
                        surroundedSpaces = new ArrayList<Coord>();
                    }
                }

                index++;
            }
        }

        return shieldwallPositions;
    }

    public List<List<Coord>> getEdges() {
        List<List<Coord>> edges = new ArrayList<List<Coord>>(4);
        edges.add(getTopEdge());
        edges.add(getBottomEdge());
        edges.add(getLeftEdge());
        edges.add(getRightEdge());

        return edges;
    }

    public List<Coord> getEdgesFlat() {
        return Coord.getEdgesFlat(getBoardDimension());
    }

    @Override
    public List<Coord> getTopEdge() {
        return Coord.getTopEdge(getBoardDimension());
    }

    @Override
    public List<Coord> getBottomEdge() {
        return Coord.getBottomEdge(getBoardDimension());
    }

    @Override
    public List<Coord> getLeftEdge() {
        return Coord.getLeftEdge(getBoardDimension());
    }

    @Override
    public List<Coord> getRightEdge() {
        return Coord.getRightEdge(getBoardDimension());
    }

    public static final int DIRECTION_X = 0;
    public static final int DIRECTION_Y = 1;

    public Coord getEdgeAdjacentSpace(int direction, Coord space) {
        if (!isEdgeSpace(space)) {
            return null;
        }

        if (direction == DIRECTION_X) {
            if (space.y == 0) {
                return Coord.get(space.x, 1);
            } else {
                return Coord.get(space.x, getBoardDimension() - 2);
            }
        } else {
            if (space.x == 0) {
                return Coord.get(1, space.y);
            } else {
                return Coord.get(getBoardDimension() - 2, space.y);
            }
        }
    }

    @Override
    public abstract Board deepCopy();
}
