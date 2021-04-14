package trashsoftware.solitaire.core.solitaireGame;

public abstract class SolitaireMove {

    protected final SolitaireGame game;

    public SolitaireMove(SolitaireGame game) {
        this.game = game;
    }

    /**
     * Perform this move, returns {@code true} if the move is a success, {@code false} otherwise.
     *
     * Do not modify any status of the game.
     *
     * @return {@code true} if the move is a success, {@code false} otherwise
     */
    public abstract boolean move();

    /**
     * Undo this move.
     * <p>
     * Precondition: the move was a success.
     */
    public abstract void undoMove();

    protected static boolean moveToFinished(SolitaireGame game, Card card) {
        SolitaireDeck deck = game.finishedArea[card.getSuit()];
        Card surface = deck.getSurfaceCard();
        if (surface == null) {
            if (card.getNum() == 1) {
                deck.add(card);
                return true;
            }
        } else {
            if (card.getNum() == surface.getNum() + 1) {
                deck.add(card);
                return true;
            }
        }
        return false;
    }

    public static class MainToMain extends SolitaireMove {
        public final int fromCol, fromRow, toCol;
        private final int movingCardsCount;

        public MainToMain(SolitaireGame game, int fromCol, int fromRow, int toCol) {
            super(game);

            this.fromCol = fromCol;
            this.fromRow = fromRow;
            this.toCol = toCol;
            this.movingCardsCount = game.mainArea[fromCol].size() - fromRow;
        }

        @Override
        public boolean move() {
            SolitaireDeck srcDeck = game.mainArea[fromCol];
            SolitaireDeck dstDeck = game.mainArea[toCol];
            Card dstSurface = dstDeck.getSurfaceCard();
            if (fromRow == srcDeck.size() - 1) {
                if (dstSurface == null) {
                    dstDeck.add(srcDeck.removeSurfaceCard());
                    return true;
                } else {
                    Card srcSurface = srcDeck.getSurfaceCard();
                    if (dstDeck.appendable(srcSurface)) {
                        dstDeck.add(srcDeck.removeSurfaceCard());
                        return true;
                    }
                }
            } else if (fromRow < srcDeck.size() - 1) {
                int moveCount = srcDeck.size() - fromRow;
                if (game.rules.isStrict()) {
                    if (moveCount <= game.getMaxMoveLength(dstDeck.isEmpty())) {
                        if (dstDeck.appendable(srcDeck.get(fromRow))) {
                            dstDeck.addAll(srcDeck.subList(fromRow, srcDeck.size()));
                            for (int i = 0; i < moveCount; ++i) {
                                srcDeck.removeSurfaceCard();
                            }
                            return true;
                        }
                    }
                } else {
                    if (dstDeck.appendable(srcDeck.get(fromRow))) {
                        dstDeck.addAll(srcDeck.subList(fromRow, srcDeck.size()));
                        for (int i = 0; i < moveCount; ++i) {
                            srcDeck.removeSurfaceCard();
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void undoMove() {
            SolitaireDeck srcDeck = game.mainArea[fromCol];
            SolitaireDeck dstDeck = game.mainArea[toCol];
            SolitaireDeck sub = new SolitaireDeck(dstDeck.subList(dstDeck.size() - movingCardsCount, dstDeck.size()));
            srcDeck.addAll(sub);
            for (int i = 0; i < movingCardsCount; ++i) {
                dstDeck.removeSurfaceCard();
            }
        }
    }

    public static class MainToSpace extends SolitaireMove {
        public final int fromCol, fromRow, toPos;

        public MainToSpace(SolitaireGame game, int fromCol, int fromRow, int toPos) {
            super(game);

            this.fromCol = fromCol;
            this.fromRow = fromRow;
            this.toPos = toPos;
        }

        @Override
        public boolean move() {
            SolitaireDeck deck = game.mainArea[fromCol];
            if (fromRow == deck.size() - 1) {
                if (game.spaceArea[toPos] == null) {
                    game.spaceArea[toPos] = deck.removeSurfaceCard();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void undoMove() {
            Card card = game.spaceArea[toPos];
            game.spaceArea[toPos] = null;
            game.mainArea[fromCol].add(card);
        }
    }

    public static class SpaceToMain extends SolitaireMove {
        public final int fromPos, toCol;

        public SpaceToMain(SolitaireGame game, int fromPos, int toCol) {
            super(game);

            this.fromPos = fromPos;
            this.toCol = toCol;
        }

        @Override
        public boolean move() {
            Card card = game.spaceArea[fromPos];
            if (card != null) {
                SolitaireDeck dstDeck = game.mainArea[toCol];
                if (dstDeck.appendable(card)) {
                    dstDeck.add(card);
                    game.spaceArea[fromPos] = null;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void undoMove() {
            game.spaceArea[fromPos] = game.mainArea[fromPos].removeSurfaceCard();
        }
    }

    public static class SpaceToSpace extends SolitaireMove {
        public final int fromPos, toPos;

        public SpaceToSpace(SolitaireGame game, int fromPos, int toPos) {
            super(game);

            this.fromPos = fromPos;
            this.toPos = toPos;
        }

        @Override
        public boolean move() {
            Card card = game.spaceArea[fromPos];
            if (card != null) {
                if (game.spaceArea[toPos] == null) {
                    game.spaceArea[toPos] = card;
                    game.spaceArea[fromPos] = null;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void undoMove() {
            game.spaceArea[fromPos] = game.spaceArea[toPos];
            game.spaceArea[toPos] = null;
        }
    }

    public static class MainToFinished extends SolitaireMove {
        public final int fromCol, fromRow, toPos;

        public MainToFinished(SolitaireGame game, int fromCol, int fromRow, int toPos) {
            super(game);

            this.fromCol = fromCol;
            this.fromRow = fromRow;
            this.toPos = toPos;
        }

        @Override
        public boolean move() {
            SolitaireDeck deck = game.mainArea[fromCol];
            if (fromRow == deck.size() - 1) {
                Card card = deck.getSurfaceCard();
                if (moveToFinished(game, card)) {
                    deck.removeSurfaceCard();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void undoMove() {
            game.mainArea[fromCol].add(game.finishedArea[toPos].removeSurfaceCard());
        }
    }

    public static class FinishedToMain extends SolitaireMove {
        public final int fromPos, toCol;

        public FinishedToMain(SolitaireGame game, int fromPos, int toCol) {
            super(game);

            this.fromPos = fromPos;
            this.toCol = toCol;
        }

        @Override
        public boolean move() {
            SolitaireDeck deck = game.finishedArea[fromPos];
            Card surface = deck.getSurfaceCard();
            if (surface != null) {
                SolitaireDeck dstDeck = game.mainArea[toCol];
                if (dstDeck.appendable(surface)) {
                    dstDeck.add(surface);
                    deck.removeSurfaceCard();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void undoMove() {
            game.finishedArea[fromPos].add(game.mainArea[toCol].removeSurfaceCard());
        }
    }

    public static class SpaceToFinished extends SolitaireMove {
        public final int fromPos, toPos;

        public SpaceToFinished(SolitaireGame game, int fromPos, int toPos) {
            super(game);

            this.fromPos = fromPos;
            this.toPos = toPos;
        }

        @Override
        public boolean move() {
            Card card = game.spaceArea[fromPos];
            if (card != null) {
                if (moveToFinished(game, card)) {
                    game.spaceArea[fromPos] = null;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void undoMove() {
            game.spaceArea[fromPos] = game.finishedArea[toPos].removeSurfaceCard();
        }
    }

    public static class FinishedToSpace extends SolitaireMove {
        public final int fromPos, toPos;

        public FinishedToSpace(SolitaireGame game, int fromPos, int toPos) {
            super(game);

            this.fromPos = fromPos;
            this.toPos = toPos;
        }

        @Override
        public boolean move() {
            SolitaireDeck deck = game.finishedArea[fromPos];
            Card surface = deck.getSurfaceCard();
            if (surface != null) {
                if (game.spaceArea[toPos] == null) {
                    game.spaceArea[toPos] = deck.removeSurfaceCard();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void undoMove() {
            game.finishedArea[fromPos].add(game.spaceArea[toPos]);
            game.spaceArea[toPos] = null;
        }
    }

    public static class FinishedToFinished extends SolitaireMove {

        public FinishedToFinished(SolitaireGame game) {
            super(game);
        }

        /**
         * @return always return false
         */
        @Override
        public boolean move() {
            return false;
        }

        @Override
        public void undoMove() {
        }
    }
}
