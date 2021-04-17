package trashsoftware.solitaire.fxml.controls;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import trashsoftware.solitaire.Main;
import trashsoftware.solitaire.core.solitaireGame.*;
import trashsoftware.solitaire.util.Configs;
import trashsoftware.solitaire.util.SolitaireRankResult;
import trashsoftware.solitaire.util.SolitaireRecord;
import trashsoftware.solitaire.util.SolitaireRecorder;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class GameView implements Initializable {
    private static final Paint RED = Paint.valueOf("red");
    private static final Paint BLACK = Paint.valueOf("black");
    private static final Paint BACKGROUND = Paint.valueOf("green");
    private static final Paint LINE = Paint.valueOf("gray");
    private static final Paint CARD_LINE = Paint.valueOf("black");
    private static final Paint CARD = Paint.valueOf("white");
    private static final Paint HIGHLIGHT = Paint.valueOf("gold");
    private static final Paint TEXT = Paint.valueOf("black");
    private static final Paint HINT = Paint.valueOf("yellow");
    private static final int NOT_IN_AREA = -1;
    private static final int IN_AREA_NOT_AT_CARD = -2;
    private static final int[] NOT_IN_AREA_ARRAY = null;
    private static final int[] IN_AREA_NOT_AT_CARD_ARRAY = new int[0];
    private static final InvalidDrag INVALID_DRAG = new InvalidDrag();
    private final double cardBorder = 5.0;
    private final double cardWidth = 90.0;
    private final double blockWidth = cardWidth + cardBorder * 2;
    private final double cardGapHeight = 40.0;
    private final double cardFullHeight = 120.0;
    private final double blockHeight = cardFullHeight + cardBorder * 2;
    private final double mainSpacing = 40.0;
    private final double areaSpacing = 10.0;  // card real spacing for finished area and space area
    private final double width = cardWidth * 8 + mainSpacing * 10;
    private final double mainAreaY = areaSpacing * 3 + blockHeight;
    private final double firstRowY = mainAreaY + cardBorder;
    private final int prefRowsCount = 10;
    private final double prefHeight = requiredHeightOfMain(prefRowsCount);
    private final double timerY = 20.0;
    @FXML
    ScrollPane basePane;
    @FXML
    Canvas canvas;
    @FXML
    MenuItem undoItem, autoFinishItem;
    @FXML
    Menu editMenu;

    private double animationDuration = 500.0;
    private double frameTime = 25.0;
    private double endAnimationFrameTime = 25.0;
    private GraphicsContext graphicsContext;
    private Stage stage;
    private ResourceBundle bundle;

    private SolitaireGame game;
    private CardLocation selected;
    private SolitaireHint hint;
    private boolean started = false;
    private boolean finished = false;
    private boolean recorded = false;

    private Timeline winingAnimation;
    private DraggedCards draggedCards;
    private AnimatingCards animatingCards;
    private double curMouseX, curMouseY;
    private long lastDragTime;

    private Timer timer;
    private GameTimerTask timeCounter;
    private int timerSeconds;
    private SolitaireRankResult rankedScores;

    private static String secondsToString(int seconds) {
        int minutes = seconds / 60;
        int sec = seconds % 60;
        return String.format("%02d:%02d", minutes, sec);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.bundle = resources;

        graphicsContext = canvas.getGraphicsContext2D();

        setupCanvas();
        startNewGame();
    }

    public void setStage(Stage stage) {
        this.stage = stage;

        stage.setOnCloseRequest(e -> timer.cancel());
    }

    @FXML
    void undoAction() {
        clearHint();
        if (game.hasMoveToUndo()) {  // double check
            SolitaireMove move = game.getLastDoneMove();

            CardLocation dst = move.getDstLocation();
            CardLocation realDstLocation = dst.reloadLocation(row -> row + 1);

            animation(realDstLocation, move.getSrcLocation(), e -> {
                game.undo();
                animatingCards = null;
                setButtonsStatus();
                draw();
            });
        }
    }

    @FXML
    void hintAction() {
        if (animatingCards != null) return;
        selected = null;
        hint = game.getHint();
        if (hint != null) {
            if (hint.getSrcLocation() != null) {
                if (hint.getSrcLocation().getCard() == null) {
                    throw new SolitaireException("Unexpected hint.");
                }
                selected = hint.getSrcLocation();
            }
        }
        draw();
    }

    @FXML
    void autoFinishAction() {
        clearHint();
        autoFinishOneStep();
    }

    private void autoFinishOneStep() {
        SolitaireMove nextMove = game.nextAutoMove();
        animation(nextMove.getSrcLocation(), nextMove.getDstLocation(), e -> {
            game.move(nextMove);
            animatingCards = null;
            draw();
            if (!game.wining()) {
                autoFinishOneStep();
            } else {
                checkWin();
            }
        });
    }

    @FXML
    void settingsAction() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("settingsView.fxml"),
                Main.getBundle()
        );
        Parent root = loader.load();

        Stage newStage = new Stage();
        newStage.initOwner(stage);

        newStage.setTitle(Main.getBundle().getString("settings"));

        Scene scene = new Scene(root);
        newStage.setScene(scene);

        SettingsView settingsView = loader.getController();
        settingsView.setStage(newStage, this);

        newStage.show();
    }

    @FXML
    void newGameAction() {
        startNewGame();
    }

    @FXML
    void restartGameAction() {
        restartGame();
    }

    void onCanvasClicked(MouseEvent event) {
        clearHint();
        if (!started) {
            started = true;
            timeCounter = new GameTimerTask();
            timer.scheduleAtFixedRate(timeCounter, 1000, 1000);
        }
        if (winingAnimation != null) {
            terminateWiningAnimation();
            return;
        }
        if (finished) return;
        if (draggedCards != null) {
            if (draggedCards != INVALID_DRAG)
                onCanvasDragReleased(event, draggedCards);
            draggedCards = null;
            draw();
            return;
        }
        if (animatingCards != null) return;

        double x = event.getX();
        double y = event.getY();

        if (event.getClickCount() == 1) singleClick(x, y);
        else if (event.getClickCount() == 2) doubleClick(x, y);
    }

    void onCanvasDragStarted(MouseEvent event) {
        clearHint();
        if (finished) return;
        if (!started) {
            started = true;
            timer.scheduleAtFixedRate(new GameTimerTask(), 1000, 1000);
        }
        if (animatingCards != null) return;

        double x = event.getX();
        double y = event.getY();
        CardLocation location = getCardLocation(x, y);
        if (location == null || location.getCard() == null) {
            draggedCards = INVALID_DRAG;
            return;
        }

        double[] cardTopLeft = location.cardLeftXY(this);
        draggedCards = createDraggingCards(
                location,
                x - cardTopLeft[0],
                y - cardTopLeft[1]);
        if (draggedCards != INVALID_DRAG) stage.getScene().setCursor(Cursor.CLOSED_HAND);
    }

    private DraggedCards createDraggingCards(CardLocation location,
                                             double mouseXFromCardLeft,
                                             double mouseYFromCardTop) {
        if (location instanceof CardLocation.MainLocation) {
            CardLocation.MainLocation mainLocation = (CardLocation.MainLocation) location;
            SolitaireDeck deck = game.getMainArea()[mainLocation.getCol()];
            if (deck.draggable(mainLocation.getRow())) {
                SolitaireDeck dragging = new SolitaireDeck(deck.subList(mainLocation.getRow(), deck.size()));
                return new DragFromMain(mainLocation,
                        mouseXFromCardLeft,
                        mouseYFromCardTop,
                        dragging);
            } else return INVALID_DRAG;
        } else if (location instanceof CardLocation.SpaceLocation) {
            CardLocation.SpaceLocation spaceLocation = (CardLocation.SpaceLocation) location;
            return new DragFromSpace(spaceLocation,
                    mouseXFromCardLeft,
                    mouseYFromCardTop);
        } else if (location instanceof CardLocation.FinishedLocation) {
            CardLocation.FinishedLocation finishedLocation = (CardLocation.FinishedLocation) location;
            return new DragFromFinished(finishedLocation,
                    mouseXFromCardLeft,
                    mouseYFromCardTop);
        } else {
            return INVALID_DRAG;
        }
    }

    void onCanvasDragReleased(MouseEvent event, DraggedCards dragging) {
        CardLocation dstLocation = getCardLocation(event.getX(), event.getY());
        if (dstLocation != null) {
            SolitaireMove move = dragging.srcLocation.createMove(dstLocation);
            if (directMoveAction(move)) {
                checkWin();
            }
        }
        stage.getScene().setCursor(Cursor.DEFAULT);
        selected = null;
    }

    void onCanvasDragged(MouseEvent event) {
        long cur = System.currentTimeMillis();
        long dragTime = cur - lastDragTime;
        if (dragTime >= frameTime) {
            curMouseX = event.getX();
            curMouseY = event.getY();
            if (curMouseX < 0 || curMouseY < 0 || curMouseX >= width || curMouseY >= canvas.getHeight()) {
                draggedCards = null;
                draw();
                return;
            }
            draw();
            lastDragTime = cur;
        }
    }

    private boolean directMoveAction(SolitaireMove move) {
        if (game.move(move)) {
            setButtonsStatus();
            return true;
        }
        return false;
    }

    private boolean animatedMoveAction(SolitaireMove move) {
        if (game.movable(move)) {
            CardLocation realDstLocation;
            if (move.getDstLocation() instanceof CardLocation.MainLocation) {
                // in a move to main, the actual dst location is the next row
                CardLocation.MainLocation mainLocation = (CardLocation.MainLocation) move.getDstLocation();
                realDstLocation = mainLocation.reloadLocation(row -> row + 1);
            } else realDstLocation = move.getDstLocation();

            animation(move.getSrcLocation(), realDstLocation, e -> {
                game.move(move);
                animatingCards = null;
                draw();
                setButtonsStatus();
            });

            return true;
        }
        return false;
    }

    private void animation(CardLocation src, CardLocation dst, EventHandler<ActionEvent> onFinished) {
        Timeline animationLine = new Timeline();
        animationLine.setCycleCount((int) (animationDuration / frameTime));
        animationLine.getKeyFrames().add(new KeyFrame(Duration.millis(frameTime), new MoveAnimation(src, dst)));
        animationLine.setOnFinished(onFinished);
        animationLine.play();
    }

    private void playEndingAnimation() {
        winingAnimation = new Timeline();
        winingAnimation.setCycleCount(Animation.INDEFINITE);
        winingAnimation.getKeyFrames().add(
                new KeyFrame(Duration.millis(endAnimationFrameTime),
                        new EndAnimation(winingAnimation)));
        winingAnimation.play();
    }

    private void terminateWiningAnimation() {
        winingAnimation.stop();
        winingAnimation = null;
        showWinMsg();
    }

    private void singleClick(double x, double y) {
        CardLocation newSelection = getCardLocation(x, y);

        if (selected == null) {
            // newly selected
            if (newSelection != null && newSelection.getCard() != null) {
                selected = newSelection;
                draw();
            }
        } else {
            // user calls a move
            if (newSelection != null) {
                if (newSelection.equals(selected)) {  // single clicked twice to cancel selection
                    selected = null;
                    draw();
                    return;
                }

                // selected.card is guaranteed non-null
                SolitaireMove move = selected.createMove(newSelection);
                if (animatedMoveAction(move)) {
                    checkWin();
                    selected = null;
                } else {
                    selected = newSelection;
                    draw();
                }
                return;
            }
            // clear selection
            selected = null;
            draw();
        }
    }

    private void doubleClick(double x, double y) {
        CardLocation newSelection = getCardLocation(x, y);

        if (newSelection != null && newSelection.getCard() != null) {
            int pos = newSelection.getCard().getSuit();
            CardLocation.FinishedLocation fl =
                    new CardLocation.FinishedLocation(game, game.getFinishedArea()[pos].getSurfaceCard(), pos);
            SolitaireMove move = newSelection.createMove(fl);
            if (animatedMoveAction(move)) {
                checkWin();
                selected = null;
                return;
            }
        }
        selected = null;
        draw();
    }

    /**
     * Returns the {@code CardLocation} object represents the card at position {x, y}.
     *
     * @param x x-coordinate in canvas
     * @param y y-coordinate in canvas
     * @return the {@code CardLocation} object represents the card at position {x, y}
     */
    private CardLocation getCardLocation(double x, double y) {
        // if null, no location is selected
        // if not null but contains a null card, indicates a empty block is selected
        CardLocation newSelection = null;

        int[] mainCr = colRowOfMain(x, y);
        if (mainCr == NOT_IN_AREA_ARRAY) {
            int posInSpace = posOfSpace(x, y);
            if (posInSpace == NOT_IN_AREA) {
                int posInFinished = posOfFinished(x, y);
                if (posInFinished >= 0) {
                    newSelection = new CardLocation.FinishedLocation(game,
                            game.getFinishedArea()[posInFinished].getSurfaceCard(),
                            posInFinished);
                }
            } else if (posInSpace >= 0) {
                newSelection = new CardLocation.SpaceLocation(game,
                        game.getSpaceArea()[posInSpace], posInSpace);
            }
        } else if (mainCr.length == 1) {
            newSelection = new CardLocation.MainLocation(game, null, mainCr[0], game.getMainArea()[mainCr[0]].size());
        } else if (mainCr.length == 2) {
            if (mainCr[1] == -1) newSelection = new CardLocation.MainLocation(game, null, mainCr[0], mainCr[1]);
            else newSelection = new CardLocation.MainLocation(game,
                    game.getMainArea()[mainCr[0]].get(mainCr[1]),
                    mainCr[0],
                    mainCr[1]);
        }
        return newSelection;
    }

    private void drawInfoText() {
        double fontSize = 18.0;
        graphicsContext.setFill(TEXT);
        graphicsContext.setFont(new Font(fontSize));
        graphicsContext.fillText(secondsToString(timerSeconds), width / 2, timerY + fontSize * 0.5);  // timer
        graphicsContext.fillText(
                String.valueOf(game.getStepsCount()), width / 2, timerY + fontSize * 2.0);  // steps
        int[] curScore = game.getCurScore(timerSeconds);
        graphicsContext.fillText(
                SolitaireRecorder.DECIMAL_FORMAT.format(curScore[0]) + " - " +
                        SolitaireRecorder.DECIMAL_FORMAT.format(curScore[1]),
                width / 2,
                timerY + fontSize * 3.5);  // score
    }

    private void checkWin() {
        if (game.wining()) {
            finish();
            playEndingAnimation();
        }
    }

    private void finish() {
        finished = true;
        timer.cancel();
        recordResult();
        setButtonsStatus();
    }

    private void recordResult() {
        if (game == null || !started || recorded) return;
        int[] finalScore = game.getFinalScore(timerSeconds);
        SolitaireRecord record = new SolitaireRecord(
                game.getRules().getInitialFinishes(),
                timerSeconds,
                finalScore[0],
                game.getStepsCount(),
                new Date()
        );
        rankedScores = SolitaireRecorder.put(record);
        recorded = true;
    }

    private void setButtonsStatus() {
        editMenu.setDisable(finished || animatingCards != null);
        undoItem.setDisable(!game.hasMoveToUndo());
        autoFinishItem.setDisable(!game.canAutoFinish());
    }

    private void drawGrid() {
        graphicsContext.setFill(BACKGROUND);
        graphicsContext.fillRect(0, 0, width, canvas.getHeight());

        // finish area
        graphicsContext.setStroke(LINE);
        graphicsContext.setLineWidth(3.0);
        double x = mainSpacing - cardBorder;
        for (int i = 0; i < 4; ++i) {
            graphicsContext.strokeRect(x, areaSpacing, blockWidth, blockHeight);
            x += blockWidth + areaSpacing - cardBorder;
        }

        // space area
        x = width - blockWidth - mainSpacing;
        for (int i = 0; i < 4; ++i) {
            graphicsContext.strokeRect(x, areaSpacing, blockWidth, blockHeight);
            x -= blockWidth + areaSpacing - cardBorder;
        }

        // separate line
        graphicsContext.strokeLine(0, mainAreaY - areaSpacing, width, mainAreaY - areaSpacing);

        // main blocks
        x = mainSpacing - cardBorder;
        for (int i = 0; i < 8; ++i) {
            graphicsContext.strokeRect(x, mainAreaY, blockWidth, blockHeight);
            x += blockWidth + mainSpacing - cardBorder;
        }
    }

    private double requiredHeightOfMain(int rowCount) {
        return xyOfMain(0, rowCount - 1)[1] + cardFullHeight + cardGapHeight;
    }

    public double[] xyOfMain(int col, int row) {
        return new double[]{
                mainSpacing + mainCardOccupyWidth() * col,
                firstRowY + row * cardGapHeight
        };
    }

    public double[] xyOfSpace(int pos) {
        return new double[]{
                width - mainSpacing + cardBorder * 2 - (4 - pos) * areaCardOccupyWidth(),
                areaSpacing + cardBorder
        };
    }

    public double[] xyOfFinished(int pos) {
        return new double[]{
                mainSpacing + areaCardOccupyWidth() * pos,
                areaSpacing + cardBorder
        };
    }

    private double mainCardOccupyWidth() {
        return cardWidth + mainSpacing + cardBorder;
    }

    private double areaCardOccupyWidth() {
        return cardWidth + areaSpacing + cardBorder;
    }

    private double firstColX() {
        return xyOfFinished(0)[0];
    }

    private double firstSpaceAreaX() {
        return xyOfSpace(0)[0];
    }

    private int[] colRowOfMain(double x, double y) {
        double yInMain = y - firstRowY;
        if (yInMain < -cardBorder) return NOT_IN_AREA_ARRAY;  // not in main area
        int probCol = (int) ((x - firstColX()) / mainCardOccupyWidth());
        double xInCol = x - xyOfMain(probCol, 0)[0];
        if (xInCol >= mainCardOccupyWidth() - cardBorder) {  // the border of the next row
            probCol++;
            xInCol = xInCol - mainCardOccupyWidth();
        }
        if (xInCol < -cardBorder || xInCol >= cardWidth + cardBorder) return IN_AREA_NOT_AT_CARD_ARRAY;
        SolitaireDeck deck = game.getMainArea()[probCol];
        if (deck.isEmpty() && yInMain < cardFullHeight + cardBorder) {
            return new int[]{probCol, -1};  // refers to an empty column
        }

        int probRow = (int) (yInMain / cardGapHeight);
        if (probRow < deck.size()) return new int[]{probCol, probRow};
        if (yInMain - (deck.size() - 1) * cardGapHeight < cardFullHeight + cardBorder)
            return new int[]{probCol, deck.size() - 1};  // last card
        if (yInMain - deck.size() * cardGapHeight < cardFullHeight + cardBorder) {
            return new int[]{probCol};  // row after the last card
        }
        return IN_AREA_NOT_AT_CARD_ARRAY;
    }

    private int posOfSpace(double x, double y) {
        double relY = y - areaSpacing - cardBorder;
        if (relY < -cardBorder || relY > cardFullHeight + cardBorder) return NOT_IN_AREA;

        int probPos = (int) ((x - firstSpaceAreaX()) / areaCardOccupyWidth());
        double xInPos = x - xyOfSpace(probPos)[0];
        if (xInPos >= areaCardOccupyWidth() - cardBorder) {  // the border of the next row
            probPos++;
            xInPos = xInPos - areaCardOccupyWidth();
        }

//        System.out.println("space at " + xInPos + ", pos " + probPos);
        if (xInPos < -cardBorder) return NOT_IN_AREA;
        if (xInPos > cardWidth + cardBorder || probPos > 3) return IN_AREA_NOT_AT_CARD;
        else return probPos;
    }

    private int posOfFinished(double x, double y) {
        double relY = y - areaSpacing - cardBorder;
        if (relY < -cardBorder || relY > cardFullHeight + cardBorder) return NOT_IN_AREA;

        int probPos = (int) ((x - firstColX()) / areaCardOccupyWidth());
        double xInPos = x - xyOfFinished(probPos)[0];
        if (xInPos >= areaCardOccupyWidth() - cardBorder) {  // the border of the next row
            probPos++;
            xInPos = xInPos - areaCardOccupyWidth();
        }
//        System.out.println("finished at " + xInPos + ", pos " + probPos);
        if (xInPos < -cardBorder || xInPos > cardWidth + cardBorder) return IN_AREA_NOT_AT_CARD;
        if (probPos > 3) return NOT_IN_AREA;
        else return probPos;
    }

    private void drawCardsInMain() {
        int maxSize = 0;
        for (int col = 0; col < 8; ++col) {
            SolitaireDeck deck = game.getMainArea()[col];
            if (deck.size() > maxSize) maxSize = deck.size();
        }

        double targetHeight = requiredHeightOfMain(Math.max(prefRowsCount, maxSize));
        if (canvas.getHeight() != targetHeight) canvas.setHeight(targetHeight);

        for (int col = 0; col < 8; ++col) {
            SolitaireDeck deck = game.getMainArea()[col];
            for (int row = 0; row < deck.size(); ++row) {
                double[] xy = xyOfMain(col, row);
                drawFixedCard(deck.get(row), xy[0], xy[1]);
            }
        }
    }

    private void drawCardsInSpace() {
        for (int i = 0; i < 4; ++i) {
            Card surface = game.getSpaceArea()[i];
            if (surface != null) {
                double[] xy = xyOfSpace(i);
                drawFixedCard(surface, xy[0], xy[1]);
            }
        }
    }

    private void drawCardsInFinished() {
        for (int i = 0; i < 4; ++i) {
            SolitaireDeck deck = game.getFinishedArea()[i];
            Card surface = deck.getSurfaceCard();
            if (surface != null) {
                double[] xy = xyOfFinished(i);
                if (deck.size() > 1) {
                    Card below = deck.get(deck.size() - 2);
                    drawFixedCard(below, xy[0], xy[1]);  // 确保拖动时第二张牌不为空
                }
                drawFixedCard(surface, xy[0], xy[1]);
            }
        }
    }

    private void drawAnimatingCards() {
        if (animatingCards != null) {
            if (animatingCards.cards instanceof DragFromMain) {
                DragFromMain drag = (DragFromMain) animatingCards.cards;
                double y = animatingCards.y;
                double x = animatingCards.x;
                for (Card card : drag.dragged) {
                    drawCard(card, x, y);
                    y += cardGapHeight;
                }
            } else if (animatingCards.cards instanceof SingleCardDrag) {
                SingleCardDrag drag = (SingleCardDrag) animatingCards.cards;
                drawCard(drag.srcLocation.getCard(), animatingCards.x, animatingCards.y);
            }
        }
    }

    private void drawDraggingCards() {
        if (draggedCards != null) {
            if (draggedCards instanceof DragFromMain) {
                DragFromMain drag = (DragFromMain) draggedCards;
                double y = curMouseY - drag.mouseYFromCardTop;
                double x = curMouseX - drag.mouseXFromCardLeft;
                for (Card card : drag.dragged) {
                    drawCard(card, x, y);
                    y += cardGapHeight;
                }
            } else if (draggedCards instanceof SingleCardDrag) {
                SingleCardDrag drag = (SingleCardDrag) draggedCards;
                double y = curMouseY - drag.mouseYFromCardTop;
                double x = curMouseX - drag.mouseXFromCardLeft;
                drawCard(drag.srcLocation.getCard(), x, y);
            }
        }
    }

    private void drawHints() {
        if (hint != null) {
            double[] xy = hint.getDstLocation().reloadLocation(row -> row == -1 ? 0 : row).cardLeftXY(this);
            drawHighlight(xy[0], xy[1], HINT);
        }
    }

    private void clearHint() {
        if (hint != null) {
            hint = null;
            draw();
        }
    }

    private void drawHighlight(double x, double y, Paint paint) {
        graphicsContext.setStroke(paint);
        graphicsContext.setLineWidth(5.0);
        graphicsContext.strokeRoundRect(x + 4.0, y + 4.0,
                cardWidth - 8.0, cardFullHeight - 8.0,
                10.0, 10.0);
    }

    private void drawCard(Card card, double x, double y) {
        graphicsContext.setStroke(CARD_LINE);
        graphicsContext.setLineWidth(3.0);

        graphicsContext.strokeRoundRect(x, y, cardWidth, cardFullHeight, 10.0, 10.0);
        graphicsContext.setFill(CARD);
        graphicsContext.fillRoundRect(x, y, cardWidth, cardFullHeight, 10.0, 10.0);
        if (selected != null && selected.isSelected(card)) {
            drawHighlight(x, y, HIGHLIGHT);
        }

        graphicsContext.setFill(card.isBlack() ? BLACK : RED);
        graphicsContext.setFont(Font.font(24.0));
        graphicsContext.fillText(card.getShowString(), x + cardWidth / 3, y + 25.0);
        graphicsContext.setFont(Font.font(40.0));
        graphicsContext.fillText(String.valueOf(card.getSuitRep()), x + cardWidth / 2, y + cardFullHeight / 1.6);
    }

    private void drawFixedCard(Card card, double x, double y) {
        if (draggedCards != null && draggedCards.cardIsDragging(card)) return;
        if (animatingCards != null && animatingCards.cards.cardIsDragging(card)) return;

        drawCard(card, x, y);
    }

    private void draw() {
        drawGrid();
        drawInfoText();
        drawCardsInMain();
        drawCardsInFinished();
        drawCardsInSpace();
        drawDraggingCards();
        drawAnimatingCards();
        drawHints();
    }

    private void drawWin() {
        drawGrid();
        drawInfoText();

        double x = width / 2;

        graphicsContext.setFont(new Font(28));
        graphicsContext.setFill(TEXT);
        graphicsContext.fillText(bundle.getString("winMessage"), x, 200.0);

        int[] finalScore = game.getFinalScore(timerSeconds);
        String scoreString =
                SolitaireRecorder.DECIMAL_FORMAT.format(finalScore[0]) +
                        " = " +
                        SolitaireRecorder.DECIMAL_FORMAT.format(finalScore[1]) +
                        " + " +
                        SolitaireRecorder.DECIMAL_FORMAT.format(finalScore[2]) +
                        " - " +
                        SolitaireRecorder.DECIMAL_FORMAT.format(finalScore[3]);
        String toDraw = String.format(bundle.getString("winMsgFmt"),
                secondsToString(timerSeconds),
                game.getStepsCount(),
                scoreString);
        graphicsContext.setFont(new Font(20));
        graphicsContext.fillText(toDraw, x, 250.0);

        double leftX = width * 0.38;
        double rightX = width * 0.62;
        double startY = 330.0;
        graphicsContext.fillText(bundle.getString("score"), leftX, startY);
        graphicsContext.fillText(bundle.getString("rank"), x, startY);
        graphicsContext.fillText(bundle.getString("best"), rightX, startY);
        graphicsContext.fillText(String.valueOf(finalScore[0]), leftX, startY + 30.0);
        graphicsContext.fillText(String.valueOf(rankedScores.scoreRank + 1), x, startY + 30.0);
        graphicsContext.fillText(
                String.valueOf(rankedScores.stepsBest == null ? "-" : rankedScores.scoreBest.score),
                rightX, startY + 30.0);
        graphicsContext.fillText(String.valueOf(timerSeconds), leftX, startY + 60.0);
        graphicsContext.fillText(String.valueOf(rankedScores.timeRank + 1), x, startY + 60.0);
        graphicsContext.fillText(
                String.valueOf(rankedScores.timeBest == null ? "-" : rankedScores.timeBest.seconds),
                rightX, startY + 60.0);
        graphicsContext.fillText(String.valueOf(game.getStepsCount()), leftX, startY + 90.0);
        graphicsContext.fillText(String.valueOf(rankedScores.stepsRank + 1), x, startY + 90.0);
        graphicsContext.fillText(
                String.valueOf(rankedScores.stepsBest == null ? "-" : rankedScores.stepsBest.steps),
                rightX, startY + 90.0);
    }

    private void showWinMsg() {
        drawWin();
    }

    private void startNewGame() {
        game = new SolitaireGame(
                new SolitaireRules.Builder()
                        .initialFinishes(SolitaireRules.loadInitialFinishes())
                        .strict(!Configs.getBoolean("casual"))
                        .build());
        setStartGameUi();
    }

    private void restartGame() {
        game.restartGame();
        setStartGameUi();
    }

    private void setStartGameUi() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timeCounter = null;
        finished = false;
        started = false;
        recorded = false;
        timerSeconds = 0;
        selected = null;
        draggedCards = null;

        draw();
    }

    private void setupCanvas() {
        graphicsContext.setTextAlign(TextAlignment.CENTER);
        canvas.setWidth(width);
        canvas.setHeight(prefHeight);
        basePane.setPrefWidth(width + 4.0);
        basePane.setPrefHeight(prefHeight + 4.0);

        canvas.setOnMouseClicked(this::onCanvasClicked);
        canvas.setOnMouseDragged(this::onCanvasDragged);
        canvas.setOnDragDetected(this::onCanvasDragStarted);
    }

    private abstract static class DraggedCards {
        protected final double mouseXFromCardLeft;
        protected final double mouseYFromCardTop;
        protected final CardLocation srcLocation;

        DraggedCards(CardLocation srcLocation, double mouseXFromCardLeft, double mouseYFromCardTop) {
            this.srcLocation = srcLocation;
            this.mouseXFromCardLeft = mouseXFromCardLeft;
            this.mouseYFromCardTop = mouseYFromCardTop;
        }

        abstract boolean cardIsDragging(Card card);
    }

    private static class DragFromMain extends DraggedCards {
        private final SolitaireDeck dragged;

        DragFromMain(CardLocation srcLocation, double mouseXFromCardLeft, double mouseYFromCardTop,
                     SolitaireDeck dragged) {
            super(srcLocation, mouseXFromCardLeft, mouseYFromCardTop);
            this.dragged = dragged;
        }

        @Override
        boolean cardIsDragging(Card card) {
            return dragged.contains(card);
        }
    }

    private static abstract class SingleCardDrag extends DraggedCards {

        SingleCardDrag(CardLocation srcLocation, double mouseXFromCardLeft, double mouseYFromCardTop) {
            super(srcLocation, mouseXFromCardLeft, mouseYFromCardTop);  // srcLocation has a non-null card
        }

        @Override
        boolean cardIsDragging(Card card) {
            return card.equals(srcLocation.getCard());
        }
    }

    private static class DragFromSpace extends SingleCardDrag {

        DragFromSpace(CardLocation srcLocation, double mouseXFromCardLeft, double mouseYFromCardTop) {
            super(srcLocation, mouseXFromCardLeft, mouseYFromCardTop);
        }
    }

    private static class DragFromFinished extends SingleCardDrag {

        DragFromFinished(CardLocation srcLocation, double mouseXFromCardLeft, double mouseYFromCardTop) {
            super(srcLocation, mouseXFromCardLeft, mouseYFromCardTop);  // srcLocation has a non-null card
        }
    }

    private static class InvalidDrag extends DraggedCards {

        InvalidDrag() {
            super(null, 0.0, 0.0);
        }

        @Override
        boolean cardIsDragging(Card card) {
            return false;
        }

        @Override
        public String toString() {
            return "Invalid drag";
        }
    }

    private static class AnimatingCards {
        final DraggedCards cards;
        double x, y;

        AnimatingCards(DraggedCards cards, double initX, double initY) {
            this.cards = cards;
            this.x = initX;
            this.y = initY;
        }

        @Override
        public String toString() {
            return "AnimatingCards{" +
                    "cards=" + cards +
                    ", x=" + x +
                    ", y=" + y +
                    '}';
        }
    }

    private class EndAnimation implements EventHandler<ActionEvent> {

        private final Timeline timeline;
        private final SolitaireDeck[] decks = new SolitaireDeck[4];
        private int count;
        private FlyingCard flyingCard;

        EndAnimation(Timeline timeline) {
            this.timeline = timeline;
            for (int i = 0; i < 4; ++i) {
                decks[i] = new SolitaireDeck(game.getFinishedArea()[i]);
            }
            generateNextCard();
        }

        @Override
        public void handle(ActionEvent event) {
            if (flyingCard.x < -cardWidth || flyingCard.x >= width) {
                generateNextCard();
            } else {
                drawCard(flyingCard.card, flyingCard.x, flyingCard.y);
                flyingCard.refreshPosition();
            }
        }

        private void generateNextCard() {
            double[] initPos = xyOfFinished(count % 4);
            Card card = decks[count++ % 4].removeSurfaceCardIfNotEmpty();
            if (card == null) {
                terminateWiningAnimation();
            }
            flyingCard = new FlyingCard(card, initPos[0], initPos[1]);
        }
    }

    private class FlyingCard {
        private static final double yAcc = 320.0;
        private final Card card;
        private final double bounceRate = 0.75 + Math.random() / 10.0;
        private double x, y;
        private double xSpeed;  // per second
        private double ySpeed;  // per second

        FlyingCard(Card card, double startX, double startY) {
            this.card = card;
            this.x = startX;
            this.y = startY;
            this.xSpeed = (Math.random() - 0.4) * 1000;  // (-400, 600)
            if (Math.abs(xSpeed) < 75) {
                if (xSpeed < 0) xSpeed = -75.0;
                else xSpeed = 75.0;
            }
        }

        private void refreshPosition() {
            double frameRate = 1000.0 / endAnimationFrameTime;
            if (y + cardFullHeight > prefHeight && ySpeed >= 0) {
                ySpeed = -ySpeed * bounceRate;
            }
            x += xSpeed / frameRate;
            y += ySpeed / frameRate;
            ySpeed += yAcc / frameRate;
        }
    }

    private class MoveAnimation implements EventHandler<ActionEvent> {

        private final double xSpeed;
        private final double ySpeed;

        MoveAnimation(CardLocation srcLocation, CardLocation dstLocation) {
            double[] srcXY = getXY(srcLocation);
            double[] dstXY = getXY(dstLocation);

            double srcX = srcXY[0];
            double srcY = srcXY[1];
            double dstX = dstXY[0];
            double dstY = dstXY[1];

            int frameCount = (int) (animationDuration / frameTime);
            xSpeed = (dstX - srcX) / frameCount;
            ySpeed = (dstY - srcY) / frameCount;

            animatingCards = new AnimatingCards(
                    createDraggingCards(srcLocation, 0.0, 0.0),
                    srcX,
                    srcY
            );
            setButtonsStatus();
        }

        private double[] getXY(CardLocation location) {
            if (location instanceof CardLocation.MainLocation) {
                CardLocation.MainLocation mainLocation = (CardLocation.MainLocation) location;
                return xyOfMain(mainLocation.getCol(), mainLocation.getRow());
            } else if (location instanceof CardLocation.SpaceLocation) {
                return xyOfSpace(((CardLocation.SpaceLocation) location).getPos());
            } else if (location instanceof CardLocation.FinishedLocation) {
                return xyOfFinished(((CardLocation.FinishedLocation) location).getPos());
            } else {
                throw new SolitaireException("Unexpected location.");
            }
        }

        @Override
        public void handle(ActionEvent event) {
            animatingCards.x += xSpeed;
            animatingCards.y += ySpeed;
            draw();
        }
    }

    private class GameTimerTask extends TimerTask {

        @Override
        public void run() {
            timerSeconds++;
            draw();
        }
    }
}
