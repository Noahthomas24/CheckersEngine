package ek.GUI;

import ek.board.Board;
import ek.board.Move;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CheckersGUI extends JPanel {
    private final int TILE_SIZE = 80;
    private final int BOARD_DIMENSION = 8;
    private Board gameBoard;
    private int selectedIndex = -1; // No piece selected
    private Move move;

    public CheckersGUI(Board gameBoard) {
        this.gameBoard = gameBoard;
        this.setPreferredSize(new Dimension(TILE_SIZE * BOARD_DIMENSION, TILE_SIZE * BOARD_DIMENSION));

        // Listen for clicks on the board
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        });
    }

    // draw panel
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;


        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBoard(g2d);
        drawPieces(g2d);
    }

    private void drawBoard(Graphics2D g2d) {
        for (int row = 0; row < BOARD_DIMENSION; row++) {
            for (int col = 0; col < BOARD_DIMENSION; col++) {
                // Determine square color based on row and col
                boolean isDarkSquare = (row + col) % 2 != 0;
                g2d.setColor(isDarkSquare ? new Color(139, 69, 19) : new Color(245, 222, 179)); // SaddleBrown and Wheat

                g2d.fillRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    private void drawPieces(Graphics2D g2d) {
        for (int row = 0; row < BOARD_DIMENSION; row++) {
            for (int col = 0; col < BOARD_DIMENSION; col++) {

                // Translate visual row/col to your 0x88 index
                int index = (row << 4) + col;

                // Skip if this isn't a valid square on the 0x88 board
                if (gameBoard.isOffBoard(index)) continue;

                int piece = gameBoard.getPiece(index);

                if (piece != Board.EMPTY) {
                    drawSinglePiece(g2d, piece, col * TILE_SIZE, row * TILE_SIZE);
                }
            }
        }
    }

    private void drawSinglePiece(Graphics2D g2d, int piece, int x, int y) {
        int padding = 10;
        int size = TILE_SIZE - (padding * 2);

        // Determine color
        if (piece == Board.WHITE || piece == Board.WHITEKing) {
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(Color.BLACK);
        }

        // Draw the main piece
        g2d.fillOval(x + padding, y + padding, size, size);

        // If it's a King, draw a crown or a marker on top
        if (piece == Board.WHITEKing || piece == Board.BLACKKing) {
            g2d.setColor(Color.RED); // Just a simple red marker for Kings for now
            g2d.fillOval(x + padding + 15, y + padding + 15, size - 30, size - 30);
        }
    }

    private void handleMouseClick(int mouseX, int mouseY) {
        // Convert mouse pixel coordinates to grid col and row
        int col = mouseX / TILE_SIZE;
        int row = mouseY / TILE_SIZE;

        //Check if we are clicking within the board
        if (col < 0 || col >= 8 || row < 0 || row >= 8) return;

        // Convert to 0x88 index

        int clickedIndex = (row << 4) + col;


        if (selectedIndex != -1) {


            Move move = new Move(selectedIndex, clickedIndex);

            // Logic to validate and execute move in your Board class
            boolean moveSuccessful = gameBoard.makeMove(move);

            if (moveSuccessful) {
                selectedIndex = -1; // Deselect after move
            } else {
                // If click was on another of the player's own pieces, switch selection
                int piece = gameBoard.getPiece(clickedIndex);
                if (piece != Board.EMPTY) {
                    selectedIndex = clickedIndex;
                } else {
                    selectedIndex = -1; // Clicked empty invalid square, deselect
                }
            }
        }
        // 2. If no piece is selected, select the clicked piece
        else {
            int piece = gameBoard.getPiece(clickedIndex);
            if (piece != Board.EMPTY) {
                selectedIndex = clickedIndex;
            }
        }

        repaint(); // Refresh visuals to show selection/move
    }
}