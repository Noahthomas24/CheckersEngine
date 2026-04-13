package ek.board;

public class Move {
    
    public int fromIndex;      // The 0x88 index where the piece started
    public int toIndex;        // The 0x88 index where the piece landed
    
    // Capture Data
    public int captureIndex;   // The index of the square where a piece was captured (-1 if no capture)
    public int capturedPiece;  // The integer of the piece captured (e.g., Board.WHITE). (0 if none)
    
    // Promotion Data
    public boolean isPromotion; // True if this move turned a regular piece into a King

    public Move(int fromIndex, int toIndex, int captureIndex, int capturedPiece, boolean isPromotion) {
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
        this.captureIndex = captureIndex;
        this.capturedPiece = capturedPiece;
        this.isPromotion = isPromotion;
    }

    // This makes debugging much easier by printing out the move details clearly!
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