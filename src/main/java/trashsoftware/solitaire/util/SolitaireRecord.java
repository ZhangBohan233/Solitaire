package trashsoftware.solitaire.util;

import java.util.Date;

public class SolitaireRecord extends GameRecord {
    public final int seconds;
    public final int score;
    public final int steps;
    public final int initFinish;
    public final Date date;

    public SolitaireRecord(int initFinish, int seconds, int score, int steps, Date date) {
        this.seconds = seconds;
        this.score = score;
        this.steps = steps;
        this.date = date;
        this.initFinish = initFinish;
    }
}
