package trashsoftware.solitaire.core.solitaireGame;

import trashsoftware.solitaire.util.Configs;

public class SolitaireRules {

    private int initialFinishes = 0;
    private boolean strict = true;

    private SolitaireRules() {
    }

    public int getInitialFinishes() {
        return initialFinishes;
    }

    /**
     * @return whether to limit move length
     */
    public boolean isStrict() {
        return strict;
    }

    public static class Builder {
        private final SolitaireRules rules = new SolitaireRules();

        public Builder initialFinishes(int initialFinished) {
            rules.initialFinishes = initialFinished;
            return this;
        }

        public Builder strict(boolean strict) {
            rules.strict = strict;
            return this;
        }

        public SolitaireRules build() {
            return rules;
        }
    }

    public static int loadInitialFinishes() {
        return Configs.getInt("initFinishes", 0);
    }
}
