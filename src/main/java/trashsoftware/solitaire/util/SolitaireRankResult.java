package trashsoftware.solitaire.util;

public class SolitaireRankResult {
    public final int scoreRank;
    public final SolitaireRecord scoreBest;
    public final int timeRank;
    public final SolitaireRecord timeBest;
    public final int stepsRank;
    public final SolitaireRecord stepsBest;

    SolitaireRankResult(int scoreRank, SolitaireRecord scoreBest,
                        int timeRank, SolitaireRecord timeBest,
                        int stepsRank, SolitaireRecord stepsBest) {
        this.scoreRank = scoreRank;
        this.scoreBest = scoreBest;
        this.timeRank = timeRank;
        this.timeBest = timeBest;
        this.stepsRank = stepsRank;
        this.stepsBest = stepsBest;
    }
}
