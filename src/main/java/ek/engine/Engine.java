package ek.engine;

import java.util.List;
import ek.board.*;

public class Engine {

    private int maxDepth;
    
    // Counters for your report data
    public long nodesEvaluated = 0;
    public long alphaBetaCutoffs = 0;

    public Engine(int depth) {
        this.maxDepth = depth;
    }

    public Move findBestMove(Board board, int aiPlayer) {
        // Reset counters at the start of each search
        nodesEvaluated = 0;
        alphaBetaCutoffs = 0;

        int bestScore = Integer.MIN_VALUE;
        Move bestMove = null;

        // Fetch all legal moves for the root position
        List<Move> moves = board.generateLegalMoves(aiPlayer);

        for (Move move : moves) {
            board.makeMove(move);
            // Call alpha-beta for the opponent's turn (isMaximizing = false)
            int score = alphaBeta(board, maxDepth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, aiPlayer);
            board.unmakeMove(move);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private int alphaBeta(Board board, int depth, int alpha, int beta, boolean isMaximizing, int aiPlayer) {
        nodesEvaluated++; // Increment for the report

        // Figure out whose turn it is in this recursive layer
        int opponent = (aiPlayer == Board.BLACK || aiPlayer == Board.BlackKing) ? Board.WHITE : Board.BLACK;
        int currentPlayer = isMaximizing ? aiPlayer : opponent;
        
        List<Move> currentMoves = board.generateLegalMoves(currentPlayer);

        // Terminal State: In Checkers, if a player has zero legal moves, they lose.
        if (currentMoves.isEmpty()) {
            if (isMaximizing) {
                return -100000 - depth; // AI loses. We subtract depth to prefer delaying the loss.
            } else {
                return 100000 + depth;  // AI wins. We add depth to prefer winning faster.
            }
        }

        // Depth limit reached: Evaluate the board statically
        if (depth == 0) {
            return evaluate(board, aiPlayer);
        }

        // MAX LAYER (AI's turn)
        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : currentMoves) {
                board.makeMove(move);
                int eval = alphaBeta(board, depth - 1, alpha, beta, false, aiPlayer);
                board.unmakeMove(move);

                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);

                if (beta <= alpha) {
                    alphaBetaCutoffs++; // Track the cut-off!
                    break;
                }
            }
            return maxEval;
        } 
        // MIN LAYER (Opponent's turn)
        else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : currentMoves) {
                board.makeMove(move);
                int eval = alphaBeta(board, depth - 1, alpha, beta, true, aiPlayer);
                board.unmakeMove(move);

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);

                if (beta <= alpha) {
                    alphaBetaCutoffs++; // Track the cut-off!
                    break; 
                }
            }
            return minEval;
        }
    }

    // The Static Evaluation Function (Required for your report)
    private int evaluate(Board board, int aiPlayer) {
        int aiScore = 0;
        int opponentScore = 0;

        boolean isAiBlack = (aiPlayer == Board.BLACK || aiPlayer == Board.BlackKing);

        // Loop through the entire 128-square 0x88 board
        for (int i = 0; i < 128; i++) {
            
            // 0x88 Magic: Instantly skip the phantom zone squares
            if ((i & 0x88) != 0) {
                continue; 
            }

            int piece = board.getPiece(i); 

            if (piece == Board.EMPTY) continue;

            // 1. Material Evaluation
            int pieceValue = 0;
            if (piece == Board.BLACK || piece == Board.WHITE) {
                pieceValue = 100;
            } else if (piece == Board.BlackKing || piece == Board.WHITEKing) {
                pieceValue = 300; // A king is historically worth about 3 men
            }

            // 2. Positional Heuristic (Bonus points)
            // Pieces on the left/right edges cannot be jumped. Give them a small bonus.
            int col = i % 16; 
            if (col == 0 || col == 7) {
                pieceValue += 10;
            }

            // 3. Assign points to the correct player
            boolean isBlackPiece = (piece == Board.BLACK || piece == Board.BlackKing);
            
            if (isBlackPiece == isAiBlack) {
                aiScore += pieceValue;
            } else {
                opponentScore += pieceValue;
            }
        }

        // Positive score means AI is winning, negative means Opponent is winning
        return aiScore - opponentScore;
    }
}