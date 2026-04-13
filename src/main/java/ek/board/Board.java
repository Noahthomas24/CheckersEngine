package ek.board;

public class Board {



    //0x88 Board with 128 slots
    private final int[] board = new int[128];


    //Pieces are represented as Integers
    // All Black pieces are Odd and White pieces are Even
    // This can help us determine which piece is what color faster later.

    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int BLACKKing = 3;
    public static final int WHITE = 2;
    public static final int WHITEKing = 4;

    public static final int OFF_BOARD = -1;


    public boolean isOffBoard(int fromIndex) {
        return (fromIndex & 0x88) != 0;
    }


    public int getPiece(int square) {
        if (isOffBoard(square)) {
            return OFF_BOARD;
        }
        return board[square];
    }


    public void findSlidingMove(int fromIndex) {
        int piece = board[fromIndex];
        int[] directions;


        //Define move direction based off color. White moves positive, black moves negative.
        if (piece == WHITEKing || piece == BLACKKing) {
            directions = new int[] {15, 17, -15, -17}; // All 4 diagonals
        } else if (piece == WHITE) {
            directions = new int[] {15, 17};           // Only forwards
        } else { // BLACK
            directions = new int[] {-15, -17};         // Only backwards
        }

        for (int offset : directions) {
            int target = fromIndex + offset;

            // The 0x88 Magic: Instant boundary check
            if (!isOffBoard(target)) {
                if (board[target] == EMPTY) {
                    System.out.println("Valid move from " + fromIndex + " to " + target);
                }
            }
        }



    }


    public void findJumpMoves(int fromIndex) {
        int piece = board[fromIndex];
        int[] directions;

        if (piece == WHITEKing || piece == BLACKKing) {
            directions = new int[] {15, 17, -15, -17};
        } else if (piece == WHITE || piece == WHITEKing) { // Assuming White moves positive
            directions = new int[] {15, 17};
        } else {
            directions = new int[] {-15, -17};
        }

        // Determine if the current piece is Black or White (using your Odd/Even logic)
        boolean isCurrentPieceBlack = (piece % 2 != 0);

        for (int offset : directions) {
            int captureSquare = fromIndex + offset;
            int landingSquare = fromIndex + (offset * 2);

            // 1. Is the landing square on the board?
            if (!isOffBoard(landingSquare)) {

                // 2. Is the landing square empty?
                if (board[landingSquare] == EMPTY) {

                    // 3. Is the intermediate square an enemy piece?
                    int capturedPiece = board[captureSquare];

                    if (capturedPiece != EMPTY) {
                        boolean isCapturedPieceBlack = (capturedPiece % 2 != 0);

                        if (isCurrentPieceBlack != isCapturedPieceBlack) {
                            System.out.println("Valid JUMP from " + fromIndex + " to " + landingSquare + " capturing " + captureSquare);

                        }
                    }
                }
            }
        }
    }



    public void generateAllMoves(int activePlayerColor) {


        // Iterate through all 128 indices

        for (int fromIndex = 0; fromIndex < 128; fromIndex++) {

            // 1. 0x88 Check: Skip invalid squares
            if ((fromIndex & 0x88) != 0) {
                continue;
            }

            int piece = board[fromIndex];
            if (piece == EMPTY) continue;
            // Odd % 2 is 1 (Black). Even % 2 is 0 (White).
            boolean isPieceBlack = (piece % 2 != 0);
            boolean isActivePlayerBlack = (activePlayerColor % 2 != 0);

            if (isPieceBlack != isActivePlayerBlack) {
                continue; // Not the active player's piece
            }

            findSlidingMove(fromIndex);
        }
    }


    public void makeMove(Move move) {
        int movingPiece = board[move.fromIndex];

        // 1. Check if this move is a Jump (Distance is 30 or 34 in 0x88)
        int distance = Math.abs(move.fromIndex - move.toIndex);
        if (distance == 30 || distance == 34) {
            // Calculate the square we jumped over
            move.captureIndex = move.fromIndex + ((move.toIndex - move.fromIndex) / 2);

            // Save the captured piece so we can restore it later, then remove it
            move.capturedPiece = board[move.captureIndex];
            board[move.captureIndex] = EMPTY;
        }

        // 2. Move the piece
        board[move.toIndex] = movingPiece;
        board[move.fromIndex] = EMPTY;

        // 3. Handle King Promotions
        // White moves positive (towards row 7). Black moves negative (towards row 0).
        // >> 4 divides the index by 16 to get the row number.
        int targetRow = move.toIndex >> 4;

        if (movingPiece == WHITE && targetRow == 7) {
            board[move.toIndex] = WHITEKing;
            move.isPromotion = true;
        } else if (movingPiece == BLACK && targetRow == 0) {
            board[move.toIndex] = BLACKKing;
            move.isPromotion = true;
        }
    }

    public void unmakeMove(Move move) {
        int pieceOnTarget = board[move.toIndex];

        // 1. Undo King Promotion if it happened on this turn
        if (move.isPromotion) {
            // Demote it back to a standard piece
            pieceOnTarget = (pieceOnTarget == WHITEKing) ? WHITE : BLACK;
        }

        // 2. Move the piece back to its starting square
        board[move.fromIndex] = pieceOnTarget;
        board[move.toIndex] = EMPTY;

        // 3. Restore the captured piece, if there was one
        if (move.capturedPiece != EMPTY) {
            board[move.captureIndex] = move.capturedPiece;
        }
    }


}
