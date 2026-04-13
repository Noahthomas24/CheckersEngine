package ek.board;

public class Board {


    //0x88 Board with 128 slots
    private final int[] board = new int[128];


    //Pieces are represented as Integers
    // All Black pieces are Odd and White pieces are Even
    // This can help us determine which piece is what color faster later.

    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int BlackKing = 3;
    public static final int WHITE = 2;
    public static final int WHITEKing = 4;

    public static final int OFF_BOARD = -1;


    public boolean isOffBoard(int square) {
        return (square & 0x88) != 0;
    }

    //Make move
    //Generate Move
    //Undo move



}
