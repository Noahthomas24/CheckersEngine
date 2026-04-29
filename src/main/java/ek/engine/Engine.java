package ek.engine;

import java.util.List;
import ek.board.*;

public class Engine {

    private long timeLimitMillis;
    private long startTime;
    private boolean timeUp;
    
    // Counters for evaluating efficiency of program
    public long nodesEvaluated = 0;
    public long alphaBetaCutoffs = 0;

    // Constructor now takes milliseconds instead of depth
    public Engine(long timeLimitMillis) {
        this.timeLimitMillis = timeLimitMillis;
    }

public Move findBestMove(Board board, int aiPlayer) {
        // Reset timers and counters at the start of each search
        startTime = System.currentTimeMillis();
        timeUp = false;
        nodesEvaluated = 0;
        alphaBetaCutoffs = 0;

        Move bestMoveOverall = null;
        int currentDepth = 1;

        // Fetch all legal moves for the root position
        List<Move> rootMoves = board.generateLegalMoves(aiPlayer);
        
        // Safety fallback: if only one move is available, play it instantly
        if (rootMoves.size() == 1) return rootMoves.get(0);

        // --- Iterative Deepening Loop ---
        while (!timeUp) {
            int bestScore = Integer.MIN_VALUE;
            Move bestMoveForThisDepth = null;

            for (Move move : rootMoves) {
                board.makeMove(move);
                // Call alpha-beta for the opponent's turn
                int score = alphaBeta(board, currentDepth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, aiPlayer);
                board.unmakeMove(move);

                // If time ran out during this search, discard the results
                if (timeUp) {
                    break;
                }

                if (score > bestScore) {
                    bestScore = score;
                    bestMoveForThisDepth = move;
                }
            }

            // Only update our overall best move if the entire depth finished without timing out
            if (!timeUp && bestMoveForThisDepth != null) {
                bestMoveOverall = bestMoveForThisDepth;
            }

            currentDepth++;
            
            // Hard cap to prevent infinite loops in trivial endgames
            if (currentDepth > 100) break; 
        }

        return bestMoveOverall;
    }

    private int alphaBeta(Board board, int depth, int alpha, int beta, boolean isMaximizing, int aiPlayer) {
        nodesEvaluated++;

        // --- Time Check Optimization ---
        // Calling System.currentTimeMillis() on EVERY node is slow.
        // We only check the clock every 1024 nodes to save CPU cycles.
        if ((nodesEvaluated & 1023) == 0) { 
            if (System.currentTimeMillis() - startTime >= timeLimitMillis) {
                timeUp = true;
            }
        }

        // If time is up, collapse the search tree instantly by returning 0. findBestMove will ignore this invalid score.
        if (timeUp) return 0;

        // Figure out whose turn it is 
        int opponent = (aiPlayer == Board.BLACK || aiPlayer == Board.BLACKKing) ? Board.WHITE : Board.BLACK;
        int currentPlayer = isMaximizing ? aiPlayer : opponent;
        
        List<Move> currentMoves = board.generateLegalMoves(currentPlayer);

        // Terminal state
        if (currentMoves.isEmpty()) {
            if (isMaximizing) {
                return -100000 + depth; 
            } else {
                return 100000 - depth;  
            }
        }

        // Depth limit reached
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

                if (timeUp) return 0; // Abort up the chain

                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);

                if (beta <= alpha) {
                    alphaBetaCutoffs++;  
                    break;
                }
            }
            return maxEval;
        } 
        // MIN LAYER (human's turn)
        else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : currentMoves) {
                board.makeMove(move);
                int eval = alphaBeta(board, depth - 1, alpha, beta, true, aiPlayer);
                board.unmakeMove(move);

                if (timeUp) return 0; // Abort up the chain

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);

                if (beta <= alpha) {
                    alphaBetaCutoffs++; 
                    break; 
                }
            }
            return minEval;
        }
    }

    // Function for evaluating board
    private int evaluate(Board board, int aiPlayer) {
        int aiScore = 0;
        int opponentScore = 0;

        boolean isAiBlack = (aiPlayer == Board.BLACK || aiPlayer == Board.BLACKKing);

        // Loop through the entire 128-square 0x88 board
        for (int i = 0; i < 128; i++) {
            
            // Instantly skip the ineligible zone
            if ((i & 0x88) != 0) {
                continue; 
            }

            int piece = board.getPiece(i); 

            if (piece == Board.EMPTY) continue;

            // Material Evaluation
            int pieceValue = 0;
            if (piece == Board.BLACK || piece == Board.WHITE) {
                pieceValue = 100;
            } else if (piece == Board.BLACKKing|| piece == Board.WHITEKing) {
                pieceValue = 300; // A king is worth about 3 men
            }

        
            // Pieces on the left/right edges cannot be jumped. Give them a small bonus.
            int col = i % 16; 
            if (col == 0 || col == 7) {
                pieceValue += 10;
            }

            // Assign points to the correct player
            boolean isBlackPiece = (piece == Board.BLACK || piece == Board.BLACKKing);
            
            if (isBlackPiece == isAiBlack) {
                aiScore += pieceValue;
            } else {
                opponentScore += pieceValue;
            }
        }

        // Positive score means AI is winning, negative means human is winning
        return aiScore - opponentScore;
    }
}