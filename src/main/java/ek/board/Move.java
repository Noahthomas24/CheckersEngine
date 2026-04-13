package ek.board;

public class Move {
    
    public int fromIndex;      // Position where the piece started
    public int toIndex;        // Position of where the piece landed
    
    // Capture Data
    public int captureIndex;   // Position of where a piece was captured (-1 if no capture)
    public int capturedPiece;  // The integer of the piece captured (Board.WHITE = 1, EMPTY=0, etc)
    
    // Promotion Data
    public boolean isPromotion; // True if this move turned a regular piece into a King

    public Move(int fromIndex, int toIndex, int captureIndex, int capturedPiece, boolean isPromotion) {
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
        this.captureIndex = captureIndex;
        this.capturedPiece = capturedPiece;
        this.isPromotion = isPromotion;
    }

    // Prints out the entire move with details
    @Override
    public String toString() {
        String details = "Move from " + fromIndex + " to " + toIndex;
        if (captureIndex != -1) {
            details += " (Captured piece " + capturedPiece + " at " + captureIndex + ")";
        }
        if (isPromotion) {
            details += " [PROMOTION]";
        }
        return details;
    }
}