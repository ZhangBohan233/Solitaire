package trashsoftware.solitaire.core.solitaireGame;

public abstract class SolitaireMove {

    protected final SolitaireGame game;

    public SolitaireMove(SolitaireGame game) {
        this.game = game;
    }

    protected static boolean movableToFinished(SolitaireGame game, Card card) {
        if (card == null) return false;
        SolitaireDeck deck = game.finishedArea[card.getSuit()];
        Card surface = deck.getSurfaceCard();
        if (surface == null) {
            return card.getNum() == 1;
        } else {
            return card.getNum() == surface.getNum() + 1;
        }
    }

    protected static boolean moveToFinished(SolitaireGame game, Card card) {
        if (card == null) return false;
        if (movableToFinished(game, card)) {
            SolitaireDeck deck = game.finishedArea[card.getSuit()];
            deck.add(card);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Perform this move, returns {@code true} if the move is a success, {@code false} otherwise.
     * <p>
     * Do not modify any status of the game.
     *
     * @return {@code true} if the move is a success, {@code false} otherwise
     */
    public abstract boolean move();

    public abstract boolean movable();

    /**
     * Undo this move.
     * <p>
     * Precondition: the move was a success.
     */
    public abstract void undoMove();

    public abstract CardLocation getSrcLocation();

    public abstract CardLocation getDstLocation();

    public static class MainToMain extends SolitaireMove {
        private final CardLocation.MainLocation srcLocation, dstLocation;
        private final int movingCardsCount;

        public MainToMain(SolitaireGame game,
                          CardLocation.MainLocation srcLocation,
                          CardLocation.MainLocation dstLocation) {
            super(game);

            this.srcLocation = srcLocation;
            this.dstLocation = dstLocation;
            this.movingCardsCount = game.mainArea[srcLocation.getCol()].size() - srcLocation.getRow();
        }

        @Override
        public CardLocation.MainLocation getSrcLocation() {
            return srcLocation;
        }

        @Override
        public CardLocation.MainLocation getDstLocation() {
            return dstLocation;
        }

        @Override
        public boolean movable() {
            SolitaireDeck srcDeck = game.mainArea[srcLocation.getCol()];
            SolitaireDeck dstDeck = game.mainArea[dstLocation.getCol()];
            Card dstSurface = dstDeck.getSurfaceCard();
            if (srcLocation.getRow() == srcDeck.size() - 1) {
                if (dstSurface == null) {
                    return true;
                } else {
                    Card srcSurface = srcDeck.getSurfaceCard();
                    return dstDeck.appendable(srcSurface);
                }
            } else if (srcLocation.getRow() < srcDeck.size() - 1) {
                int moveCount = srcDeck.size() - srcLocation.getRow();
                if (game.rules.isStrict()) {
                    if (moveCount <= game.getMaxMoveLength(dstDeck.isEmpty())) {
                        return dstDeck.appendable(srcDeck.get(srcLocation.getRow()));
                    }
                } else {
                    return dstDeck.appendable(srcDeck.get(srcLocation.getRow()));
                }
            }
            return false;
        }

        @Override
        public boolean move() {
            if (movable()) {
                SolitaireDeck srcDeck = game.mainArea[srcLocation.getCol()];
                SolitaireDeck dstDeck = game.mainArea[dstLocation.getCol()];
                dstDeck.addAll(srcDeck.removeToSize(srcLocation.getRow()));
                return true;
            } else {
                return false;
            }
//            SolitaireDeck srcDeck = game.mainArea[srcLocation.getCol()];
//            SolitaireDeck dstDeck = game.mainArea[dstLocation.getCol()];
//            Card dstSurface = dstDeck.getSurfaceCard();
//            if (srcLocation.getRow() == srcDeck.size() - 1) {
//                if (dstSurface == null) {
//                    dstDeck.add(srcDeck.removeSurfaceCard());
//                    return true;
//                } else {
//                    Card srcSurface = srcDeck.getSurfaceCard();
//                    if (dstDeck.appendable(srcSurface)) {
//                        dstDeck.add(srcDeck.removeSurfaceCard());
//                        return true;
//                    }
//                }
//            } else if (srcLocation.getRow() < srcDeck.size() - 1) {
//                int moveCount = srcDeck.size() - srcLocation.getRow();
//                if (game.rules.isStrict()) {
//                    if (moveCount <= game.getMaxMoveLength(dstDeck.isEmpty())) {
//                        if (dstDeck.appendable(srcDeck.get(srcLocation.getRow()))) {
//                            dstDeck.addAll(srcDeck.subList(srcLocation.getRow(), srcDeck.size()));
//                            for (int i = 0; i < moveCount; ++i) {
//                                srcDeck.removeSurfaceCard();
//                            }
//                            return true;
//                        }
//                    }
//                } else {
//                    if (dstDeck.appendable(srcDeck.get(srcLocation.getRow()))) {
//                        dstDeck.addAll(srcDeck.subList(srcLocation.getRow(), srcDeck.size()));
//                        for (int i = 0; i < moveCount; ++i) {
//                            srcDeck.removeSurfaceCard();
//                        }
//                        return true;
//                    }
//                }
//            }
//            return false;
        }

        @Override
        public void undoMove() {
            SolitaireDeck srcDeck = game.mainArea[srcLocation.getCol()];
            SolitaireDeck dstDeck = game.mainArea[dstLocation.getCol()];
            SolitaireDeck sub = new SolitaireDeck(dstDeck.subList(dstDeck.size() - movingCardsCount, dstDeck.size()));
            srcDeck.addAll(sub);
            for (int i = 0; i < movingCardsCount; ++i) {
                dstDeck.removeSurfaceCard();
            }
        }
    }

    public static class MainToSpace extends SolitaireMove {
        private final CardLocation.MainLocation srcLocation;
        private final CardLocation.SpaceLocation dstLocation;

        public MainToSpace(SolitaireGame game,
                           CardLocation.MainLocation srcLocation,
                           CardLocation.SpaceLocation dstLocation) {
            super(game);

            this.srcLocation = srcLocation;
            this.dstLocation = dstLocation;
        }

        @Override
        public CardLocation.MainLocation getSrcLocation() {
            return srcLocation;
        }

        @Override
        public CardLocation.SpaceLocation getDstLocation() {
            return dstLocation;
        }

        @Override
        public boolean movable() {
            SolitaireDeck deck = game.mainArea[srcLocation.getCol()];
            if (srcLocation.getRow() == deck.size() - 1) {
                return game.spaceArea[dstLocation.getPos()] == null;
            }
            return false;
        }

        @Override
        public boolean move() {
            if (movable()) {
                SolitaireDeck deck = game.mainArea[srcLocation.getCol()];
                game.spaceArea[dstLocation.getPos()] = deck.removeSurfaceCard();
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void undoMove() {
            Card card = game.spaceArea[dstLocation.getPos()];
            game.spaceArea[dstLocation.getPos()] = null;
            game.mainArea[srcLocation.getCol()].add(card);
        }
    }

    public static class SpaceToMain extends SolitaireMove {
        private final CardLocation.SpaceLocation srcLocation;
        private final CardLocation.MainLocation dstLocation;

        public SpaceToMain(SolitaireGame game,
                           CardLocation.SpaceLocation srcLocation,
                           CardLocation.MainLocation dstLocation) {
            super(game);

            this.srcLocation = srcLocation;
            this.dstLocation = dstLocation;
        }

        @Override
        public CardLocation.SpaceLocation getSrcLocation() {
            return srcLocation;
        }

        @Override
        public CardLocation.MainLocation getDstLocation() {
            return dstLocation;
        }

        @Override
        public boolean movable() {
            Card card = game.spaceArea[srcLocation.getPos()];
            if (card != null) {
                SolitaireDeck dstDeck = game.mainArea[dstLocation.getCol()];
                return dstDeck.appendable(card);
            }
            return false;
        }

        @Override
        public boolean move() {
            Card card = game.spaceArea[srcLocation.getPos()];
            if (card != null) {
                SolitaireDeck dstDeck = game.mainArea[dstLocation.getCol()];
                if (dstDeck.appendable(card)) {
                    dstDeck.add(card);
                    game.spaceArea[srcLocation.getPos()] = null;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void undoMove() {
            game.spaceArea[srcLocation.getPos()] = game.mainArea[dstLocation.getCol()].removeSurfaceCard();
        }
    }

    public static class SpaceToSpace extends SolitaireMove {
        private final CardLocation.SpaceLocation srcLocation, dstLocation;

        public SpaceToSpace(SolitaireGame game,
                            CardLocation.SpaceLocation srcLocation,
                            CardLocation.SpaceLocation dstLocation) {
            super(game);

            this.srcLocation = srcLocation;
            this.dstLocation = dstLocation;
        }

        @Override
        public CardLocation.SpaceLocation getSrcLocation() {
            return srcLocation;
        }

        @Override
        public CardLocation.SpaceLocation getDstLocation() {
            return dstLocation;
        }

        @Override
        public boolean movable() {
            Card card = game.spaceArea[srcLocation.getPos()];
            if (card != null) {
                return game.spaceArea[dstLocation.getPos()] == null;
            }
            return false;
        }

        @Override
        public boolean move() {
            Card card = game.spaceArea[srcLocation.getPos()];
            if (card != null) {
                if (game.spaceArea[dstLocation.getPos()] == null) {
                    game.spaceArea[dstLocation.getPos()] = card;
                    game.spaceArea[srcLocation.getPos()] = null;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void undoMove() {
            game.spaceArea[srcLocation.getPos()] = game.spaceArea[dstLocation.getPos()];
            game.spaceArea[dstLocation.getPos()] = null;
        }
    }

    public static class MainToFinished extends SolitaireMove {
        private final CardLocation.MainLocation srcLocation;
        private final CardLocation.FinishedLocation dstLocation;

        public MainToFinished(SolitaireGame game,
                              CardLocation.MainLocation srcLocation,
                              CardLocation.FinishedLocation dstLocation) {
            super(game);

            this.srcLocation = srcLocation;
            this.dstLocation = dstLocation;
        }

        @Override
        public CardLocation.MainLocation getSrcLocation() {
            return srcLocation;
        }

        @Override
        public CardLocation.FinishedLocation getDstLocation() {
            return dstLocation;
        }

        @Override
        public boolean movable() {
            SolitaireDeck deck = game.mainArea[srcLocation.getCol()];
            if (srcLocation.getRow() == deck.size() - 1) {
                Card card = deck.getSurfaceCard();
                return movableToFinished(game, card);
            }
            return false;
        }

        @Override
        public boolean move() {
            SolitaireDeck deck = game.mainArea[srcLocation.getCol()];
            if (srcLocation.getRow() == deck.size() - 1) {
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
            game.mainArea[srcLocation.getCol()].add(game.finishedArea[dstLocation.getPos()].removeSurfaceCard());
        }
    }

    public static class FinishedToMain extends SolitaireMove {
        private final CardLocation.FinishedLocation srcLocation;
        private final CardLocation.MainLocation dstLocation;

        public FinishedToMain(SolitaireGame game,
                              CardLocation.FinishedLocation srcLocation,
                              CardLocation.MainLocation dstLocation) {
            super(game);

            this.srcLocation = srcLocation;
            this.dstLocation = dstLocation;
        }

        @Override
        public CardLocation.FinishedLocation getSrcLocation() {
            return srcLocation;
        }

        @Override
        public CardLocation.MainLocation getDstLocation() {
            return dstLocation;
        }

        @Override
        public boolean movable() {
            SolitaireDeck deck = game.finishedArea[srcLocation.getPos()];
            Card surface = deck.getSurfaceCard();
            if (surface != null) {
                SolitaireDeck dstDeck = game.mainArea[dstLocation.getCol()];
                return dstDeck.appendable(surface);
            }
            return false;
        }

        @Override
        public boolean move() {
            SolitaireDeck deck = game.finishedArea[srcLocation.getPos()];
            Card surface = deck.getSurfaceCard();
            if (surface != null) {
                SolitaireDeck dstDeck = game.mainArea[dstLocation.getCol()];
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
            game.finishedArea[srcLocation.getPos()].add(game.mainArea[dstLocation.getCol()].removeSurfaceCard());
        }
    }

    public static class SpaceToFinished extends SolitaireMove {
        private final CardLocation.SpaceLocation srcLocation;
        private final CardLocation.FinishedLocation dstLocation;

        public SpaceToFinished(SolitaireGame game,
                               CardLocation.SpaceLocation srcLocation,
                               CardLocation.FinishedLocation dstLocation) {
            super(game);

            this.srcLocation = srcLocation;
            this.dstLocation = dstLocation;
        }

        @Override
        public boolean movable() {
            Card card = game.spaceArea[srcLocation.getPos()];
            if (card != null) {
                return movableToFinished(game, card);
            }
            return false;
        }

        @Override
        public CardLocation.SpaceLocation getSrcLocation() {
            return srcLocation;
        }

        @Override
        public CardLocation.FinishedLocation getDstLocation() {
            return dstLocation;
        }

        @Override
        public boolean move() {
            Card card = game.spaceArea[srcLocation.getPos()];
            if (card != null) {
                if (moveToFinished(game, card)) {
                    game.spaceArea[srcLocation.getPos()] = null;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void undoMove() {
            game.spaceArea[srcLocation.getPos()] = game.finishedArea[dstLocation.getPos()].removeSurfaceCard();
        }
    }

    public static class FinishedToSpace extends SolitaireMove {
        private final CardLocation.FinishedLocation srcLocation;
        private final CardLocation.SpaceLocation dstLocation;

        public FinishedToSpace(SolitaireGame game,
                               CardLocation.FinishedLocation srcLocation,
                               CardLocation.SpaceLocation dstLocation) {
            super(game);

            this.srcLocation = srcLocation;
            this.dstLocation = dstLocation;
        }

        @Override
        public CardLocation.FinishedLocation getSrcLocation() {
            return srcLocation;
        }

        @Override
        public CardLocation.SpaceLocation getDstLocation() {
            return dstLocation;
        }

        @Override
        public boolean movable() {
            SolitaireDeck deck = game.finishedArea[srcLocation.getPos()];
            Card surface = deck.getSurfaceCard();
            if (surface != null) {
                return game.spaceArea[dstLocation.getPos()] == null;
            }
            return false;
        }

        @Override
        public boolean move() {
            SolitaireDeck deck = game.finishedArea[srcLocation.getPos()];
            Card surface = deck.getSurfaceCard();
            if (surface != null) {
                if (game.spaceArea[dstLocation.getPos()] == null) {
                    game.spaceArea[dstLocation.getPos()] = deck.removeSurfaceCard();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void undoMove() {
            game.finishedArea[srcLocation.getPos()].add(game.spaceArea[dstLocation.getPos()]);
            game.spaceArea[dstLocation.getPos()] = null;
        }
    }

    public static class FinishedToFinished extends SolitaireMove {

        public FinishedToFinished(SolitaireGame game) {
            super(game);
        }

        @Override
        public CardLocation getSrcLocation() {
            return null;
        }

        @Override
        public CardLocation getDstLocation() {
            return null;
        }

        @Override
        public boolean movable() {
            return false;
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
