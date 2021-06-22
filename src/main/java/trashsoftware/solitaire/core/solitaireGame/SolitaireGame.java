package trashsoftware.solitaire.core.solitaireGame;


import java.util.*;

public class SolitaireGame {

    final SolitaireDeck[] mainArea = new SolitaireDeck[8];
    final Card[] spaceArea = new Card[4];
    final SolitaireDeck[] finishedArea = new SolitaireDeck[4];
    final SolitaireRules rules;
    /**
     * Moves recorder, actually a stack.
     */
    private final Deque<SolitaireMove> moves = new ArrayDeque<>();
    private int stepsCount = 0;
    private int[] finalScore = null;

    public SolitaireGame(SolitaireRules rules) {
        this.rules = rules;
        for (int i = 0; i < mainArea.length; ++i) mainArea[i] = new SolitaireDeck();
        for (int i = 0; i < finishedArea.length; ++i) finishedArea[i] = new SolitaireDeck();
        initGame();
    }

    private static void shuffle(List<Card> list) {
        Collections.shuffle(list);
    }

    public SolitaireDeck[] getMainArea() {
        return mainArea;
    }

    public Card[] getSpaceArea() {
        return spaceArea;
    }

    public SolitaireDeck[] getFinishedArea() {
        return finishedArea;
    }

    public SolitaireRules getRules() {
        return rules;
    }

    public void restartGame() {
        while (hasMoveToUndo()) undo();
        stepsCount = 0;
        finalScore = null;
    }

    public boolean canAutoFinish() {
        for (SolitaireDeck deck : mainArea) {
            if (deck.size() > 1) {
                for (int i = 1; i < deck.size(); ++i) {
                    Card prev = deck.get(i - 1);
                    Card cur = deck.get(i);
                    if (prev.getNum() <= cur.getNum()) return false;
                }
            }
        }
        return true;
    }

    public SolitaireMove nextAutoMove() {
        for (int c = 0; c < mainArea.length; ++c) {
            SolitaireDeck deck = mainArea[c];
            Card surface = deck.getSurfaceCard();
            if (SolitaireMove.movableToFinished(this, surface)) {  // null check inside this
                CardLocation.FinishedLocation dst = new CardLocation.FinishedLocation(
                        this,
                        finishedArea[surface.getSuit()].getSurfaceCard(),
                        surface.getSuit());
                CardLocation.MainLocation src = new CardLocation.MainLocation(
                        this,
                        surface,
                        c,
                        deck.size() - 1);
                return new SolitaireMove.MainToFinished(
                        this,
                        src,
                        dst
                );
            }
        }
        for (int i = 0; i < spaceArea.length; ++i) {
            Card card = spaceArea[i];
            if (SolitaireMove.movableToFinished(this, card)) {
                CardLocation.FinishedLocation dst = new CardLocation.FinishedLocation(
                        this,
                        finishedArea[card.getSuit()].getSurfaceCard(),
                        card.getSuit());
                CardLocation.SpaceLocation src = new CardLocation.SpaceLocation(
                        this,
                        card,
                        i
                );
                return new SolitaireMove.SpaceToFinished(
                        this,
                        src,
                        dst
                );
            }
        }
        throw new SolitaireException("Unexpected error, cannot auto finish.");
    }

    private void initGame() {
        LinkedList<Card> cards = new LinkedList<>();
        for (int i = 1; i <= 13; ++i) {
            for (int j = 0; j < 4; ++j) {
                cards.add(new Card(i, j));
            }
        }
        // initial finishes
        for (int x = 0; x < rules.getInitialFinishes() * 4; ++x) {
            Card card = cards.removeFirst();
            finishedArea[card.getSuit()].add(card);
        }
        shuffle(cards);
        int index = 0;
        while (!cards.isEmpty()) {
            Card card = cards.removeFirst();
            mainArea[(index++) % mainArea.length].add(card);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SolitaireGame{\nFinished:\n");
        for (SolitaireDeck finished : finishedArea) {
            builder.append(finished.getSurfaceCard()).append(", ");
        }
        builder.append("\nSpace:\n");
        builder.append(Arrays.toString(spaceArea));
        builder.append("\nMain area:\n");
        for (int i = 0; i < mainArea.length; ++i) {
            SolitaireDeck deck = mainArea[i];
            builder.append(i).append(": ").append(deck.toString()).append("\n");
        }
        return builder.toString();
    }

    public SolitaireHint getHint() {
        List<SolitaireHint> hints = new ArrayList<>();
        for (int c = 0; c < mainArea.length; ++c) {
            SolitaireDeck deck = mainArea[c];
            if (deck.isEmpty()) {
                hints.add(new SolitaireHint(
                        null,
                        new CardLocation.MainLocation(
                                this,
                                null,
                                c,
                                0),  // all +1 for main destinations
                        SolitaireHint.PRE_EMPTY_MAIN
                ));
                continue;
            }

            Card surface = deck.getSurfaceCard();

            // try main to finished
            if (SolitaireMove.movableToFinished(this, surface)) {
                hints.add(new SolitaireHint(
                        new CardLocation.MainLocation(
                                this,
                                surface,
                                c,
                                deck.size() - 1),
                        createFinished(surface),
                        SolitaireHint.PRE_MAIN_TO_FINISH));
            }

            // try main to main
            int srcContinuous = deck.getContinuousCount();
            for (int r = deck.size() - srcContinuous; r < deck.size(); ++r) {
                CardLocation.MainLocation src = new CardLocation.MainLocation(
                        this,
                        deck.get(r),
                        c,
                        r
                );
                int moveCount = deck.size() - r;
                for (int dc = 0; dc < mainArea.length; ++dc) {
                    if (dc != c) {
                        SolitaireDeck dstDeck = mainArea[dc];
                        CardLocation.MainLocation dst = new CardLocation.MainLocation(
                                this,
                                dstDeck.getSurfaceCard(),
                                dc,
                                dstDeck.size() - 1
                        );
                        SolitaireMove move = new SolitaireMove.MainToMain(
                                this, src, dst
                        );
                        if (move.movable()) {
                            int dstContinuousAfter = dstDeck.getContinuousCount() + moveCount;
                            if (dstContinuousAfter > srcContinuous) {
                                // longer continuous after move
                                hints.add(new SolitaireHint(
                                        src,
                                        dst,
                                        SolitaireHint.PRE_MAIN_TO_MAIN));
                            }
                        }
                    }
                }
            }
        }

        // try space area
        for (int pos = 0; pos < spaceArea.length; ++pos) {
            CardLocation.SpaceLocation spaceLocation = new CardLocation.SpaceLocation(
                    this, spaceArea[pos], pos
            );

            // use this space
            if (spaceArea[pos] == null) {
                hints.add(new SolitaireHint(
                        null,
                        spaceLocation,
                        SolitaireHint.PRE_EMPTY_SPACE
                ));
                continue;
            }

            // try space to finished
            if (SolitaireMove.movableToFinished(this, spaceArea[pos])) {
                hints.add(new SolitaireHint(
                        spaceLocation,
                        createFinished(spaceArea[pos]),
                        SolitaireHint.PRE_SPACE_TO_FINISH
                ));
            }

            // try space to main
            for (int c = 0; c < mainArea.length; ++c) {
                SolitaireDeck deck = mainArea[c];
                if (deck.appendable(spaceArea[pos])) {
                    hints.add(new SolitaireHint(
                            spaceLocation,
                            new CardLocation.MainLocation(
                                    this,
                                    deck.getSurfaceCard(),
                                    c,
                                    deck.size() - 1
                            ),
                            SolitaireHint.PRE_SPACE_TO_MAIN
                    ));
                }
            }
        }

        if (hints.isEmpty()) return null;

        Collections.sort(hints);

        return hints.get(0);
    }

    private CardLocation.FinishedLocation createFinished(Card card) {
        return new CardLocation.FinishedLocation(
                this,
                finishedArea[card.getSuit()].getSurfaceCard(),
                card.getSuit()
        );
    }

    public boolean movable(SolitaireMove move) {
        return move.movable();
    }

    public boolean move(SolitaireMove move) {
        boolean suc = innerMove(move);
        if (suc) {
            moves.addLast(move);
            stepsCount++;
        }
        return suc;
    }

    private boolean innerMove(SolitaireMove move) {
        return move.move();
    }

    public boolean hasMoveToUndo() {
        return !moves.isEmpty();
    }

    public void undo() {
        SolitaireMove lastMove = moves.removeLast();
        lastMove.undoMove();
    }

    public SolitaireMove getLastDoneMove() {
        return moves.getLast();
    }

    public boolean wining() {
        for (SolitaireDeck deck : finishedArea) {
            Card surface = deck.getSurfaceCard();
            if (surface == null || surface.getNum() != 13) return false;
        }
        return true;
    }

    private int getBaseScore() {
        int initialFinishes = getRules().getInitialFinishes();
        int finishedScore = 0;
        for (SolitaireDeck deck : getFinishedArea()) {
            if (deck.getSurfaceCard() != null) {
                finishedScore += (deck.getSurfaceCard().getNum() - initialFinishes) * getRules().getEachCardScore();
            }
        }
        return finishedScore;
    }

    /**
     * @param seconds time used
     * @return an integer array, in [currentScore, timeScore]
     */
    public int[] getCurScore(int seconds) {
        int timeReduced = getTimerReduced(seconds);
        return new int[]{getBaseScore(), timeReduced};
    }

    /**
     * This method must be called after game finished.
     *
     * @param seconds time used
     * @return an integer array, in [totalScore, baseScore, stepScore, timeReducedScore]
     */
    public int[] getFinalScore(int seconds) {
        if (finalScore == null) {
            int baseScore = getBaseScore();
            int timeReduced = getTimerReduced(seconds);
            int stepScore = wining() ?
                    (int) ((double) getRules().getEachCardScore() / stepsCount * 1500 / getDifficultyMultiplier()) :
                    0;
            finalScore = new int[]{baseScore + stepScore - timeReduced, baseScore, stepScore, timeReduced};
        }
        return finalScore;
    }

    private int getTimerReduced(int seconds) {
        return (int) (getRules().getEachCardScore() * getDifficultyMultiplier() / 20.0 * seconds);
    }

    private double getDifficultyMultiplier() {
        return 1 + (getRules().getInitialFinishes() / 2.0);
    }

    public int getStepsCount() {
        return stepsCount;
    }

    /**
     * @param dstIsEmpty whether the destination column is empty
     * @return the maximum combo moving length from main to main
     */
    int getMaxMoveLength(boolean dstIsEmpty) {
        int m = getEmptySpaceArea();
        int n = getEmptyMainArea();
        if (dstIsEmpty) {
            return (m + 1) * n;
        } else {
            return (m + 1) * (n + 1);
        }
    }

    private int getEmptySpaceArea() {
        int count = 0;
        for (Card card : spaceArea) if (card == null) count++;
        return count;
    }

    private int getEmptyMainArea() {
        int count = 0;
        for (SolitaireDeck deck : mainArea) if (deck.isEmpty()) count++;
        return count;
    }
}
