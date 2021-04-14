package trashsoftware.solitaire.core.solitaireGame;

import java.util.ArrayList;
import java.util.Collection;

public class SolitaireDeck extends ArrayList<Card> {

    public SolitaireDeck() {

    }

    public SolitaireDeck(Collection<Card> content) {
        super(content);
    }

    /**
     * @return the card at the last position of the deck, or {@code null} if the deck is empty
     */
    public Card getSurfaceCard() {
        if (isEmpty()) return null;
        else return get(size() - 1);
    }

    public Card removeSurfaceCard() {
        if (isEmpty()) throw new SolitaireException("No cards to remove.");
        else return remove(size() - 1);
    }

    /**
     * This method only used for decks in main area.
     *
     * @param card the card to be appended
     * @return {@code true} if append success
     */
    public boolean appendable(Card card) {
        Card last = getSurfaceCard();
        if (last == null) return true;
        return (last.getNum() == card.getNum() + 1) && (last.isBlack() != card.isBlack());
    }

    public boolean draggable(int row) {
        if (row >= size()) return false;
        Card card = get(row);
        for (int r = row + 1; r < size(); ++r) {
            Card nextCard = get(r);
            if (card.getNum() != nextCard.getNum() + 1 || card.isBlack() == nextCard.isBlack()) return false;
            card = nextCard;
        }
        return true;
    }
}
