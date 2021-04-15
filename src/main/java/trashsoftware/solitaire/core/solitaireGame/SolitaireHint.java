package trashsoftware.solitaire.core.solitaireGame;

public class SolitaireHint implements Comparable<SolitaireHint> {

    // The lower is the better
    public static final int PRE_SPACE_TO_FINISH = 1;
    public static final int PRE_MAIN_TO_FINISH = 2;
    public static final int PRE_SPACE_TO_MAIN = 3;
    public static final int PRE_MAIN_TO_MAIN = 4;
    public static final int PRE_EMPTY_SPACE = 5;
    public static final int PRE_EMPTY_MAIN = 6;

    private final CardLocation srcLocation;
    private final CardLocation dstLocation;
    private final int precedence;

    public SolitaireHint(CardLocation srcLocation, CardLocation dstLocation, int precedence) {
        this.srcLocation = srcLocation;
        this.dstLocation = dstLocation;
        this.precedence = precedence;
    }

    public CardLocation getSrcLocation() {
        return srcLocation;
    }

    public CardLocation getDstLocation() {
        return dstLocation;
    }

    @Override
    public int compareTo(SolitaireHint o) {
        return Integer.compare(this.precedence, o.precedence);
    }

    @Override
    public String toString() {
        return String.format("Hint{%s to %s}", srcLocation, dstLocation);
    }
}
