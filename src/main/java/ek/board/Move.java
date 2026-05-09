package ek.board;

import java.util.ArrayList;
import java.util.List;

public class Move {

    public int fromIndex;      // Position where the piece started
    public int toIndex;        // Position of where the piece landed

    // Capture Data 
    public List<Integer> captureIndices;  // Squares jumped over, in chain order
    public List<Integer> capturedPieces; // Piece values at each of those squares, in chain order

    // Promotion Data
    public boolean isPromotion; 

    public Move(int fromIndex, int toIndex, List<Integer> captureIndices, List<Integer> capturedPieces, boolean isPromotion) {
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
        this.captureIndices = captureIndices;
        this.capturedPieces = capturedPieces;
        this.isPromotion = isPromotion;
    }

    public Move(int fromIndex, int toIndex) {
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
        this.captureIndices = new ArrayList<>();
        this.capturedPieces = new ArrayList<>();
    }

    public int captureCount() {
        return captureIndices.size();
    }

    @Override
    public String toString() {
        String details = "Move from " + fromIndex + " to " + toIndex;
        if (!captureIndices.isEmpty()) {
            details += " (Captured " + captureCount() + " piece(s) at " + captureIndices + ")";
        }
        if (isPromotion) {
            details += " [PROMOTION]";
        }
        return details;
    }
}
