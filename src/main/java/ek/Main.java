package ek;

import ek.GUI.CheckersGUI;
import ek.board.Board;
import ek.engine.Engine;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        Board gameBoard = new Board();
        gameBoard.initializeStartingPosition();

        Engine engine = new Engine(15);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Checkers Engine (0x88)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            // CheckersGUI shows the color selection dialog and constructs Game internally
            CheckersGUI gui = new CheckersGUI(gameBoard, engine);
            frame.add(gui);

            frame.pack(); // Sizes the window to exactly fit the JPanel
            frame.setLocationRelativeTo(null); // Centers on screen
            frame.setVisible(true);
        });
    }
}
