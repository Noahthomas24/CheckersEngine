package ek.GUI;

import ek.board.Board;
import ek.board.Move;
import ek.engine.Engine;
import ek.game.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CheckersGUI extends JPanel {

    private static final int TILE_SIZE = 80;
    private static final int BOARD_DIMENSION = 8;

    private final Board board;
    private final Game game;
    private final int humanColor;

    private int selectedIndex = -1;
    private JTextField timeLimitField;

    public CheckersGUI(Board board, Engine engine) {
        this.board = board;

        // Asks the player which color they want before the window opens
        Object[] options = {"Black", "White"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Choose your color",
                "Checkers — Color Selection",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        this.humanColor = (choice == 1) ? Board.WHITE : Board.BLACK;

        this.game = new Game(board, engine, humanColor);

        setLayout(new BorderLayout());

        BoardPanel boardPanel = new BoardPanel();
        boardPanel.setPreferredSize(new Dimension(TILE_SIZE * BOARD_DIMENSION, TILE_SIZE * BOARD_DIMENSION));
        add(boardPanel, BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Thinking time (sec)"));
        timeLimitField = new JTextField("10", 4);
        controls.add(timeLimitField);
        add(controls, BorderLayout.SOUTH);

        // Repaint the board after every AI move so the result and the overlay both update
        game.setOnAiMoveComplete(() -> boardPanel.repaint());

        // If the human chose White, the AI (Black) moves first immediately
        game.start();
    }

    // Converts a board row (0x88 row index) to the screen row it should be drawn on.
    // When the human plays White, the board is flipped so White's pieces appear at the bottom.
    // Because 7 - (7 - x) == x, this function is its own inverse and is used for
    // mouse-click conversion (screen row → board row) as well.
    private int toScreenRow(int boardRow) {
        return (humanColor == Board.WHITE) ? 7 - boardRow : boardRow;
    }

    private long parseTimeLimitMs() {
        try {
            long seconds = Long.parseLong(timeLimitField.getText().trim());
            if (seconds > 0) return seconds * 1000;
        } catch (NumberFormatException ignored) {}
        return 10000; // fall back to 10 s on invalid input
    }


    // Inner panel - all drawing and mouse handling lives here
    private class BoardPanel extends JPanel {

        BoardPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleMouseClick(e.getX(), e.getY());
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawBoard(g2d);
            drawPieces(g2d);
            drawSelection(g2d);

            if (game.isAiThinking()) {
                drawThinkingOverlay(g2d);
            }
        }

        private void drawBoard(Graphics2D g2d) {
            Move lastMove = board.getLastMove();
            for (int boardRow = 0; boardRow < BOARD_DIMENSION; boardRow++) {
                int screenRow = toScreenRow(boardRow);
                for (int col = 0; col < BOARD_DIMENSION; col++) {
                    // Square color is determined by the board row so the pattern stays
                    // correct regardless of visual orientation
                    boolean isDarkSquare = (boardRow + col) % 2 != 0;
                    g2d.setColor(isDarkSquare ? new Color(139, 69, 19) : new Color(245, 222, 179));
                    g2d.fillRect(col * TILE_SIZE, screenRow * TILE_SIZE, TILE_SIZE, TILE_SIZE);


                    //Colors the from and to squares yellow, to better highlight which squares have just moved.
                    int currentIndex = (boardRow << 4) + col;
                    if (lastMove != null) {
                        if (currentIndex == lastMove.fromIndex || currentIndex == lastMove.toIndex) {

                            g2d.setColor(new Color(255, 255, 0, 100));
                            g2d.fillRect(col * TILE_SIZE, screenRow * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        }
                    }
                }
            }
        }

        private void drawPieces(Graphics2D g2d) {
            for (int boardRow = 0; boardRow < BOARD_DIMENSION; boardRow++) {
                int screenRow = toScreenRow(boardRow);
                for (int col = 0; col < BOARD_DIMENSION; col++) {
                    int index = (boardRow << 4) + col;
                    if (board.isOffBoard(index)) continue;

                    int piece = board.getPiece(index);
                    if (piece != Board.EMPTY) {
                        drawSinglePiece(g2d, piece, col * TILE_SIZE, screenRow * TILE_SIZE);
                    }
                }
            }
        }

        private void drawSinglePiece(Graphics2D g2d, int piece, int x, int y) {
            int padding = 10;
            int size = TILE_SIZE - (padding * 2);

            if (piece == Board.WHITE || piece == Board.WHITEKing) {
                g2d.setColor(Color.WHITE);
            } else {
                g2d.setColor(Color.BLACK);
            }
            g2d.fillOval(x + padding, y + padding, size, size);

            // If it's a King, draw a crown or a marker on top
            if (piece == Board.WHITEKing || piece == Board.BLACKKing) {
                g2d.setColor(Color.RED); // Just a simple red marker for Kings for now
                g2d.fillOval(x + padding + 15, y + padding + 15, size - 30, size - 30);
            }
        }

        private void drawSelection(Graphics2D g2d) {
            if (selectedIndex == -1) return;

            int boardRow = selectedIndex >> 4;
            int col = selectedIndex & 0x0F;
            int screenRow = toScreenRow(boardRow);

            // Yellow ring — visible on both the dark brown and light wheat squares
            g2d.setColor(new Color(255, 220, 0, 200));
            g2d.setStroke(new BasicStroke(3));
            int padding = 6;
            int size = TILE_SIZE - padding * 2;
            g2d.drawOval(col * TILE_SIZE + padding, screenRow * TILE_SIZE + padding, size, size);
        }

        private void drawThinkingOverlay(Graphics2D g2d) {
            int boardPixels = TILE_SIZE * BOARD_DIMENSION;

            // Semi-transparent dark veil over the board
            g2d.setColor(new Color(0, 0, 0, 120));
            g2d.fillRect(0, 0, boardPixels, boardPixels);

            // Centered "Thinking..." label
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 32));
            FontMetrics fm = g2d.getFontMetrics();
            String text = "Thinking...";
            int textX = (boardPixels - fm.stringWidth(text)) / 2;
            int textY = (boardPixels + fm.getAscent() - fm.getDescent()) / 2;
            g2d.drawString(text, textX, textY);
        }

        private void handleMouseClick(int mouseX, int mouseY) {
            if (game.isAiThinking() || game.isGameOver()) return;

            int col = mouseX / TILE_SIZE;
            int screenRow = mouseY / TILE_SIZE;

            if (col < 0 || col >= 8 || screenRow < 0 || screenRow >= 8) return;

            // toScreenRow is its own inverse, so applying it to the screen row gives the board row
            int boardRow = toScreenRow(screenRow);
            int clickedIndex = (boardRow << 4) + col;

            if (selectedIndex != -1) {
                // Apply the current time limit before triggering the AI
                game.setTimeLimitMs(parseTimeLimitMs());

                boolean moveAccepted = game.tryHumanMove(selectedIndex, clickedIndex);

                if (moveAccepted) {
                    selectedIndex = -1;
                } else {
                    // If the click landed on one of the human's own pieces, switch selection
                    int piece = board.getPiece(clickedIndex);
                    if (piece != Board.EMPTY && isHumanPiece(piece)) {
                        selectedIndex = clickedIndex;
                    } else {
                        selectedIndex = -1;
                    }
                }
            } else {
                // No piece selected yet — select only the human's own pieces
                int piece = board.getPiece(clickedIndex);
                if (piece != Board.EMPTY && isHumanPiece(piece)) {
                    selectedIndex = clickedIndex;
                }
            }

            repaint();
        }

        private boolean isHumanPiece(int piece) {
            boolean isPieceBlack = (piece % 2 != 0);
            boolean isHumanBlack = (humanColor % 2 != 0);
            return isPieceBlack == isHumanBlack;
        }
    }
}
