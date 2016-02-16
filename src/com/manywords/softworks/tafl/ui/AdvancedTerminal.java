package com.manywords.softworks.tafl.ui;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.ResizeListener;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import com.manywords.softworks.tafl.engine.Game;
import com.manywords.softworks.tafl.engine.MoveRecord;
import com.manywords.softworks.tafl.rules.Side;
import com.manywords.softworks.tafl.ui.command.Command;
import com.manywords.softworks.tafl.ui.command.CommandEngine;
import com.manywords.softworks.tafl.ui.command.CommandResult;
import com.manywords.softworks.tafl.ui.command.HumanCommandParser;
import com.manywords.softworks.tafl.ui.lanterna.settings.TerminalSettings;
import com.manywords.softworks.tafl.ui.lanterna.theme.TerminalThemeConstants;
import com.manywords.softworks.tafl.ui.lanterna.theme.TerminalTheme;
import com.manywords.softworks.tafl.ui.lanterna.theme.TerminalWindowDecorationRenderer;
import com.manywords.softworks.tafl.ui.lanterna.theme.TerminalWindowPostRenderer;
import com.manywords.softworks.tafl.ui.lanterna.window.BoardWindow;
import com.manywords.softworks.tafl.ui.lanterna.window.CommandWindow;
import com.manywords.softworks.tafl.ui.lanterna.window.MainMenuWindow;
import com.manywords.softworks.tafl.ui.lanterna.window.StatusWindow;
import com.manywords.softworks.tafl.ui.player.Player;
import com.manywords.softworks.tafl.ui.player.UiWorkerThread;

import javax.swing.*;
import java.io.IOException;

/**
 * Created by jay on 2/15/16.
 */
public class AdvancedTerminal extends SwingTerminalFrame implements UiCallback {
    public interface TerminalCallback {
        public void onMenuNavigation(Window destination);
        public void onEnteringGame(Game g, BoardWindow bw, StatusWindow sw, CommandWindow cw);
        public void handleInGameCommand(String command);

        public UiCallback getUiCallback();
    }

    private MultiWindowTextGUI mGui;

    private BoardWindow mBoardWindow;
    private StatusWindow mStatusWindow;
    private CommandWindow mCommandWindow;

    private UiWorkerThread mGameThread;
    private Game mGame;
    private CommandEngine mCommandEngine;

    private boolean mInGame;

    public AdvancedTerminal() {
        super();
        setTitle("OpenTafl");
        setSize(1024, 768);
        setResizable(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
        addResizeListener(new ResizeListener() {
            @Override
            public void onResized(Terminal terminal, TerminalSize terminalSize) {
                layoutGameWindows(terminalSize);
            }
        });

        Screen s = null;
        try {
            s = new TerminalScreen(this);
            s.startScreen();
        }
        catch(IOException e) {
            System.out.println("Failed to start");
            System.exit(0);
        }
        mGui = new MultiWindowTextGUI(s, new DefaultWindowManager(new TerminalWindowDecorationRenderer()), new TerminalWindowPostRenderer(), new EmptySpace(TextColor.ANSI.BLACK));
        mGui.setTheme(new TerminalTheme());
        Window mainMenuWindow = new MainMenuWindow(mTerminalCallback);
        mainMenuWindow.setHints(TerminalThemeConstants.CENTERED);
        mGui.addWindowAndWait(mainMenuWindow);
    }

    private void addBoardWindows() {
        mGui.addWindow(mBoardWindow);
        mGui.addWindow(mStatusWindow);
        mGui.addWindow(mCommandWindow);

        layoutGameWindows(mGui.getScreen().getTerminalSize());

        mGui.waitForWindowToClose(mBoardWindow);
    }

    private void layoutGameWindows(TerminalSize size) {
        if(mBoardWindow == null || mStatusWindow == null || mCommandWindow == null) return;

        mBoardWindow.setHints(TerminalThemeConstants.BOARD_WINDOW);
        mStatusWindow.setHints(TerminalThemeConstants.STATUS_WINDOW);
        mCommandWindow.setHints(TerminalThemeConstants.COMMAND_WINDOW);

        TerminalSize screenSize = size;
        TerminalSize boardWindowSize = mBoardWindow.getPreferredSize();

        mBoardWindow.setPosition(new TerminalPosition(0, 0));
        int leftoverRight = screenSize.getColumns() - boardWindowSize.getColumns();
        int leftoverBottom = screenSize.getRows() - boardWindowSize.getRows();
        int boardWindowHeight = boardWindowSize.getRows();
        int boardWindowWidth = boardWindowSize.getColumns();
        TerminalPosition statusPosition, commandPosition;
        TerminalSize statusSize, commandSize;

        if(leftoverRight < 20) {
            statusPosition = new TerminalPosition(0, boardWindowHeight + 2);
            statusSize = new TerminalSize(boardWindowWidth, leftoverBottom - 6);

            leftoverBottom -= statusSize.getRows();
            leftoverBottom -= 4;

            commandPosition = new TerminalPosition(0, boardWindowHeight + 2 + statusSize.getRows() + 2);
            commandSize = new TerminalSize(boardWindowWidth, leftoverBottom);
        }
        else if(leftoverRight < 40){
            statusPosition = new TerminalPosition(boardWindowWidth + 2, 0);
            statusSize = new TerminalSize(leftoverRight - 4, boardWindowHeight);

            commandPosition = new TerminalPosition(0, boardWindowHeight + 2);
            commandSize = new TerminalSize(boardWindowWidth, 4);
        }
        else {
            statusPosition = new TerminalPosition(boardWindowWidth + 2, 0);
            statusSize = new TerminalSize(leftoverRight - 4, boardWindowHeight - 6);

            commandPosition = new TerminalPosition(boardWindowWidth + 2, statusSize.getRows() + 2);
            commandSize = new TerminalSize(leftoverRight - 4, 4);
        }

        mStatusWindow.setPosition(statusPosition);
        mStatusWindow.setSize(statusSize);

        mCommandWindow.setPosition(commandPosition);
        mCommandWindow.setSize(commandSize);
    }

    private void leaveGameUi() {
        mGui.removeWindow(mBoardWindow);
        mGui.removeWindow(mStatusWindow);
        mGui.removeWindow(mCommandWindow);

        //mGameThread.cancel();
        mTerminalCallback.onMenuNavigation(new MainMenuWindow(mTerminalCallback));
    }

    @Override
    public void gameStarting() {
        statusText("Game starting");
        mInGame = true;
    }

    @Override
    public void awaitingMove(Player currentPlayer, boolean isAttackingSide) {
        statusText("Awaiting" + (isAttackingSide ? " attacker " : " defender ") + "move");
    }

    @Override
    public void moveResult(CommandResult result, MoveRecord move) {
        if(result.result == CommandResult.FAIL) {
            statusText(result.message);
        }
        else {
            statusText("Last move: " + move);
            mBoardWindow.rerenderBoard();
        }
    }

    @Override
    public void statusText(String text) {
        mStatusWindow.addStatus(text);
    }

    @Override
    public void gameStateAdvanced() {
        mBoardWindow.rerenderBoard();
    }

    @Override
    public void victoryForSide(Side side) {
        mBoardWindow.rerenderBoard();
        if(side.isAttackingSide()) {
            statusText("Attackers win!");
        }
        else {
            statusText("Defenders win!");
        }
    }

    @Override
    public void gameFinished() {
        mInGame = false;
    }

    @Override
    public MoveRecord waitForHumanMoveInput() {
        return null;
    }

    @Override
    public boolean inGame() {
        return mInGame;
    }

    private TerminalCallback mTerminalCallback = new TerminalCallback() {
        @Override
        public void onMenuNavigation(Window destination) {
            if(destination == null) System.exit(0);

            mGui.removeWindow(mGui.getActiveWindow());
            destination.setHints(TerminalThemeConstants.CENTERED);
            mGui.addWindowAndWait(destination);
        }

        @Override
        public void onEnteringGame(Game g, BoardWindow bw, StatusWindow sw, CommandWindow cw) {
            mGui.removeWindow(mGui.getActiveWindow());
            mBoardWindow = bw;
            mStatusWindow = sw;
            mCommandWindow = cw;

            // Set up a game thread
            mGameThread = new UiWorkerThread(new UiWorkerThread.UiWorkerRunnable() {
                private boolean mRunning = true;
                @Override
                public void cancel() {
                    mRunning = false;
                }

                @Override
                public void run() {
                    mGame = g;
                    mCommandEngine = new CommandEngine(g, AdvancedTerminal.this, TerminalSettings.getNewPlayer(TerminalSettings.attackers), TerminalSettings.getNewPlayer(TerminalSettings.defenders));
                    mCommandEngine.startGame();
                }
            });
            mGameThread.start();

            // This is our UI thread (blocking call)
            addBoardWindows();
        }

        @Override
        public void handleInGameCommand(String command) {
            Command c = HumanCommandParser.parseCommand(mCommandEngine, command);
            CommandResult r = mCommandEngine.executeCommand(c);

            if(r.result != CommandResult.SUCCESS) {
                statusText(r.message);
            }
            else if (r.type == CommandResult.Type.MOVE) {
                if(r.extra != null) {
                    if(mCommandEngine.getCurrentPlayer().getType() == Player.Type.HUMAN) {
                        mCommandEngine.getCurrentPlayer().onMoveDecided((MoveRecord) r.extra);
                    }
                    else {
                        statusText("Not your turn!");
                    }
                }
                else {
                    throw new IllegalStateException("Received successful move command with no move record");
                }
            }
            else if (r.type == CommandResult.Type.INFO) {
                HumanCommandParser.Info infoCommand = (HumanCommandParser.Info) c;
                mBoardWindow.rerenderBoard(infoCommand.location, infoCommand.stops, infoCommand.moves, infoCommand.captures);
            }
            else if (r.type == CommandResult.Type.SHOW) {
                mBoardWindow.rerenderBoard();
            }
            else if (r.type == CommandResult.Type.HISTORY) {
                String gameRecord = (String) r.extra;
                statusText(gameRecord);
            }
            else if (r.type == CommandResult.Type.QUIT) {
                if(mInGame) {
                    // Leave the game thread running for history.
                    statusText("Finished game. Enter 'quit' again to return to menu.");
                    mCommandEngine.finishGame();
                }
                else {
                    leaveGameUi();
                }
            }
        }

        @Override
        public UiCallback getUiCallback() {
            return AdvancedTerminal.this;
        }
    };
}
