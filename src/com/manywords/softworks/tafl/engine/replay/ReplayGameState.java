package com.manywords.softworks.tafl.engine.replay;

import com.manywords.softworks.tafl.OpenTafl;
import com.manywords.softworks.tafl.engine.GameState;
import com.manywords.softworks.tafl.engine.MoveRecord;
import com.manywords.softworks.tafl.rules.Coord;
import com.manywords.softworks.tafl.rules.Taflman;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jay on 7/30/16.
 */
public class ReplayGameState extends GameState {
    private ReplayGame mReplayGame;
    private ReplayGameState mParent;
    private Variation mEnclosingVariation;
    private MoveAddress mMoveAddress;
    private ReplayGameState mCanonicalChild;
    private List<Variation> mVariations = new ArrayList<>();

    public ReplayGameState(ReplayGame replayGame, GameState copyState) {
        super(copyState);
        mReplayGame = replayGame;
    }

    public ReplayGameState(int error) {
        super(error);
    }

    public void setParent(ReplayGameState state) {
        mParent = state;
        mParent.mCanonicalChild = this;

        mMoveAddress = mParent.getMoveAddress().increment(mReplayGame, this);
    }

    public void setVariationParent(ReplayGameState state, Variation parentVariation) {
        mParent = state;
        mMoveAddress = parentVariation.getNextChildAddress(mReplayGame, this);
        mEnclosingVariation = parentVariation;
    }

    public MoveAddress getMoveAddress() {
        return mMoveAddress;
    }

    public ReplayGameState getParent() {
        return mParent;
    }

    public void setMoveAddress(MoveAddress address) {
        mMoveAddress = address;
    }

    @Override
    protected GameState moveTaflman(char taflman, Coord destination) {
        GameState state = super.moveTaflman(taflman, destination);
        ReplayGameState replayState = new ReplayGameState(mReplayGame, state);

        mGame.advanceState(
                this,
                replayState,
                replayState.getBerserkingTaflman() == Taflman.EMPTY,
                replayState.getBerserkingTaflman(),
                true);

        replayState.setParent(this);

        return replayState;
    }

    private ReplayGameState moveTaflmanVariation(char taflman, Coord destination) {
        GameState state = super.moveTaflman(taflman, destination);
        ReplayGameState replayState = new ReplayGameState(mReplayGame, state);

        // Don't record this move
        mGame.advanceState(
                this,
                replayState,
                replayState.getBerserkingTaflman() == Taflman.EMPTY,
                replayState.getBerserkingTaflman(),
                false);

        return replayState;
    }

    @Override
    public int makeMove(MoveRecord nextMove) {
        if(getPieceAt(nextMove.start.x, nextMove.start.y) == Taflman.EMPTY) return ILLEGAL_MOVE;

        GameState nextState = moveTaflman(getPieceAt(nextMove.start.x, nextMove.start.y), nextMove.end);
        if(nextState.getLastMoveResult() == GOOD_MOVE) {
            nextState.mLastMoveResult = nextState.checkVictory();
        }

        return nextState.getLastMoveResult();
    }

    public ReplayGameState findVariationState(MoveAddress moveAddress) {
        MoveAddress.Element e = moveAddress.getRootElement();
        int index = e.rootIndex - 1;

        System.out.println("Replay state: " + mMoveAddress);
        System.out.println("Searching for: " + moveAddress);

        return mVariations.get(index).findVariationState(new MoveAddress(moveAddress.getNonRootElements()));
    }

    /**
     * Adds a variation to the history tree. If this state has no canonical child, the variation becomes the canonical
     * child. If this state does have a canonical child, the variation becomes the root of a new variation off of this
     * state. If the variation already exists, it is not re-added. Callers of this method should change the current
     * state as desired.
     * @param move The move to enter the variation.
     * @return A game state containing either the next state or an error, or null, if this variation already exists.
     */
    public ReplayGameState makeVariation(MoveRecord move) {
        if(getPieceAt(move.start.x, move.start.y) == Taflman.EMPTY) return new ReplayGameState(ILLEGAL_MOVE);

        if(mCanonicalChild != null && mCanonicalChild.getEnteringMove().equals(move)) {
            return null;
        }

        for(Variation v : mVariations) {
            if(v.getRoot().getEnteringMove().equals(move)) {
                return null;
            }
        }

        ReplayGameState nextState = (ReplayGameState) moveTaflmanVariation(getPieceAt(move.start.x, move.start.y), move.end);

        if(nextState.getLastMoveResult() == GOOD_MOVE) {
            nextState.mLastMoveResult = nextState.checkVictory();
        }

        if(nextState.getLastMoveResult() >= GOOD_MOVE) {
            if(mCanonicalChild == null) {
                nextState.setParent(this);
                nextState.mEnclosingVariation = mEnclosingVariation;
                mEnclosingVariation.addState(nextState);
            }
            else {
                Variation v = new Variation(mMoveAddress.nextVariation(mVariations.size() + 1), nextState);
                mVariations.add(v);
                nextState.setVariationParent(this, v);
            }
        }

        return nextState;
    }

    /**
     * Deletes a variation from the history tree, including all of its children.
     * @param moveAddress
     */
    // TODO: deleting the canonical child must move one of the variations into canonical childhood
    // That's gonna be messy. Maybe I should back off of allowing deletion.
    public void deleteVariation(MoveAddress moveAddress) {
        // Remove this address from the front of the move address.
        MoveAddress variationAddress = moveAddress.changePrefix(mMoveAddress, new MoveAddress());
        List<MoveAddress.Element> variationElements = variationAddress.getElements();

        if(variationElements.size() == 1) {
            // Hooray! a variation!
            int index = variationElements.get(0).rootIndex - 1;
            mVariations.remove(index);

            // Each variation now has index i+2 (because they're one-indexed, not zero-indexed).
            // Its address is our address, plus a variation number, plus the rest of the address. For each one,
            // the prefix is our address plus i+2. Change that prefix to our address plus i+1.
            List<MoveAddress.Element> thisElements = mMoveAddress.getElements();

            for(int i = index; i < mVariations.size(); i++) {
                // Allocate inside the loop to avoid any trickiness with reuse
                MoveAddress.Element oldVariation = new MoveAddress.Element(index+2, -1);
                MoveAddress.Element newVariation = new MoveAddress.Element(index+1, -1);

                mVariations.get(i).changeAddress(new MoveAddress(thisElements, oldVariation), new MoveAddress(thisElements, newVariation));
            }
        }
        else if(variationElements.size() > 1) {
            // Get the next replay state addressed by the move address and call this on that
            MoveAddress.Element variationElement = variationElements.get(0);
            MoveAddress.Element nextStateElement = variationElements.get(1);

            ReplayGameState variationState = mVariations.get(variationElement.rootIndex).getDirectChild(nextStateElement);
            if(variationState != null) {
                variationState.deleteVariation(moveAddress);
            }
        }
        else {
            throw new IllegalArgumentException("Argument to deleteVariation does not address a variation: " + moveAddress);
        }
    }

    public void changeAddressPrefix(MoveAddress oldPrefix, MoveAddress newPrefix) {
        MoveAddress oldAddress = mMoveAddress;
        mMoveAddress = mMoveAddress.changePrefix(oldPrefix, newPrefix);

        if(mCanonicalChild != null) {
            mCanonicalChild.changeAddressPrefix(oldPrefix, newPrefix);
        }

        List<MoveAddress.Element> oldElements = oldAddress.getElements();
        List<MoveAddress.Element> thisElements = mMoveAddress.getElements();
        for(int i = 0; i < mVariations.size(); i++) {
            Variation v = mVariations.get(i);
            MoveAddress.Element variation = new MoveAddress.Element(i+1, -1);

            v.changeAddress(new MoveAddress(oldElements, variation), new MoveAddress(thisElements, variation));
        }
    }

    public void dumpVariations() {
        OpenTafl.logPrintln(OpenTafl.LogLevel.NORMAL, "Variations for " + mMoveAddress);
        for(int i = 0; i < mVariations.size(); i++) {
            OpenTafl.logPrintln(OpenTafl.LogLevel.NORMAL, i + ": " + mVariations.get(i).getRoot().getMoveAddress());
        }
    }
}
