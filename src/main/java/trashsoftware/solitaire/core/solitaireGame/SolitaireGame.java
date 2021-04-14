package trashsoftware.solitaire.core.solitaireGame;


import trashsoftware.solitaire.Main;

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

    public void autoFinishNext() {
        for (SolitaireDeck deck : mainArea) {

        }
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

    public boolean wining() {
        for (SolitaireDeck deck : finishedArea) {
            Card surface = deck.getSurfaceCard();
            if (surface == null || surface.getNum() != 13) return false;
        }
        return true;
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
