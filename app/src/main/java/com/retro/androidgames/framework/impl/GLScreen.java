package com.retro.androidgames.framework.impl;

import com.retro.androidgames.framework.Game;
import com.retro.androidgames.framework.Screen;
import com.retro.androidgames.framework.impl.GLGame;
import com.retro.androidgames.framework.impl.GLGraphics;

public abstract class GLScreen extends Screen {
    protected final GLGraphics glGraphics;
    protected final GLGame glGame;

    public GLScreen(Game game) {
        super(game);
        glGame = (GLGame)game;
        glGraphics = ((GLGame)game).getGLGraphics();
    }
}
