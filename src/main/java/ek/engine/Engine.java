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

    public Engine(){}

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

        for (int depth = 1; depth <= 100; depth++) {
            System.out.println("Dybde: "+depth);
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
        System.out.println("Noder evalueret: "+ nodesEvaluated + " - cutoffs: " + alphaBetaCutoffs);

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


    // Function for evaluating board
    private int evaluate(Board board, int aiPlayer) {
        int aiScore = 0;
        int opponentScore = 0;

        boolean isAiBlack = (aiPlayer == Board.BLACK || aiPlayer == Board.BLACKKing);

        // Tracking for situational bonuses. Max pieces per side is 12; 24 is a safe cap.
        int[] aiPositions = new int[24];
        boolean[] aiIsKing = new boolean[24];
        int aiPieceCount = 0;

        int[] opponentPositions = new int[24];
        boolean[] opponentIsKing = new boolean[24];
        int opponentPieceCount = 0;

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

            // Permanent center bonus — 2x2 zone at rows 3-4, columns 3-4
            if (row >= 3 && row <= 4 && col >= 2 && col <= 5) {
            pieceValue += 8;
}

            // Assign points to the correct player
            boolean isBlackPiece = (piece == Board.BLACK || piece == Board.BLACKKing);
            boolean isKingPiece = (piece == Board.BLACKKing || piece == Board.WHITEKing);

            if (isBlackPiece == isAiBlack) {
                aiScore += pieceValue;
                if (aiPieceCount < 24) {
                    aiPositions[aiPieceCount] = i;
                    aiIsKing[aiPieceCount] = isKingPiece;
                    aiPieceCount++;
                }
            } else {
                opponentScore += pieceValue;
                if (opponentPieceCount < 24) {
                    opponentPositions[opponentPieceCount] = i;
                    opponentIsKing[opponentPieceCount] = isKingPiece;
                    opponentPieceCount++;
                }
            }
        }

        // Classify game state by material differential
        int materialDiff = aiScore - opponentScore;

        // Within the "balanced" band, skip the situational work entirely
        if (materialDiff > -150 && materialDiff < 150) {
            return aiScore - opponentScore;
        }

        // Compute the shared threat-aware distance sum.
        // For each AI piece, find the minimum Chebyshev distance to any opponent piece
        // that can still threaten it (kings always threaten; regular pieces only
        // threaten in their forward direction, which depends on piece color, not role).
        int totalDistance = 0;
        for (int a = 0; a < aiPieceCount; a++) {
            int aiSq = aiPositions[a];
            int aiRow = aiSq >> 4;
            int aiCol = aiSq & 7;

            int minDist = Integer.MAX_VALUE;
            for (int o = 0; o < opponentPieceCount; o++) {
                int oppSq = opponentPositions[o];
                int oppRow = oppSq >> 4;
                int oppCol = oppSq & 7;

                // Determine whether this opponent piece threatens the AI piece.
                // Direction depends on the opponent piece's COLOR, not on AI/opponent role.
                boolean threatens;
                if (opponentIsKing[o]) {
                    threatens = true;
                } else {
                    // Opponent is a regular piece. Figure out its color.
                    // Opponent color is the opposite of AI color.
                    boolean isOpponentWhite = isAiBlack; // AI is black => opponent is white
                    if (isOpponentWhite) {
                        // White regular pieces move toward higher rows.
                        // They can reach an AI piece only if the AI piece's row >= white's row.
                        threatens = (aiRow >= oppRow);
                    } else {
                        // Black regular pieces move toward lower rows.
                        // They can reach an AI piece only if the AI piece's row <= black's row.
                        threatens = (aiRow <= oppRow);
                    }
                }

                if (!threatens) continue;

                int dr = Math.abs(aiRow - oppRow);
                int dc = Math.abs(aiCol - oppCol);
                int chebyshev = Math.max(dr, dc);
                if (chebyshev < minDist) minDist = chebyshev;
            }

            // No threatening opponent for this AI piece => contributes 0
            if (minDist != Integer.MAX_VALUE) {
                totalDistance += minDist;
            }
        }

        int totalPieces = aiPieceCount + opponentPieceCount;

        if (materialDiff >= 150) {
            // Hunting mode: encourage trades and closing distance
            int leadFactor = Math.min(materialDiff / 100, 5);

            // Trade-down: fewer pieces on the board is better when ahead
            aiScore += (24 - totalPieces) * leadFactor * 3;

            // Chase: shorter total distance is better. Skip if either side has no pieces.
            if (aiPieceCount > 0 && opponentPieceCount > 0) {
                aiScore -= totalDistance * leadFactor * 2;
            }
        } else { // materialDiff <= -150
            // Defensive mode: keep pieces, keep distance
            int lagFactor = Math.min(-materialDiff / 100, 5);

            // Avoid trades: more pieces on the board is better when behind
            aiScore += totalPieces * lagFactor * 3;

            // Distance: greater distance is better. Skip if either side has no pieces.
            if (aiPieceCount > 0 && opponentPieceCount > 0) {
                aiScore += totalDistance * lagFactor * 2;
            }
        }

        // Positive score means AI is winning, negative means human is winning
        return aiScore - opponentScore;
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
                return -100000 + depth; // AI loses. We add depth to prefer delaying the loss.
            } else {
                return 100000 - depth;  // AI wins. We subtract depth to prefer winning faster.
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
}