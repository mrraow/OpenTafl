package com.manywords.softworks.tafl.ui.lanterna.window.serverlobby;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Panel;

/**
 * Created by jay on 5/23/16.
 */
public class GameDetailWindow extends BasicWindow {
    public GameDetailWindow() {
        super("Game Details");

        Panel p = new Panel();

        setComponent(p);
    }
}
