package ek.game;

import ek.board.Board;
import ek.board.Move;
import ek.engine.Engine;

import javax.swing.SwingWorker;

public class Game {

    private final Board board;
    private final Engine engine;
    private final int humanColor;
    private final int aiColor;

    private int currentPlayer;  // whose turn it is right now
    private boolean gameOver;
    private boolean aiThinking;

    private long timeLimitMs = 10000;
    private Runnable onAiMoveComplete;

    // Black always moves first in checkers, regardless of which side the human is on
    public Game(Board board, Engine engine, int humanColor) {
        this.board = board;
        this.engine = engine;
        this.humanColor = humanColor;
        this.aiColor = (humanColor == Board.BLACK) ? Board.WHITE : Board.BLACK;
        this.currentPlayer = Board.BLACK;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isAiThinking() {
        return aiThinking;
    }

    public void setTimeLimitMs(long ms) {
        this.timeLimitMs = ms;
    }

    public void setOnAiMoveComplete(Runnable callback) {
        this.onAiMoveComplete = callback;
    }

    // Returns true if the move was accepted, false if it was the wrong turn,
    // the game is already over, the AI is thinking, or the move was illegal.
    public boolean tryHumanMove(int fromIndex, int toIndex) {
        if (currentPlayer != humanColor || gameOver || aiThinking) return false;

        Move move = null;
        for (Move legal : board.generateLegalMoves(humanColor)) {
            if (legal.fromIndex == fromIndex && legal.toIndex == toIndex) {
                move = legal;
                break;
            }
        }
        if (move == null) return false;
        board.makeMove(move);

        // Switch to AI's turn and check whether AI still has legal moves
        currentPlayer = aiColor;
        checkGameOver();

        if (!gameOver) {
            triggerAiMove();
        }
        return true;
    }

    // Runs the engine search on a background thread
    private void triggerAiMove() {
        aiThinking = true;

        SwingWorker<Move, Void> worker = new SwingWorker<Move, Void>() {
            @Override
            protected Move doInBackground() {
                Board searchBoard = board.copy();
                return engine.findBestMoveInTime(searchBoard, aiColor, timeLimitMs);
            }

            @Override
            protected void done() {
                try {
                    Move aiMove = get();
                    if (aiMove != null) {
                        board.makeMove(aiMove);
                    }
                } catch (Exception e) {
                    // Search was interrupted or failed; turn passes to human silently
                }

                aiThinking = false;

                // Switch back to human's turn and check whether human still has legal moves
                currentPlayer = humanColor;
                checkGameOver();

                if (onAiMoveComplete != null) {
                    onAiMoveComplete.run();
                }
            }
        };

        worker.execute();
    }


    public void start() {
        if (currentPlayer == aiColor) {
            triggerAiMove();
        }
    }

    private void checkGameOver() {
        if (board.generateLegalMoves(currentPlayer).isEmpty()) {
            gameOver = true;
        }
    }
}
