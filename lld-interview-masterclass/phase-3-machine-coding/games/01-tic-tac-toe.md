# Tic-Tac-Toe

> Two-player game with state management, win detection, and strategy-based AI.

## Requirements

- 3x3 board, two players (X and O)
- Alternating turns
- Win detection (row, column, diagonal)
- Draw detection
- Play against another player or AI
- Game history / replay

## Domain Model

```
Game
  ├── Board (3x3 grid)
  ├── Player[] (X, O)
  ├── currentPlayer: Player
  ├── gameState: GameState
  └── GameHistory (move log)
```

## Key Patterns

### State Pattern
```
WAITING → PLAYING → PLAYER_X_WIN / PLAYER_O_WIN / DRAW
```

### Strategy Pattern (AI)
```java
interface MoveStrategy {
    Position getNextMove(Board board, Player player);
}

class RandomMoveStrategy implements MoveStrategy {
    public Position getNextMove(Board board, Player player) {
        List<Position> available = board.getAvailablePositions();
        return available.get(new Random().nextInt(available.size()));
    }
}

class MinimaxStrategy implements MoveStrategy {
    public Position getNextMove(Board board, Player player) {
        // Minimax algorithm — always optimal
        return findBestMove(board, player);
    }
}
```

## Core Implementation

```java
enum CellState { EMPTY, X, O }
enum GameState { WAITING, PLAYING, X_WON, O_WON, DRAW }

class Board {
    private final CellState[][] grid = new CellState[3][3];
    private int movesMade = 0;

    Board() {
        for (int i = 0; i < 3; i++)
            Arrays.fill(grid[i], CellState.EMPTY);
    }

    boolean makeMove(int row, int col, CellState player) {
        if (grid[row][col] != CellState.EMPTY) return false;
        grid[row][col] = player;
        movesMade++;
        return true;
    }

    GameState checkGameState() {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (grid[i][0] != CellState.EMPTY &&
                grid[i][0] == grid[i][1] && grid[i][1] == grid[i][2]) {
                return grid[i][0] == CellState.X ? GameState.X_WON : GameState.O_WON;
            }
        }
        // Check columns
        for (int j = 0; j < 3; j++) {
            if (grid[0][j] != CellState.EMPTY &&
                grid[0][j] == grid[1][j] && grid[1][j] == grid[2][j]) {
                return grid[0][j] == CellState.X ? GameState.X_WON : GameState.O_WON;
            }
        }
        // Check diagonals
        if (grid[1][1] != CellState.EMPTY &&
            grid[0][0] == grid[1][1] && grid[1][1] == grid[2][2]) {
            return grid[1][1] == CellState.X ? GameState.X_WON : GameState.O_WON;
        }
        if (grid[1][1] != CellState.EMPTY &&
            grid[0][2] == grid[1][1] && grid[1][1] == grid[2][0]) {
            return grid[1][1] == CellState.X ? GameState.X_WON : GameState.O_WON;
        }
        // Draw
        if (movesMade == 9) return GameState.DRAW;
        return GameState.PLAYING;
    }

    List<Position> getAvailablePositions() {
        List<Position> positions = new ArrayList<>();
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (grid[i][j] == CellState.EMPTY)
                    positions.add(new Position(i, j));
        return positions;
    }
}

class Game {
    private final Board board = new Board();
    private final Player[] players;
    private int currentTurn = 0;
    private GameState state = GameState.WAITING;
    private final MoveStrategy aiStrategy;  // Optional AI

    Game(Player x, Player o, MoveStrategy aiStrategy) {
        this.players = new Player[]{x, o};
        this.aiStrategy = aiStrategy;
    }

    void play() {
        state = GameState.PLAYING;
        while (state == GameState.PLAYING) {
            Player current = players[currentTurn % 2];
            Position move;
            if (aiStrategy != null && current.isAI()) {
                move = aiStrategy.getNextMove(board, current.getSymbol());
            } else {
                move = current.makeMove(board);  // Human input
            }
            board.makeMove(move.row(), move.col(), current.getSymbol());
            state = board.checkGameState();
            currentTurn++;
        }
    }
}
```

## Interview Tips

1. **Win detection**: Can be optimized — check only affected row/col/diagonal after each move
2. **AI strategy**: Strategy pattern allows swapping Random → Minimax → Alpha-Beta
3. **Extensibility**: Make board size configurable (NxN, K-in-a-row to win)
4. **Undo**: Maintain move history stack for undo functionality
