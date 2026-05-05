package ek.board;

import java.util.ArrayList;
import java.util.List;

public class Board {

    private Move move;
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


    private boolean isKing(int piece) {
        return piece == WHITEKing || piece == BLACKKing;
    }


    public List<Move> findSlidingMove(int fromIndex) {
        List<Move> moves = new ArrayList<>();
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

            // Instant boundary check
            if (!isOffBoard(target)) {
                if (board[target] == EMPTY) {
                    // Create the Move object instead of printing
                    moves.add(new Move(fromIndex, target, -1, EMPTY, false));
                }
            }
        }
        return moves;
    }

    public List<Move> findJumpMoves(int fromIndex) {
        List<Move> jumps = new ArrayList<>();
        int piece = board[fromIndex];
        int[] directions;

        if (piece == WHITEKing || piece == BLACKKing) {
            directions = new int[] {15, 17, -15, -17};
        } else if (piece == WHITE) { // Assuming White moves positive
            directions = new int[] {15, 17};
        } else {
            directions = new int[] {-15, -17};
        }

        // Determine if the current piece is Black or White
        boolean isCurrentPieceBlack = (piece % 2 != 0);

        for (int offset : directions) {
            int captureSquare = fromIndex + offset;
            int landingSquare = fromIndex + (offset * 2);

            // validate if landing position is on the board
            if (!isOffBoard(landingSquare)) {

                // validate if target position is empty
                if (board[landingSquare] == EMPTY) {

                    // validate if intermediate square is enemy
                    int capturedPiece = board[captureSquare];

                    if (capturedPiece != EMPTY) {
                        boolean isCapturedPieceBlack = (capturedPiece % 2 != 0);

                        if (isCurrentPieceBlack != isCapturedPieceBlack) {
                            // Create the Jump Move object instead of printing
                            jumps.add(new Move(fromIndex, landingSquare, captureSquare, capturedPiece, false));
                        }
                    }
                }
            }
        }
        return jumps;
    }

    public List<Move> generateLegalMoves(int activePlayerColor) {
        List<Move> slidingMoves = new ArrayList<>();
        List<Move> jumpMoves = new ArrayList<>();

        // Iterate through all 128 indices
        for (int fromIndex = 0; fromIndex < 128; fromIndex++) {

            // Skip invalid squares in 0x88 format
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

            // Gather the moves into their respective lists
            jumpMoves.addAll(findJumpMoves(fromIndex));
            slidingMoves.addAll(findSlidingMove(fromIndex));
        }

        // Mandatory Capture Rule: If any jumps exist, ignore regular moves.
        if (!jumpMoves.isEmpty()) {
            return jumpMoves;
        }

        return slidingMoves;
    }

    public boolean makeMove(Move move) {

        if (move == null) return false;

        // Check to make sure destination is empty
        if (board[move.toIndex] != EMPTY) {
            return false;
        }

        int movingPiece = board[move.fromIndex];
        // Check if  move is a jumping move (Distance is 30 or 34 in 0x88)
        int distance = Math.abs(move.fromIndex - move.toIndex);


        /*if (distance == 30 || distance == 34) {
            // Calculate the square we jumped over
            move.captureIndex = move.fromIndex + ((move.toIndex - move.fromIndex) / 2);

            // Save the captured piece so we can restore it later, then remove it
            move.capturedPiece = board[move.captureIndex];
            board[move.captureIndex] = EMPTY;
        }*/

        // Validation for making valid move
        boolean isStep = (distance == 15 || distance == 17);
        boolean isJump = (distance == 30 || distance == 34);
        if (!isStep && !isJump) {
            return false;
        }

        //Direction Check (Only for non-kings)
        if (movingPiece == WHITE && move.toIndex < move.fromIndex && !isKing(movingPiece)) return false;
        if (movingPiece == BLACK && move.toIndex > move.fromIndex && !isKing(movingPiece)) return false;


        if (isJump) {
            move.captureIndex = move.fromIndex + ((move.toIndex - move.fromIndex) / 2);
            move.capturedPiece = board[move.captureIndex];

            // Ensure there is actually an ENEMY piece to jump over
            if (move.capturedPiece == EMPTY || (move.capturedPiece % 2 == movingPiece % 2)) {
                return false;
            }

            // Clear the captured piece
            board[move.captureIndex] = EMPTY;
        }


        // Move the piece
        board[move.toIndex] = movingPiece;
        board[move.fromIndex] = EMPTY;

        // Handle King Promotions
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
        return true;
    }

    public void unmakeMove(Move move) {
        int pieceOnTarget = board[move.toIndex];

        // Undo King Promotion if it happened on this turn
        if (move.isPromotion) {
            // Demote it back to a standard piece
            pieceOnTarget = (pieceOnTarget == WHITEKing) ? WHITE : BLACK;
        }

        // Move the piece back to its starting square
        board[move.fromIndex] = pieceOnTarget;
        board[move.toIndex] = EMPTY;

        // Restore the captured piece, if there was one
        if (move.capturedPiece != EMPTY) {
            board[move.captureIndex] = move.capturedPiece;
        }
    }


    public void initializeStartingPosition() {
        // Clear the board to make sure no pieces are on the board
        for (int i = 0; i < 128; i++) {
            board[i] = EMPTY;
        }

        // White pieces on rows 0, 1, 2
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 8; col++) {
                if ((row + col) % 2 != 0) { // Only dark squares
                    board[(row << 4) + col] = WHITE;
                }
            }
        }

        // Black pieces on rows 5, 6, 7
        for (int row = 5; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if ((row + col) % 2 != 0) {
                    board[(row << 4) + col] = BLACK;
                }
            }
        }
    }




}