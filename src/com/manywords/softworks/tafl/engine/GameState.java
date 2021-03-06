package com.manywords.softworks.tafl.engine;

import com.manywords.softworks.tafl.engine.ai.GameTreeState;
import com.manywords.softworks.tafl.notation.PositionSerializer;
import com.manywords.softworks.tafl.notation.RulesSerializer;
import com.manywords.softworks.tafl.rules.*;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    public GameState(Game game, Rules startingRules) {
        mGameLength = 0;
        mBoard = startingRules.getBoard().deepCopy();
        mAttackers = startingRules.getAttackers().deepCopy(mBoard);
        mDefenders = startingRules.getDefenders().deepCopy(mBoard);
        mGame = game;

        mBoard.setState(this);
        mBoard.setupTaflmen(mAttackers, mDefenders);

        if (mBoard.getRules().getStartingSide().isAttackingSide()) {
            mCurrentSide = mAttackers;
        } else {
            mCurrentSide = mDefenders;
        }

        mZobristHash = zobristHash();
        TaflmanMoveCache.reset(getBoard().getBoardDimension(), mZobristHash, (byte) startingRules.howManyAttackers(), (byte) startingRules.howManyDefenders());
    }

    public GameState(Game game, Rules startingRules, Board board, Side attackers, Side defenders) {
        mBoard = board;
        mAttackers = attackers;
        mDefenders = defenders;
        mGame = game;
        mBoard.setState(this);
        mBoard.setupTaflmen(attackers, defenders);

        if (mBoard.getRules().getStartingSide().isAttackingSide()) {
            mCurrentSide = mAttackers;
        } else {
            mCurrentSide = mDefenders;
        }

        mZobristHash = zobristHash();
        TaflmanMoveCache.reset(getBoard().getBoardDimension(), mZobristHash, (byte) startingRules.howManyAttackers(), (byte) startingRules.howManyDefenders());
    }

    public GameState(Game game, GameState previousState, Board board, Side attackers, Side defenders, boolean updateZobrist) {
        this(previousState);

        updateGameState(game, previousState, board, attackers, defenders, updateZobrist, Taflman.EMPTY);
    }

    public GameState(int moveErrorCode) {
        mLastMoveResult = moveErrorCode;
    }

    public GameState(GameState copyState) {
        mGameLength = copyState.mGameLength;
        mBoard = copyState.getBoard().deepCopy();
        mBoard.setState(this);
        mAttackers = copyState.getAttackers().deepCopy(mBoard);
        mDefenders = copyState.getDefenders().deepCopy(mBoard);
        mZobristHash = copyState.mZobristHash;
        mGame = copyState.mGame;
        mCurrentSide = (copyState.getCurrentSide().isAttackingSide() ? mAttackers : mDefenders);
        mExitingMove = copyState.getExitingMove();
        mDetailedExitingMove = copyState.mDetailedExitingMove;
        mEnteringMove = copyState.getEnteringMove();
        mBerserkingTaflman = copyState.getBerserkingTaflman();
        TaflmanMoveCache.reset(getBoard().getBoardDimension(), mZobristHash, (byte) mGame.getRules().howManyAttackers(), (byte) mGame.getRules().howManyDefenders());
    }

    public void updateBoard(GameState previousState) {
        mBoard = previousState.getBoard().deepCopy();
        mBoard.setState(this);
    }

    public void updateGameState(Game game, GameState previousState, Board board, Side attackers, Side defenders, boolean updateZobrist, char berserkingTaflman) {
        mBoard = board.deepCopy();
        mBoard.setState(this);
        mAttackers = attackers.deepCopy(mBoard);
        mDefenders = defenders.deepCopy(mBoard);
        mGame = game;
        mGameLength = (char)(previousState.mGameLength + 1);
        mEnteringMove = previousState.getExitingMove();

        boolean changeSides = true;

        TaflmanMoveCache.invalidate();

        if(berserkingTaflman != Taflman.EMPTY) {
            int x = Taflman.getCurrentSpace(this, berserkingTaflman).x;
            int y = Taflman.getCurrentSpace(this, berserkingTaflman).y;

            char taflman = getPieceAt(x, y);

            if (getBoard().getRules().getBerserkMode() == Rules.BERSERK_CAPTURE_ONLY) {
                if (Taflman.getCapturingMoves(this, taflman).size() > 0) {
                    setBerserkingTaflman(taflman);
                    changeSides = false;
                }
                else {
                    setBerserkingTaflman(Taflman.EMPTY);
                    changeSides = true;
                }
            }
            else if (getBoard().getRules().getBerserkMode() == Rules.BERSERK_ANY_MOVE) {
                if (Taflman.getAllowableMoves(this, taflman).size() > 0) {
                    setBerserkingTaflman(taflman);
                    changeSides = false;
                }
                else {
                    setBerserkingTaflman(Taflman.EMPTY);
                    changeSides = true;
                }
            }
        }

        if(updateZobrist) {
            mZobristHash = updateZobristHash(previousState.mZobristHash, previousState.getBoard(), previousState.getExitingMove(), changeSides);
        }

        TaflmanMoveCache.reset(getBoard().getBoardDimension(), mZobristHash, (byte) mGame.getRules().howManyAttackers(), (byte) mGame.getRules().howManyDefenders());

        if (changeSides) {
            if (previousState.getCurrentSide().isAttackingSide()) setCurrentSide(getDefenders());
            else setCurrentSide(getAttackers());
        } else {
            if (previousState.getCurrentSide().isAttackingSide()) setCurrentSide(getAttackers());
            else setCurrentSide(getDefenders());
        }
    }

    public Game mGame;
    public int mLastMoveResult;
    protected int mVictory = VICTORY_UNCHECKED;
    public long mZobristHash;
    private Board mBoard;
    private Side mAttackers;
    private Side mDefenders;
    private Side mCurrentSide;
    private char mBerserkingTaflman;
    protected char mGameLength;

    /**
     * This move object is a concise representation of what we moved to where.
     */
    protected MoveRecord mExitingMove;
    protected DetailedMoveRecord mDetailedExitingMove;
    protected MoveRecord mEnteringMove;

    public MoveRecord getExitingMove() {
        return mExitingMove;
    }
    public MoveRecord getEnteringMove() { return mEnteringMove; }
    public int getLastMoveResult() { return mLastMoveResult; }

    public Side setCurrentSide(Side side) {
        mCurrentSide = side;
        return mCurrentSide;
    }

    public void setBerserkingTaflman(char taflman) {
        mBerserkingTaflman = taflman;
    }

    public char getBerserkingTaflman() {
        return mBerserkingTaflman;
    }

    public Side getCurrentSide() {
        return mCurrentSide;
    }

    public Board getBoard() {
        return mBoard;
    }

    public Side getAttackers() {
        return mAttackers;
    }

    public Side getDefenders() {
        return mDefenders;
    }

    public char getPieceAt(int x, int y) {
        return mBoard.getOccupier(x, y);
    }

    public Coord getSpaceAt(int x, int y) {
        return Coord.get(x, y);
    }

    public void setCachedAllowableMovesForTaflman(char taflman, List<Coord> moves) {
        TaflmanMoveCache.setCachedAllowableMovesForTaflman(mZobristHash, taflman, moves);
    }

    public void setCachedAllowableDestinationsForTaflman(char taflman, List<Coord> moves) {
        TaflmanMoveCache.setCachedAllowableDestinationsForTaflman(mZobristHash, taflman, moves);
    }

    public void setCachedJumpsForTaflman(char taflman, List<Coord> jumps) {
        TaflmanMoveCache.setCachedJumpsForTaflman(mZobristHash, taflman, jumps);
    }

    public void setCachedCapturingMovesForTaflman(char taflman, List<Coord> moves) {
        TaflmanMoveCache.setCachedCapturingMovesForTaflman(mZobristHash, taflman, moves);
    }

    public void setCachedReachableSpacesForTaflman(char taflman, List<Coord> moves) {
        TaflmanMoveCache.setCachedReachableSpacesForTaflman(mZobristHash, taflman, moves);
    }

    public List<Coord> getCachedAllowableMovesForTaflman(char taflman) {
        return TaflmanMoveCache.getCachedAllowableMovesForTaflman(mZobristHash, taflman);
    }

    public List<Coord> getCachedAllowableDestinationsForTaflman(char taflman) {
        return TaflmanMoveCache.getCachedAllowableDestinationsForTaflman(mZobristHash, taflman);
    }

    public List<Coord> getCachedJumpsForTaflman(char taflman) {
        return TaflmanMoveCache.getCachedJumpsForTaflman(mZobristHash, taflman);
    }

    public List<Coord> getCachedCapturingMovesForTaflman(char taflman) {
        return TaflmanMoveCache.getCachedCapturingMovesForTaflman(mZobristHash, taflman);
    }

    public List<Coord> getCachedReachableSpacesForTaflman(char taflman) {
        return TaflmanMoveCache.getCachedReachableSpacesForTaflman(mZobristHash, taflman);
    }

    public static String getStringForMoveResult(int result) {
        switch (result) {
            case GOOD_MOVE:
                return "";
            case DRAW:
                return "Draw.";
            case DEFENDER_WIN:
                return "Defender win!";
            case ATTACKER_WIN:
                return "Attacker win!";
            case ILLEGAL_SIDE:
                return "Not your turn/taflman.";
            case ILLEGAL_SIDE_BERSERKER:
                return "The berserking taflman must move first.";
            case ILLEGAL_MOVE:
                return "Illegal move.";
            case ILLEGAL_MOVE_BERSERKER:
                return "The berserking taflman must make a berserk move.";
            case GOOD_MOVE_NOT_IN_PUZZLE:
                return "This move does not occur in the puzzle solution.";
            case STRICT_PUZZLE_MISSING_MOVE:
                return "This move does not occur in the puzzle solution, and has been ignored.";
        }

        return "Unknown move result! Please report this as a bug.";
    }

    // TODO: refactor to bitwise flag or something
    // I want to be able to capture e.g. DEFENDER_WIN by GOOD_MOVE_NOT_IN_PUZZLE
    public static final int DRAW = 4;
    public static final int DEFENDER_WIN = 3;
    public static final int ATTACKER_WIN = 2;
    public static final int GOOD_MOVE_NOT_IN_PUZZLE = 1;
    public static final int GOOD_MOVE = 0;
    public static final int ILLEGAL_SIDE = -1;
    public static final int ILLEGAL_SIDE_BERSERKER = -2;
    public static final int ILLEGAL_MOVE = -3;
    public static final int ILLEGAL_MOVE_BERSERKER = -4;
    public static final int STRICT_PUZZLE_MISSING_MOVE = -5;
    public static final int TRANSPOSITION_HIT = -49;
    public static final int VICTORY_UNCHECKED = -50;

    public static final int HIGHEST_NONTERMINAL_RESULT = GOOD_MOVE_NOT_IN_PUZZLE;
    public static final int LOWEST_NONERROR_RESULT = GOOD_MOVE;

    protected GameState moveTaflman(char taflman, Coord destination) {
        if (mBerserkingTaflman != Taflman.EMPTY && Taflman.getSide(this, taflman).isAttackingSide() != getCurrentSide().isAttackingSide()) {
            return new GameState(ILLEGAL_SIDE_BERSERKER);
        }

        if (Taflman.getSide(this, taflman).isAttackingSide() != getCurrentSide().isAttackingSide()) {
            return new GameState(ILLEGAL_SIDE);
        }

        if (mBerserkingTaflman != Taflman.EMPTY && taflman != mBerserkingTaflman) {
            return new GameState(ILLEGAL_MOVE_BERSERKER);
        }

        List<Coord> moves = Taflman.getAllowableDestinations(this, taflman);
        if (!moves.contains(destination)) {
            return new GameState(ILLEGAL_MOVE);
        }
        else {
            GameState nextState = new GameState(this);

            boolean detailed = !(this instanceof GameTreeState);
            MoveRecord move = Taflman.moveTo(nextState, taflman, destination, detailed);
            List<Coord> captures = move.captures;

            if (getBoard().getRules().allowShieldWallCaptures() > Rules.NO_SHIELDWALL) {
                List<ShieldwallPosition> shieldwallPositionsAttackers = nextState.getBoard().detectShieldwallPositionsForSide(getAttackers(), getDefenders());
                List<ShieldwallPosition> shieldwallPositionsDefenders = nextState.getBoard().detectShieldwallPositionsForSide(getDefenders(), getAttackers());


                for (ShieldwallPosition position : shieldwallPositionsAttackers) {
                    captures.addAll(nextState.checkShieldwallPositionForCaptures(taflman, destination, position));
                }

                for (ShieldwallPosition position : shieldwallPositionsDefenders) {
                    captures.addAll(nextState.checkShieldwallPositionForCaptures(taflman, destination, position));
                }
            }

            if (getBoard().getRules().allowLinnaeanCaptures() && Taflman.getPackedSide(taflman) == Taflman.SIDE_ATTACKERS) {
                Coord linnaeanCaptureCoord = checkLinnaeanCapture(destination);

                if(linnaeanCaptureCoord != null) {
                    char occupier = mBoard.getOccupier(linnaeanCaptureCoord);
                    if(occupier != Taflman.EMPTY) {
                        Taflman.capturedBy(nextState, occupier, taflman, destination, false);
                        captures.add(linnaeanCaptureCoord);
                    }
                }
            }

            nextState.mEnteringMove = move;
            mExitingMove = move;

            if(detailed) {
                mDetailedExitingMove = (DetailedMoveRecord) mExitingMove;
            }

            if (captures.size() > 0 && getBoard().getRules().getBerserkMode() > 0) {
                nextState.setBerserkingTaflman(taflman);
            } else {
                nextState.setBerserkingTaflman(Taflman.EMPTY);
            }

            return nextState;
        }
    }

    private List<Coord> checkShieldwallPositionForCaptures(char potentialCapturer, Coord destination, ShieldwallPosition position) {
        List<Coord> surroundedByShieldwall = position.surroundedSpaces;
        List<Coord> captures = new ArrayList<Coord>();

        if (getBoard().getRules().allowFlankingShieldwallCapturesOnly()) {
            // Flanking captures require a movement to an edge space
            if (!getBoard().isEdgeSpace(destination)) {
                return captures;
            }
            // If this shieldwall position doesn't contain the capturer, then nothing happens.
            else if (!position.surroundingTaflmen.contains(potentialCapturer)) {
                return captures;
            }
        }

        // Guards can't close a shieldwall capture.
        if(Taflman.isGuard(potentialCapturer)) {
            return captures;
        }

        boolean capturingShieldwall = true;
        for (Coord space : surroundedByShieldwall) {
            if (mBoard.getOccupier(space) == Taflman.EMPTY || Taflman.getPackedSide(mBoard.getOccupier(space)) == Taflman.getPackedSide(potentialCapturer)) {
                capturingShieldwall = false;
            }
        }

        if (capturingShieldwall) {
            for (Coord space : surroundedByShieldwall) {
                char occupier = mBoard.getOccupier(space);
                char type = Taflman.getPackedType(occupier);
                if(type != Taflman.TYPE_KING && type != Taflman.TYPE_GUARD) {
                    Taflman.capturedBy(this, mBoard.getOccupier(space), potentialCapturer, destination, false);
                    captures.add(space);
                }
            }
        }

        return captures;
    }

    public int countPositionOccurrences() {
        return mGame.getRepetitions().getRepetitionCount(this.mZobristHash);
    }

    public void winByResignation(boolean isWinnerAttackingSide) {
        if(isWinnerAttackingSide) mVictory = ATTACKER_WIN;
        else mVictory = DEFENDER_WIN;
    }

    public int checkVictory() {
        if(mVictory == VICTORY_UNCHECKED) {
            mVictory = checkVictoryInternal();
        }
        return mVictory;
    }

    private int checkVictoryInternal() {
        if(getAttackers().getTaflmen().size() == 0) return DEFENDER_WIN;
        else if (getDefenders().getTaflmen().size() == 0) return ATTACKER_WIN;
        int threefoldRepetitionResult = mGame.getRules().threefoldRepetitionResult();
        // Threefold repetition cannot occur as the result of a berserk move
        if(threefoldRepetitionResult != Rules.THIRD_REPETITION_IGNORED && mBerserkingTaflman == Taflman.EMPTY) {
            int repeats = countPositionOccurrences();

            // If this position has occurred three times...
            if(repeats > 2) {
                if(threefoldRepetitionResult == Rules.THIRD_REPETITION_DRAWS) {
                    return DRAW;
                }
                else if (threefoldRepetitionResult == Rules.THIRD_REPETITION_LOSES) {
                    return (getCurrentSide().isAttackingSide() ? DEFENDER_WIN : ATTACKER_WIN);
                }
                else if (threefoldRepetitionResult == Rules.THIRD_REPETITION_WINS) {
                    return (getCurrentSide().isAttackingSide() ? ATTACKER_WIN : DEFENDER_WIN);
                }
            }
        }

        boolean kingAlive = false;
        boolean defenderMovesAvailable = false;

        for (char taflman : getDefenders().getTaflmen()) {
            // King-related win conditions
            if (Taflman.isKing(taflman)) {
                kingAlive = true;
                if (getBoard().getRules().getEscapeType() == Rules.EDGES &&
                        getBoard().isEdgeSpace(Taflman.getCurrentSpace(this, taflman))) {
                    return DEFENDER_WIN;
                } else if (getBoard().getRules().getEscapeType() == Rules.CORNERS) {
                    for (Coord corner : mGame.getRules().getCornerSpaces()) {
                        if (mBoard.getOccupier(corner) == taflman) {
                            return DEFENDER_WIN;
                        }
                    }
                }
            }
        }

        defenderLoop:
        for (char taflman : getDefenders().getTaflmen()) {
            Coord space = Taflman.getCurrentSpace(this, taflman);
            if (space != null) {
                for(Coord c : getBoard().getAdjacentSpaces(space)) {
                    if(getBoard().getOccupier(c) == Taflman.EMPTY) {
                        defenderMovesAvailable = true;
                        break defenderLoop;
                    }
                }
            }
        }

        if (!kingAlive) {
            return ATTACKER_WIN;
        }

        if (!defenderMovesAvailable) {
            return ATTACKER_WIN;
        }

		/* Handle edge fort escapes */
        if (getBoard().getRules().allowEdgeFortEscapes()) {
            List<ShieldwallPosition> defenderShieldwalls = getBoard().detectShieldwallPositionsForSide(getDefenders(), getAttackers());

            // A shieldwall shape is a subset of all invincible shapes, so don't bother checking them for
            // invincibility
            for (ShieldwallPosition position : defenderShieldwalls) {
                List<Coord> shieldwallInterior = position.surroundedSpaces;
                for (Coord space : shieldwallInterior) {
                    if (mBoard.getOccupier(space) != Taflman.EMPTY
                            && Taflman.isKing(mBoard.getOccupier(space))
                            && Taflman.getAllowableDestinations(this, mBoard.getOccupier(space)).size() > 0) {
                        return DEFENDER_WIN;
                    }
                }
            }

            // Do the exhaustive edge fort check
            if (checkEdgeFortEscape()) {
                return DEFENDER_WIN;
            }
        }

        // Surrounded sides are sides that cannot reach an edge. Surrounding is sometimes, but not always, fatal.
        if (getBoard().getRules().isSurroundingFatal() && getBoard().isSideEncircled(getDefenders())) {
            return ATTACKER_WIN;
        }

        return GOOD_MOVE; // i.e. no win
    }

    private Coord checkLinnaeanCapture(Coord endSpace) {
        // A move might be a Linnaean capture if:
        // 1. Ending space is adjacent to a space adjacent to the throne.
        // 2. The king is on the throne.
        // 3. The target space is occupied by a defender.
        // 4. The remaining three king-adjacent spaces are occupied by
        //    attackers.

        int center = getBoard().getBoardDimension() / 2;

        // If it isn't the Linnaean space in the +x or +y directions...
        if(!(endSpace.x == center && endSpace.y == center + 2 || endSpace.y == center && endSpace.x == center + 2)) {
            // And it isn't the Linnaean space in the -x or -y directions...
            if(!(endSpace.x == center && endSpace.y == center - 2 || endSpace.y == center && endSpace.x == center - 2)) {
                // Then it can't be a Linnaean capture.
                return null;
            }
        }

        Coord throne = Coord.get(center, center);

        // Can't be a Linnaean capture if the king isn't on the throne
        if(Taflman.getPackedType(getPieceAt(center,center)) != Taflman.TYPE_KING) {
            return null;
        }

        List<Coord> interveningSpaces = Coord.getInterveningSpaces(getBoard().getBoardDimension(), throne, endSpace);

        if(interveningSpaces.size() != 1) {
            throw new IllegalStateException("Impossible Linnaean space passed checks");
        }

        Coord targetSpace = Coord.getInterveningSpaces(getBoard().getBoardDimension(), throne, endSpace).get(0);

        // Can't be a Linnaean capture if the target space isn't occupied by a defender
        char target = getBoard().getOccupier(targetSpace);
        if(Taflman.getPackedSide(target) != Taflman.SIDE_DEFENDERS) {
            return null;
        }

        // Can't be a Linnaean capture if the target space is occupied by an uncapturable guard
        if(Taflman.isGuard(target)) return null;

        List<Character> neighbors = getBoard().getAdjacentNeighbors(throne);
        int neighborCount = 0;
        for(char neighbor : neighbors) {
            if(Taflman.getPackedSide(neighbor) == Taflman.SIDE_ATTACKERS) neighborCount++;
        }

        // Can't be a Linnaean capture if the king is not otherwise surrounded by attackers
        if(neighborCount < 3) {
            return null;
        }

        return targetSpace;
    }

    private boolean checkEdgeFortEscape() {
        // We have an edge fort escape if four conditions hold:
        // 1. The king can reach an edge, or is on an edge
        // 2. The king has at least one available move
        // 3. No black piece can reach the king, excluding jumps.
        // 4. The white pieces surrounding the king cannot be captured.
        //    We can check this by looking at each one in turn, checking its
        //    horizontal and vertical neighbors, and seeing if any of them
        //    have two potentially hostile spaces on the same rank or file.
        //    (A potentially hostile space is a space which is empty, but
        //    not part of the fort. The spaces the king can reach are the
        //    fort spaces.

        // Get the king.
        char king = Taflman.EMPTY;
        for (char taflman : getDefenders().getTaflmen()) {
            if (Taflman.isKing(taflman)) {
                king = taflman;
                break;
            }
        }

        boolean kingOnEdge = getBoard().isEdgeSpace(Taflman.getCurrentSpace(this, king));
        List<Coord> fortSpaces = Taflman.getReachableSpaces(this, king, false);

        if (kingOnEdge) {
            // If the king is on an edge and his allowable destinations are
            // empty, then he's surrounded closely, and is by definition
            // not in an edge fort.
            if (Taflman.getAllowableDestinations(this, king).size() < 1) return false;
        } else {
            // If the king is not on an edge, then he must be able to
            // reach an edge. (He must also be allowed to move, but
            // if he can reach an edge from not an edge, he can clearly
            // move.)

            boolean edgeReachable = false;
            for (Coord space : fortSpaces) {
                if (getBoard().isEdgeSpace(space)) {
                    edgeReachable = true;
                    break;
                }
            }

            if (!edgeReachable) return false;
        }

        // If the king can't reach any attacking pieces, then he is surrounded
        // by friendly taflmen.
        List<Character> edgefortTaflmen = new ArrayList<>();
        for (Coord space : fortSpaces) {
            for (char t : getBoard().getAdjacentNeighbors(space)) {
                if (Taflman.getSide(this, t).isAttackingSide()) return false;
                else edgefortTaflmen.add(t);
            }
        }

        // We've established that the king is fully surrounded by friendly taflmen.
        // Now we have to check to see if they can be captured. Do that this way:
        // 1. Get the adjacent spaces for each taflman in the edge fort.
        // 2. Remove any spaces which make up the fort, and any occupied by friendly
        //    taflmen.
        // 3. If the number of remaining spaces is not 3, then it can't be captured.
        //    (Try it yourself on a piece of paper if you don't believe me.)

        for(char taflman : edgefortTaflmen) {
            List<Coord> adjacent = new ArrayList<>(getBoard().getAdjacentSpaces(Taflman.getCurrentSpace(this, taflman)));
            adjacent.removeAll(fortSpaces);

            int friendlySpaces = 0;
            for(Coord c : adjacent) {
                char occupier = getBoard().getOccupier(c);
                if(occupier != Taflman.EMPTY && !Taflman.getSide(this, occupier).isAttackingSide()) {
                    friendlySpaces++;
                }
            }

            if(adjacent.size() - friendlySpaces >= 3) {
                return false;
            }
        }

        // If we've checked every black piece and none of them can reach the king, then
        // this is a successful edge fort.
        return true;
    }

    public GameState deepCopy() {
        return new GameState(this);
    }

    public String getOTNPositionString() {
        return PositionSerializer.getPositionRecord(getBoard());
    }

    public String getPasteableRulesString() {
        String otnrString = RulesSerializer.getRulesStringWithoutStart(mGame.getRules());
        otnrString += "starti:" + PositionSerializer.invertRecord(PositionSerializer.getPositionRecord(getBoard()));

        if(getCurrentSide().isAttackingSide() != mGame.getRules().getStartingSide().isAttackingSide()) {
            if(getCurrentSide().isAttackingSide()) {
                otnrString = otnrString.replaceFirst("atkf:n", "atkf:y");
            }
            else {
                otnrString = otnrString.replaceFirst("atkf:y", "atkf:n");
            }
        }

        return otnrString;
    }

    public long updateZobristHash(long oldZobrist, Board oldBoard, MoveRecord move, boolean changeTurn) {
        long hash = oldZobrist;
        int startIndex = oldBoard.getIndex(move.start);
        int endIndex = oldBoard.getIndex(move.end);
        int oldType = getZobristTypeIndex(oldBoard.getOccupier(move.start));

        hash = hash ^ mGame.mZobristConstants[Game.ZOBRIST_BOARD][startIndex][oldType];
        hash = hash ^ mGame.mZobristConstants[Game.ZOBRIST_BOARD][endIndex][oldType];

        for(Coord capturedCoord : move.captures) {
            int captureIndex = oldBoard.getIndex(capturedCoord);
            oldType = getZobristTypeIndex(oldBoard.getOccupier(capturedCoord));
            hash = hash ^ mGame.mZobristConstants[Game.ZOBRIST_BOARD][captureIndex][oldType];
        }

        // Take out one side, add other side.
        if(changeTurn) {
            hash = hash ^ mGame.mZobristConstants[Game.ZOBRIST_STATE][Game.ZOBRIST_TURN][Game.ZOBRIST_TURN_DEFENDERS];
            hash = hash ^ mGame.mZobristConstants[Game.ZOBRIST_STATE][Game.ZOBRIST_TURN][Game.ZOBRIST_TURN_ATTACKERS];
        }

        if(hash == 0) hash = 1;
        return hash;
    }

    public long zobristHash() {
        int boardSquares = getBoard().getBoardDimension() * getBoard().getBoardDimension();

        long hash = 0;
        for (char taflman : getBoard().getCachedTaflmanLocations().getTaflmen()) {
            int typeIndex = getZobristTypeIndex(taflman);
            int coordIndex = Coord.getIndex(getBoard().getBoardDimension(), getBoard().findTaflmanSpace(taflman));
            hash = hash ^ mGame.mZobristConstants[Game.ZOBRIST_BOARD][coordIndex][typeIndex];
        }

        if(getCurrentSide().isAttackingSide()) {
            hash = hash ^ mGame.mZobristConstants[Game.ZOBRIST_STATE][Game.ZOBRIST_TURN][Game.ZOBRIST_TURN_ATTACKERS];
        }
        else {
            hash = hash ^ mGame.mZobristConstants[Game.ZOBRIST_STATE][Game.ZOBRIST_TURN][Game.ZOBRIST_TURN_DEFENDERS];
        }

        if(hash == 0) hash = 1;
        return hash;
    }

    private int getZobristTypeIndex(char taflman) {
        int typeIndex = 0;
        int type = Taflman.getPackedType(taflman);
        switch (type) {
            case Taflman.TYPE_KING:
                typeIndex = 0;
                break;
            case Taflman.TYPE_KNIGHT:
                typeIndex = 1;
                break;
            case Taflman.TYPE_COMMANDER:
                typeIndex = 2;
                break;
            default:
                typeIndex = 3;
                break;
        }
        if (Taflman.getPackedSide(taflman) > 0) {
            typeIndex += Taflman.TYPES_BY_PIECE;
        }

        return typeIndex;
    }

    // TODO: makeMove(Coord, Coord)
    public int makeMove(MoveRecord nextMove) {
        if(getPieceAt(nextMove.start.x, nextMove.start.y) == Taflman.EMPTY) return ILLEGAL_MOVE;

        GameState nextState = moveTaflman(getPieceAt(nextMove.start.x, nextMove.start.y), nextMove.end);
        if(nextState.getLastMoveResult() == GOOD_MOVE) {
            char berserker = nextState.getBerserkingTaflman();
            // Advance turn if berserker is empty
            nextState = mGame.advanceState(this, nextState, (berserker == Taflman.EMPTY), berserker, true);
            nextState.mLastMoveResult = nextState.checkVictory();
        }

        return nextState.getLastMoveResult();
    }

    public int makeMove(DetailedMoveRecord nextMove) {
        int result = makeMove((MoveRecord) nextMove);
        if(result >= LOWEST_NONERROR_RESULT) {
            mDetailedExitingMove.setTimeRemaining(nextMove.getTimeRemaining());
        }

        return result;
    }

    public void setExitingMove(DetailedMoveRecord exitingMove) {
        mExitingMove = exitingMove;
    }

    public void setEnteringMove(DetailedMoveRecord enteringMove) {
        mEnteringMove = enteringMove;
    }
}
