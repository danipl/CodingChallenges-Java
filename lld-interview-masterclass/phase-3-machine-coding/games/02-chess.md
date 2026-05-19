# Chess

> Complex board game with piece movement rules, turn management, and game state validation.

## Requirements

- 8x8 board with standard piece setup
- Piece-specific movement rules (pawn, rook, knight, bishop, queen, king)
- Turn-based play (white → black → white)
- Check and checkmate detection
- Special moves: castling, en passant, pawn promotion
- Move history and undo

## Domain Model

```
ChessGame
  ├── Board (8x8)
  │     └── Square[][] (each holds optional Piece)
  ├── Player[] (White, Black)
  ├── Piece[] (32 pieces)
  │     ├── type: PieceType
  │     ├── color: Color
  │     └── position: Position
  ├── MoveHistory
  └── GameState
```

## Key Patterns

### State Pattern
```
NOT_STARTED → WHITE_TURN → BLACK_TURN → CHECK → CHECKMATE / STALEMATE / DRAW
```

### Strategy Pattern (Move Validation)
```java
interface MovementStrategy {
    List<Position> getValidMoves(Piece piece, Board board);
    boolean isValidMove(Piece piece, Position from, Position to, Board board);
}

class RookMovement implements MovementStrategy { /* straight lines */ }
class KnightMovement implements MovementStrategy { /* L-shapes */ }
class BishopMovement implements MovementStrategy { /* diagonals */ }
class QueenMovement implements MovementStrategy { /* rook + bishop */ }
class KingMovement implements MovementStrategy { /* one square any direction */ }
class PawnMovement implements MovementStrategy { /* forward, capture diagonal, en passant */ }
```

### Observer (Game Events)
```java
interface GameObserver {
    void onMove(Move move);
    void onCheck(Player player);
    void onCheckmate(Player winner);
    void onDraw();
}
```

## Core Implementation

```java
enum PieceType { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }
enum Color { WHITE, BLACK }

abstract class Piece {
    final PieceType type;
    final Color color;
    Position position;
    final MovementStrategy movement;

    Piece(PieceType type, Color color, MovementStrategy movement) {
        this.type = type; this.color = color; this.movement = movement;
    }

    List<Position> getValidMoves(Board board) {
        return movement.getValidMoves(this, board);
    }
}

class Board {
    private final Square[][] squares = new Square[8][8];

    boolean isValidMove(Position from, Position to) {
        Piece piece = getPieceAt(from);
        if (piece == null) return false;
        if (piece.getColor() != currentTurn) return false;
        if (!piece.getMovement().isValidMove(piece, from, to, this)) return false;
        // Check if move puts own king in check
        Board temp = this.clone();
        temp.makeMove(from, to);
        return !temp.isKingInCheck(piece.getColor());
    }

    boolean isKingInCheck(Color color) {
        Position kingPos = findKing(color);
        Color opponent = color == Color.WHITE ? Color.BLACK : Color.WHITE;
        for (Piece piece : getPiecesOf(opponent)) {
            if (piece.getValidMoves(this).contains(kingPos)) return true;
        }
        return false;
    }

    boolean isCheckmate(Color color) {
        if (!isKingInCheck(color)) return false;
        // No legal moves for any piece of this color
        for (Piece piece : getPiecesOf(color)) {
            if (!piece.getValidMoves(this).isEmpty()) return false;
        }
        return true;
    }
}
```

## Interview Tips

1. **Don't implement full chess** — focus on core: movement, validation, check/checkmate
2. **MovementStrategy** is the key pattern — each piece has its own strategy
3. **Check detection**: After every move, check if opponent's king is under attack
4. **Special moves**: Mention them but implement only if time permits
5. **Move validation**: Clone board, make move, check if own king is in check
