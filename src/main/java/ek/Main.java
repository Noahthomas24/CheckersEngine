package ek;

import ek.GUI.CheckersGUI;
import ek.board.Board;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Initialize
        Board gameBoard = new Board();
        gameBoard.initializeStartingPosition();

        // Set up the GUI
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Checkers Engine (0x88)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            CheckersGUI gui = new CheckersGUI(gameBoard);
            frame.add(gui);

            frame.pack(); // Sizes the window to exactly fit the JPanel
            frame.setLocationRelativeTo(null); // Centers on screen
            frame.setVisible(true);
        });
    }
}