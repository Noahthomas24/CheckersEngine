package ek.board;

import java.util.ArrayList;
import java.util.List;

public class Board {

    //0x88 Board with 128 slots
    private final int[] board = new int[128];

    // Pieces are represented as integers
    // All Black pieces are odd and White pieces are even
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

    public List<Move> generateLegalMoves(int activePlayerColor) {
        List<Move> slidingMoves = new ArrayList<>();
        List<Move> jumpMoves = new ArrayList<>();

        // Determine if the current piece is Black or White
        boolean isActivePlayerBlack = (activePlayerColor % 2 != 0);

        // Iterate through all 128 indices
        for (int i = 0; i < 128; i++) {
            // Skip invalid squares in 0x88 format
            if (isOffBoard(i)) {
                continue;
            }

            int piece = board[i];
            if (piece == EMPTY) continue;

            // Odd % 2 is 1 (Black). Even % 2 is 0 (White).
            boolean isPieceBlack = (piece % 2 != 0);

            if (isPieceBlack != isActivePlayerBlack) {
                continue; // Not the active player's piece
            }

            // Find all possible multi-jump sequences for this piece
            findJumpSequences(i, i, new Move(i, i), jumpMoves);
        }

        // Only look for sliding moves if no jumps have been found
        if (jumpMoves.isEmpty()) {
            for (int i = 0; i < 128; i++) {
                // Skip invalid squares in 0x88 format
                if (isOffBoard(i)) {
                    continue;
                }

                int piece = board[i];
                if (piece == EMPTY) continue;

                boolean isPieceBlack = (piece % 2 != 0);
                if (isPieceBlack != isActivePlayerBlack) {
                    continue;
                }

                slidingMoves.addAll(findSlidingMoves(i));
            }
        }

        // If any jumps exist, ignore regular moves.
        return !jumpMoves.isEmpty() ? jumpMoves : slidingMoves;
    }

    private void findJumpSequences(int startIndex, int currentIndex, Move currentMove, List<Move> allSequences) {
        int piece = board[startIndex];
        int[] directions = getDirections(piece);
        boolean foundFurtherJump = false;

        for (int dir : directions) {
            int captureSq = currentIndex + dir;
            int landingSq = currentIndex + (dir * 2);

            if (!isOffBoard(landingSq) && board[landingSq] == EMPTY) {
                int victim = board[captureSq];
                if (victim != EMPTY && (victim % 2 != piece % 2)) {
                    
                    // --- Simulation Step ---
                    foundFurtherJump = true;
                    board[landingSq] = piece;
                    board[currentIndex] = EMPTY;
                    board[captureSq] = EMPTY;

                    // Create a copy of the move sequence so far
                    Move nextMove = new Move(startIndex, landingSq);
                    nextMove.capturedIndexes.addAll(currentMove.capturedIndexes);
                    nextMove.capturedPieces.addAll(currentMove.capturedPieces);
                    nextMove.addCapture(captureSq, victim);

                    // Check for King promotion, jumping stops if you promote
                    boolean promoted = (piece == WHITE && (landingSq >> 4) == 7) || 
                                       (piece == BLACK && (landingSq >> 4) == 0);

                    if (promoted) {
                        nextMove.isPromotion = true;
                        allSequences.add(nextMove);
                    } else {
                        // Recurse to find more jumps
                        findJumpSequences(startIndex, landingSq, nextMove, allSequences);
                    }

                    // --- Backtrack Step ---
                    board[captureSq] = victim;
                    board[currentIndex] = piece;
                    board[landingSq] = EMPTY;
                }
            }
        }

        // If no more jumps were possible from this square, but we have made at least one capture
        if (!foundFurtherJump && !currentMove.capturedIndexes.isEmpty()) {
            allSequences.add(currentMove);
        }
    }

    private int[] getDirections(int piece) {
        if (piece == WHITEKing || piece == BLACKKing) return new int[]{15, 17, -15, -17};
        return (piece == WHITE) ? new int[]{15, 17} : new int[]{-15, -17};
    }

    public void makeMove(Move move) {
        int piece = board[move.fromIndex];
        board[move.fromIndex] = EMPTY;
        
        // Remove all captured pieces
        for (int capIdx : move.capturedIndexes) {
            board[capIdx] = EMPTY;
        }

        // Handle Promotion
        if (move.isPromotion) {
            piece = (piece == WHITE) ? WHITEKing : BLACKKing;
        }
        
        board[move.toIndex] = piece;
    }

    public void unmakeMove(Move move) {
        int piece = board[move.toIndex];
        
        // Demote if it was a promotion move
        if (move.isPromotion) {
            piece = (piece == WHITEKing) ? WHITE : BLACK;
        }

        board[move.fromIndex] = piece;
        board[move.toIndex] = EMPTY;

        // Restore all captured pieces
        for (int i = 0; i < move.capturedIndexes.size(); i++) {
            board[move.capturedIndexes.get(i)] = move.capturedPieces.get(i);
        }
    }

    
    private List<Move> findSlidingMoves(int from) {
        List<Move> moves = new ArrayList<>();
        int piece = board[from];
        for (int dir : getDirections(piece)) {
            int target = from + dir;
            if (!isOffBoard(target) && board[target] == EMPTY) {
                Move m = new Move(from, target);
                // Check for promotion on slide
                if ((piece == WHITE && (target >> 4) == 7) || (piece == BLACK && (target >> 4) == 0)) {
                    m.isPromotion = true;
                }
                moves.add(m);
            }
        }
        return moves;
    }
}