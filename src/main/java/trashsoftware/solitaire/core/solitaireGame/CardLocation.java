package trashsoftware.solitaire.core.solitaireGame;

import trashsoftware.solitaire.fxml.controls.GameView;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public abstract class CardLocation {
    protected final SolitaireGame game;
    protected final Card card;  // nullable

    CardLocation(SolitaireGame game, Card card) {
        this.game = game;
        this.card = card;
    }

    public Card getCard() {
        return card;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CardLocation that = (CardLocation) o;

        return Objects.equals(card, that.card);
    }

    @Override
    public int hashCode() {
        return card != null ? card.hashCode() : 0;
    }

    public abstract SolitaireMove createMove(CardLocation dstLocation);

    public abstract boolean isSelected(Card card);

    public abstract double[] cardLeftXY(GameView gameView);

    /**
     * Reloads card from this location, returns the new loaded one.
     *
     * @return the new loaded one
     */
    public abstract CardLocation reloadLocation();

    public abstract static class SingleCardLocation extends CardLocation {
        protected final int pos;

        SingleCardLocation(SolitaireGame game, Card card, int pos) {
            super(game, card);

            this.pos = pos;
        }

        public int getPos() {
            return pos;
        }

        @Override
        public boolean isSelected(Card card) {
            return card == this.card;
        }
    }

    public static class MainLocation extends CardLocation {
        private final int col, row;
        private final Set<Card> highlightedCards = new TreeSet<>();

        public MainLocation(SolitaireGame game, Card card, int col, int row) {
            super(game, card);

            this.col = col;
            this.row = row;

            if (card != null) {
                highlightedCards.add(card);
                SolitaireDeck deck = game.getMainArea()[col];
                if (deck.draggable(row)) {
                    for (int i = row + 1; i < deck.size(); ++i) {
                        highlightedCards.add(deck.get(i));
                    }
                }
            }
        }

        public int getCol() {
            return col;
        }

        public int getRow() {
            return row;
        }

        @Override
        public double[] cardLeftXY(GameView gameView) {
            return gameView.xyOfMain(col, row);
        }

        @Override
        public CardLocation reloadLocation() {
            Card card = null;
            if (row < game.getMainArea()[col].size()) card = game.getMainArea()[col].get(row);
            return new MainLocation(game, card, col, row);
        }

        @Override
        public SolitaireMove createMove(CardLocation dstLocation) {
            if (dstLocation instanceof MainLocation) {
                return new SolitaireMove.MainToMain(game, this, (MainLocation) dstLocation);
            } else if (dstLocation instanceof SpaceLocation) {
                return new SolitaireMove.MainToSpace(game, this, (SpaceLocation) dstLocation);
            } else if (dstLocation instanceof FinishedLocation) {
                return new SolitaireMove.MainToFinished(game, this, (FinishedLocation) dstLocation);
            } else {
                throw new SolitaireException();
            }
        }

        @Override
        public boolean isSelected(Card card) {
            return highlightedCards.contains(card);
        }

        @Override
        public String toString() {
            return "MainLocation{" +
                    "col=" + col +
                    ", row=" + row +
                    '}';
        }
    }

    public static class SpaceLocation extends SingleCardLocation {
        public SpaceLocation(SolitaireGame game, Card card, int pos) {
            super(game, card, pos);
        }

        @Override
        public SolitaireMove createMove(CardLocation dstLocation) {
            if (dstLocation instanceof MainLocation) {
                return new SolitaireMove.SpaceToMain(game, this, (MainLocation) dstLocation);
            } else if (dstLocation instanceof SpaceLocation) {
                return new SolitaireMove.SpaceToSpace(game, this, (SpaceLocation) dstLocation);
            } else if (dstLocation instanceof FinishedLocation) {
                return new SolitaireMove.SpaceToFinished(game, this, (FinishedLocation) dstLocation);
            } else {
                throw new SolitaireException();
            }
        }

        @Override
        public double[] cardLeftXY(GameView gameView) {
            return gameView.xyOfSpace(pos);
        }

        @Override
        public CardLocation reloadLocation() {
            return new SpaceLocation(game, game.getSpaceArea()[pos], pos);
        }

        @Override
        public String toString() {
            return "SpaceLocation{" +
                    "pos=" + pos +
                    '}';
        }
    }

    public static class FinishedLocation extends SingleCardLocation {
        public FinishedLocation(SolitaireGame game, Card card, int pos) {
            super(game, card, pos);
        }

        @Override
        public SolitaireMove createMove(CardLocation dstLocation) {
            if (dstLocation instanceof MainLocation) {
                return new SolitaireMove.FinishedToMain(game, this, (MainLocation) dstLocation);
            } else if (dstLocation instanceof SpaceLocation) {
                return new SolitaireMove.FinishedToSpace(game, this, (SpaceLocation) dstLocation);
            } else if (dstLocation instanceof FinishedLocation) {
                return new SolitaireMove.FinishedToFinished(game);
            } else {
                throw new SolitaireException();
            }
        }

        @Override
        public double[] cardLeftXY(GameView gameView) {
            return gameView.xyOfFinished(pos);
        }

        @Override
        public CardLocation reloadLocation() {
            return new FinishedLocation(game, game.getFinishedArea()[pos].getSurfaceCard(), pos);
        }

        @Override
        public String toString() {
            return "FinishedLocation{" +
                    "pos=" + pos +
                    '}';
        }
    }
}


