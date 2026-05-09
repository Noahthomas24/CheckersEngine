package ek.board;

import java.util.ArrayList;
import java.util.List;

public class Board {

    private Move move;
    private Move lastMove;
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
                    moves.add(new Move(fromIndex, target));
                }
            }
        }
        return moves;
    }

    // Called once per piece from generateLegalMoves.
    // visitedCaptures is empty on the initial call, the recursive helper fills it.
    private void expandJumpChains(int fromIndex, int piece, List<Integer> visitedCaptures, List<Move> results) {
        expandJumpChainRecursive(fromIndex, fromIndex, piece, visitedCaptures, new ArrayList<>(), results);
    }

    private void expandJumpChainRecursive(
            int originalFrom, int currentSquare, int piece,
            List<Integer> captureIndices, List<Integer> capturedPieces,
            List<Move> results) {

        int[] directions;
        if (piece == WHITEKing || piece == BLACKKing) {
            directions = new int[] {15, 17, -15, -17};
        } else if (piece == WHITE) {
            directions = new int[] {15, 17};
        } else { // BLACK
            directions = new int[] {-15, -17};
        }

        boolean isCurrentPieceBlack = (piece % 2 != 0);
        boolean foundJump = false;

        for (int offset : directions) {
            int captureSquare = currentSquare + offset;
            int landingSquare = currentSquare + (offset * 2);

            if (isOffBoard(landingSquare)) continue;
            if (board[landingSquare] != EMPTY) continue;          // occupied (includes originalFrom)
            if (captureIndices.contains(captureSquare)) continue; // already captured in this chain

            int capturedPiece = board[captureSquare];
            if (capturedPiece == EMPTY) continue;
            boolean isCapturedPieceBlack = (capturedPiece % 2 != 0);
            if (isCurrentPieceBlack == isCapturedPieceBlack) continue; // same colour, not an enemy

            foundJump = true;

            List<Integer> newCaptureIndices = new ArrayList<>(captureIndices);
            newCaptureIndices.add(captureSquare);
            List<Integer> newCapturedPieces = new ArrayList<>(capturedPieces);
            newCapturedPieces.add(capturedPiece);

            // Promotion ends the chain, a promoted piece does not continue jumping this turn
            int targetRow = landingSquare >> 4;
            boolean promotesHere = (piece == WHITE && targetRow == 7) || (piece == BLACK && targetRow == 0);

            if (promotesHere) {
                results.add(new Move(originalFrom, landingSquare, newCaptureIndices, newCapturedPieces, true));
            } else {
                expandJumpChainRecursive(originalFrom, landingSquare, piece,
                        newCaptureIndices, newCapturedPieces, results);
            }
        }

        // No further jumps from here and we have accumulated at least one capture: record the chain
        if (!foundJump && !captureIndices.isEmpty()) {
            results.add(new Move(originalFrom, currentSquare, captureIndices, capturedPieces, false));
        }
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
            expandJumpChains(fromIndex, piece, new ArrayList<>(), jumpMoves);
            slidingMoves.addAll(findSlidingMove(fromIndex));
        }

        // Mandatory Capture Rule: If any jumps exist, ignore regular moves.
        if (!jumpMoves.isEmpty()) {
            // Sort: most captures first; promotions rank above equal-length chains
            jumpMoves.sort((a, b) -> {
                int cmp = Integer.compare(b.captureCount(), a.captureCount());
                if (cmp != 0) return cmp;
                if (a.isPromotion == b.isPromotion) return 0;
                return a.isPromotion ? 1 : -1;
            });
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
        int distance = Math.abs(move.fromIndex - move.toIndex);

        // Engine-generated multi-jump chains already carry their capture lists.
        // GUI-constructed moves have empty lists, classify them by distance instead.
        boolean isStep        = (distance == 15 || distance == 17) && move.captureCount() == 0;
        boolean isSingleJump  = (distance == 30 || distance == 34) && move.captureCount() == 0;
        boolean isEngineJump  = move.captureCount() > 0;

        if (!isStep && !isSingleJump && !isEngineJump) {
            return false;
        }

        //Direction Check (Only for non-kings on steps and single GUI jumps)
        if (isStep || isSingleJump) {
            if (movingPiece == WHITE && move.toIndex < move.fromIndex && !isKing(movingPiece)) return false;
            if (movingPiece == BLACK && move.toIndex > move.fromIndex && !isKing(movingPiece)) return false;
        }

        if (isSingleJump) {
            // Compute and record the capture on the fly for GUI-constructed moves
            int captureIdx = move.fromIndex + ((move.toIndex - move.fromIndex) / 2);
            int capturedPce = board[captureIdx];

            // Ensure there is actually an ENEMY piece to jump over
            if (capturedPce == EMPTY || (capturedPce % 2 == movingPiece % 2)) {
                return false;
            }

            move.captureIndices.add(captureIdx);
            move.capturedPieces.add(capturedPce);
            board[captureIdx] = EMPTY;
        } else if (isEngineJump) {
            // Clear every square in the pre-computed chain
            for (int captureIdx : move.captureIndices) {
                board[captureIdx] = EMPTY;
            }
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
        this.lastMove = move;
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

        // Restore all captured pieces (each is on a distinct square, so order doesn't matter)
        for (int i = 0; i < move.captureCount(); i++) {
            board[move.captureIndices.get(i)] = move.capturedPieces.get(i);
        }
    }

    // Had to make a copy for the AI so that the board would not accidentally be modified when searching
    public Board copy() {
        Board copy = new Board();
        System.arraycopy(this.board, 0, copy.board, 0, 128);
        return copy;
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

    //returns last move, used in CheckersGUI class to color moved squares
    public Move getLastMove() {
        return lastMove;
    }




}