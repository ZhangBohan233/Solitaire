package trashsoftware.solitaire.core.solitaireGame;

public class Card implements Comparable<Card> {
    public static final int CLUB = 0;
    public static final int DIAMOND = 1;
    public static final int HEART = 2;
    public static final int SPADE = 3;

    private final int num;
    private final int suit;

    public Card(int num, int suit) {
        this.num = num;
        this.suit = suit;
    }

    @Override
    public String toString() {
        return "[" + getShowString() + ']';
    }

    @Override
    public int compareTo(Card o) {
        return Integer.compare(this.cardIndex(), o.cardIndex());
    }

    public String getShowString() {
        return getNumRep() + " " + getSuitRep();
    }

    public boolean isBlack() {
        return suit == CLUB || suit == SPADE;
    }

    public int getNum() {
        return num;
    }

    public int getSuit() {
        return suit;
    }

    public String getNumRep() {
        if (num == 1) return "A";
        if (num == 11) return "J";
        if (num == 12) return "Q";
        if (num == 13) return "K";
        return String.valueOf(num);
    }

    public char getSuitRep() {
        switch (suit) {
            case CLUB:
                return '♣';
            case DIAMOND:
                return '♦';
            case HEART:
                return '♥';
            case SPADE:
                return '♠';
            default:
                throw new RuntimeException("Unexpected suit");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Card card = (Card) o;

        if (num != card.num) return false;
        return suit == card.suit;
    }

    @Override
    public int hashCode() {
        return cardIndex();
    }

    private int cardIndex() {
        return (num - 1) * 4 + suit;
    }
}
