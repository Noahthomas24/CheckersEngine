package ek.engine;

import java.util.List;
import ek.board.*;

public class Engine {

    private int maxDepth;

    // Counters for evaluating efficiency of program
    public long nodesEvaluated = 0;
    public long alphaBetaCutoffs = 0;

    // Set once per findBestMoveInTime call, read by alphaBetaTimed
    private long startTime;
    private long timeLimitMs;

    // Thrown to abort a search that has run over its time budget
    private static final class SearchTimeoutException extends RuntimeException {
        SearchTimeoutException() {
            // Suppress stack-trace generation — this is pure control flow
            super(null, null, true, false);
        }
    }

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

    // Time-limited iterative deepening search
    public Move findBestMoveInTime(Board board, int aiPlayer, long timeLimitMs) {
        this.startTime = System.currentTimeMillis();
        this.timeLimitMs = timeLimitMs;
        nodesEvaluated = 0;
        alphaBetaCutoffs = 0;

        // Fallback so we never return null 
        List<Move> rootMoves = board.generateLegalMoves(aiPlayer);
        if (rootMoves.isEmpty()) return null;
        Move bestMove = rootMoves.get(0);

        Move killerMove = null; // best move from the last fully completed depth

        for (int depth = 1; depth <= 20; depth++) {
            try {
                Move candidate = searchRootAtDepth(board, aiPlayer, depth, killerMove);
                // Only commit when the full depth finished without a timeout
                bestMove = candidate;
                killerMove = candidate;
            } catch (SearchTimeoutException e) {
                // Partial depth — discard and keep result from the last completed depth
                break;
            }

            // Avoid starting a new iteration we are unlikely to finish
            if (System.currentTimeMillis() - startTime >= timeLimitMs) {
                break;
            }
        }

        return bestMove;
    }

    // Root driver for one depth iteration, applies move ordering before searching
    private Move searchRootAtDepth(Board board, int aiPlayer, int depth, Move killerMove) {
        int bestScore = Integer.MIN_VALUE;
        Move bestMove = null;

        List<Move> moves = board.generateLegalMoves(aiPlayer);

        // Sets the killer move at first index
        orderMoves(moves, killerMove);

        // orderMoves() sets the killer move at index 0, which can disrupt the
        // capture-count order that generateLegalMoves() already established.
        // Re-sort from index 1 onward to restore that order behind the killer.
        if (moves.size() > 1) {
            moves.subList(1, moves.size()).sort((a, b) -> Integer.compare(b.captureCount(), a.captureCount()));
        }

        for (Move move : moves) {
            board.makeMove(move);
            int score = alphaBetaTimed(board, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, aiPlayer);
            board.unmakeMove(move);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    // Bubble the killer move to index 0 so it is always searched first
    private void orderMoves(List<Move> moves, Move killer) {
        if (killer == null) return;
        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);
            if (m.fromIndex == killer.fromIndex && m.toIndex == killer.toIndex) {
                moves.remove(i);
                moves.add(0, m);
                return;
            }
        }
    }

    // Alpha-beta used by the timed search, checks the clock and throws on timeout
    private int alphaBetaTimed(Board board, int depth, int alpha, int beta, boolean isMaximizing, int aiPlayer) {
        if (System.currentTimeMillis() - startTime >= timeLimitMs) {
            throw new SearchTimeoutException();
        }

        nodesEvaluated++;

        int opponent = (aiPlayer == Board.BLACK || aiPlayer == Board.BLACKKing) ? Board.WHITE : Board.BLACK;
        int currentPlayer = isMaximizing ? aiPlayer : opponent;

        List<Move> currentMoves = board.generateLegalMoves(currentPlayer);

        if (currentMoves.isEmpty()) {
            if (isMaximizing) {
                return -100000 + depth;
            } else {
                return 100000 - depth;
            }
        }

        if (depth == 0) {
            return evaluate(board, aiPlayer);
        }

        // MAX LAYER (AI's turn)
        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : currentMoves) {
                board.makeMove(move);
                int eval = alphaBetaTimed(board, depth - 1, alpha, beta, false, aiPlayer);
                board.unmakeMove(move);

                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) { alphaBetaCutoffs++; break; }
            }
            return maxEval;
        }
        // MIN LAYER (human's turn)
        else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : currentMoves) {
                board.makeMove(move);
                int eval = alphaBetaTimed(board, depth - 1, alpha, beta, true, aiPlayer);
                board.unmakeMove(move);

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) { alphaBetaCutoffs++; break; }
            }
            return minEval;
        }
    }


    // Original fixed-depth alpha-beta
    private int alphaBeta(Board board, int depth, int alpha, int beta, boolean isMaximizing, int aiPlayer) {
        nodesEvaluated++; // Increment for the report

        // Figure out whose turn it is 
        int opponent = (aiPlayer == Board.BLACK || aiPlayer == Board.BLACKKing) ? Board.WHITE : Board.BLACK;
        int currentPlayer = isMaximizing ? aiPlayer : opponent;
        
        List<Move> currentMoves = board.generateLegalMoves(currentPlayer);

        // Terminal state, if no moves are available, the player moves
        if (currentMoves.isEmpty()) {
            if (isMaximizing) {
                return -100000 + depth; // AI loses. We subtract depth to prefer delaying the loss.
            } else {
                return 100000 - depth;  // AI wins. We add depth to prefer winning faster. //TODO CHECK +-
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
                    alphaBetaCutoffs++;  // Amount of branches cut off is tracked for evaluating function
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

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);

                if (beta <= alpha) {
                    alphaBetaCutoffs++; // Amount of branches cut off is tracked for evaluating function
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
                pieceValue = 300; // A king is historically worth about 3 men
            }

            // Advancement bonus — regular pieces only; kings already move in all directions
            int row = i >> 4;
            if (piece == Board.WHITE) {
                pieceValue += row * 15;        // White advances toward row 7
            } else if (piece == Board.BLACK) {
                pieceValue += (7 - row) * 15;  // Black advances toward row 0
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