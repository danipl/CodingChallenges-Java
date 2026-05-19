# Card Game (Poker / Blackjack)

> Game with deck management, hand evaluation, betting, and player state.

## Requirements (Blackjack Example)

- Standard 52-card deck
- Dealer and multiple players
- Card dealing, hitting, standing
- Hand value calculation (Ace = 1 or 11)
- Bust detection (> 21)
- Betting system

## Domain Model

```
CardGame
  ├── Deck
  │     └── cards: List<Card>
  ├── Player[]
  │     ├── hand: Hand
  │     ├── chips: int
  │     └── bet: int
  ├── Dealer extends Player
  └── GameSession
        ├── state: GameState
        └── round: int
```

## Key Patterns

### State Pattern
```
BETTING → DEALING → PLAYER_TURN → DEALER_TURN → SETTLEMENT → GAME_OVER
```

### Strategy Pattern (Player Behavior)
```java
interface PlayerStrategy {
    Action decideAction(Hand hand, Card dealerUpCard);
}

class BasicBlackjackStrategy implements PlayerStrategy {
    public Action decideAction(Hand hand, Card dealerUpCard) {
        // Hit on 16 or less, stand on 17+
        return hand.getValue() < 17 ? Action.HIT : Action.STAND;
    }
}

class CardCountingStrategy implements PlayerStrategy {
    private int count = 0;
    public Action decideAction(Hand hand, Card dealerUpCard) {
        // Increase bet when count is positive
        return count > 0 && hand.getValue() < 19 ? Action.HIT : Action.STAND;
    }
}
```

## Core Implementation

```java
enum Suit { HEARTS, DIAMONDS, CLUBS, SPADES }
enum Rank { TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING, ACE }

class Card {
    final Suit suit;
    final Rank rank;
    Card(Suit suit, Rank rank) { this.suit = suit; this.rank = rank; }
}

class Deck {
    private final List<Card> cards = new ArrayList<>();

    Deck() {
        for (Suit suit : Suit.values())
            for (Rank rank : Rank.values())
                cards.add(new Card(suit, rank));
        shuffle();
    }

    void shuffle() { Collections.shuffle(cards); }
    Card deal() { return cards.remove(0); }
}

class Hand {
    private final List<Card> cards = new ArrayList<>();

    void addCard(Card card) { cards.add(card); }

    int getValue() {
        int value = 0, aces = 0;
        for (Card card : cards) {
            value += cardValue(card);
            if (card.rank == Rank.ACE) aces++;
        }
        while (value > 21 && aces > 0) { value -= 10; aces--; }
        return value;
    }

    boolean isBusted() { return getValue() > 21; }
    boolean isBlackjack() { return cards.size() == 2 && getValue() == 21; }

    private int cardValue(Card card) {
        return switch (card.rank) {
            case ACE -> 11;
            case KING, QUEEN, JACK, TEN -> 10;
            default -> card.rank.ordinal() + 2;
        };
    }
}

class BlackjackGame {
    private final Deck deck = new Deck();
    private final List<Player> players;
    private final Dealer dealer;
    private GameState state = GameState.BETTING;

    void dealInitialCards() {
        for (Player p : players) {
            p.getHand().addCard(deck.deal());
            p.getHand().addCard(deck.deal());
        }
        dealer.getHand().addCard(deck.deal());  // Face up
        dealer.getHand().addCard(deck.deal());  // Face down
        state = GameState.PLAYER_TURN;
    }

    void playerHit(Player player) {
        player.getHand().addCard(deck.deal());
        if (player.getHand().isBusted()) {
            player.setBusted(true);
        }
    }

    void dealerPlay() {
        while (dealer.getHand().getValue() < 17) {
            dealer.getHand().addCard(deck.deal());
        }
        state = GameState.SETTLEMENT;
        settleBets();
    }
}
```

## Interview Tips

1. **Ace handling**: Can be 1 or 11 — recalculate dynamically
2. **Deck management**: When to reshuffle (penetration threshold)
3. **Betting**: Separate from game logic — Strategy pattern for different betting strategies
4. **Extensibility**: Add Poker by changing Hand evaluation logic (Strategy)
5. **Multiple decks**: Casino uses 6-8 decks — parameterize deck count
