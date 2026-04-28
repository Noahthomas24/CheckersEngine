package ek.board;

import java.util.ArrayList;
import java.util.List;

public class Move {
    
    public int fromIndex;      // Position where the piece started
    public int toIndex;        // Position of where the piece landed
    
    // Supports capturing multiple pieces
    public List<Integer> capturedIndexes;   // Position of where a piece was captured (-1 if no capture)
    public List<Integer> capturedPieces;  // The integer of the piece captured (Board.WHITE = 1, EMPTY=0, etc)
    
    // Promotion 
    public boolean isPromotion; // True if this move turned a regular piece into a King

    public Move(int fromIndex, int toIndex) {
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    // Method for adding captures to list of captures
    public void addCapture(int index, int piece) {
        capturedIndexes.add(index);
        capturedPieces.add(piece);
    }

    // Prints out entire move
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Move ").append(fromIndex).append(" to ").append(toIndex);
        if (!capturedIndexes.isEmpty()) {
            sb.append(" (Captured ").append(capturedIndexes.size()).append(" pieces)");
        }
        if (isPromotion) sb.append(" [PROMOTION]");
        return sb.toString();
    }
}