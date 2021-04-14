package trashsoftware.solitaire.fxml.controls;

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
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import trashsoftware.solitaire.Main;
import trashsoftware.solitaire.core.solitaireGame.*;
import trashsoftware.solitaire.util.Configs;

import java.io.IOException;
import java.net.URL;
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
    private final double timerY = 20.0;
    @FXML
    Canvas canvas;
    @FXML
    MenuItem undoItem, autoFinishItem;
    private double height = cardFullHeight * 6;
    private double animationDuration = 500.0;
    private double frameTime = 25.0;
    private double firstRowY;
    private GraphicsContext graphicsContext;
    private Stage stage;

    private SolitaireGame game;
    private CardLocation selected;
    private boolean started = false;
    private boolean finished = false;

    private DraggedCards draggedCards;
    private AnimatingCards animatingCards;
    private double curMouseX, curMouseY;
    private long lastDragTime;

    private Timer timer;
    private String timerText;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
        if (game.hasMoveToUndo()) {  // double check
            SolitaireMove move = game.getLastDoneMove();

            CardLocation dst = move.getDstLocation();
            CardLocation realDstLocation;
            if (dst instanceof CardLocation.MainLocation) {
                // in a move to main, the actual dst location is the next row
                CardLocation.MainLocation mainLocation = (CardLocation.MainLocation) dst;
                realDstLocation = new CardLocation.MainLocation(
                        game,
                        mainLocation.getCard(),
                        mainLocation.getCol(),
                        mainLocation.getRow() + 1);
            } else realDstLocation = dst.reloadLocation();

            animation(realDstLocation, move.getSrcLocation(), e -> {
                game.undo();
                animatingCards = null;
                setButtonsStatus();
                draw();
            });
        }
    }

    @FXML
    void autoFinishAction() {
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

    }

    void onCanvasClicked(MouseEvent event) {
//        System.out.println("click");
        if (!started) {
            started = true;
            timer.scheduleAtFixedRate(new GameTimerTask(), 1000, 1000);
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
//        System.out.println("drag released");
        CardLocation dstLocation = getCardLocation(event.getX(), event.getY());
//        System.out.println("src at " + dragging.srcLocation);
//        System.out.println("dst at " + dstLocation);
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
//        System.out.println(event.getX() + ", " + event.getY());
        long cur = System.currentTimeMillis();
        long dragTime = cur - lastDragTime;
        if (dragTime >= frameTime) {
            curMouseX = event.getX();
            curMouseY = event.getY();
            if (curMouseX < 0 || curMouseY < 0 || curMouseX >= width || curMouseY >= height) {
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
                realDstLocation = new CardLocation.MainLocation(
                        game,
                        mainLocation.getCard(),
                        mainLocation.getCol(),
                        mainLocation.getRow() + 1);
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
        graphicsContext.fillText(timerText, width / 2, timerY + fontSize / 2);
        graphicsContext.fillText(String.valueOf(game.getStepsCount()), width / 2, timerY + fontSize * 2);
    }

    private void checkWin() {
        if (game.wining()) {
            finish();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(Main.getBundle().getString("winMessage"));
            alert.setContentText(
                    String.format(Main.getBundle().getString("winMsgFmt"), timerText, game.getStepsCount()));
            alert.initOwner(stage);
            alert.show();
        }
    }

    private void finish() {
        finished = true;
        timer.cancel();
        setButtonsStatus();
    }

    private void setButtonsStatus() {
        undoItem.setDisable(finished
                || !game.hasMoveToUndo()
                || animatingCards != null
        );
        autoFinishItem.setDisable(finished
                || !game.canAutoFinish()
                || animatingCards != null
        );
    }

    private void drawGrid() {
        graphicsContext.setFill(BACKGROUND);
        graphicsContext.fillRect(0, 0, width, height);

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

        // separate
        graphicsContext.strokeLine(0, areaSpacing * 2 + blockHeight, width, areaSpacing * 2 + blockHeight);

        // main blocks
        double y = areaSpacing * 3 + blockHeight;
        firstRowY = y + cardBorder;
        x = mainSpacing - cardBorder;
        for (int i = 0; i < 8; ++i) {
            graphicsContext.strokeRect(x, y, blockWidth, blockHeight);
            x += blockWidth + mainSpacing - cardBorder;
        }
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

    private void drawCard(Card card, double x, double y) {
        graphicsContext.setStroke(CARD_LINE);
        graphicsContext.setLineWidth(3.0);
//        Rectangle rectangle = new Rectangle(cardWidth, cardFullHeight, RED);
//        rectangle.setX(55.0);
//        rectangle.setArcHeight(10.0);
//        rectangle.setArcWidth(10.0);
//        basePane.getChildren().clear();
//        basePane.getChildren().add(rectangle);

        graphicsContext.strokeRoundRect(x, y, cardWidth, cardFullHeight, 10.0, 10.0);
        graphicsContext.setFill(CARD);
        graphicsContext.fillRoundRect(x, y, cardWidth, cardFullHeight, 10.0, 10.0);
        if (selected != null && selected.isSelected(card)) {
            graphicsContext.setStroke(HIGHLIGHT);
            graphicsContext.setLineWidth(5.0);
            graphicsContext.strokeRoundRect(x + 4.0, y + 4.0,
                    cardWidth - 8.0, cardFullHeight - 8.0,
                    10.0, 10.0);
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
    }

    private void startNewGame() {
        game = new SolitaireGame(
                new SolitaireRules.Builder()
                        .initialFinishes(SolitaireRules.loadInitialFinishes())
                        .strict(!Configs.getBoolean("casual"))
                        .build());
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        finished = false;
        started = false;
        timerText = "00:00";
        selected = null;
        draggedCards = null;

        draw();
    }

    private void setupCanvas() {
        graphicsContext.setTextAlign(TextAlignment.CENTER);
        canvas.setWidth(width);
        canvas.setHeight(height);

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
        private int seconds;

        @Override
        public void run() {
            seconds++;
            timerText = secondsToString();
            draw();
        }

        private String secondsToString() {
            int minutes = seconds / 60;
            int sec = seconds % 60;
            return String.format("%02d:%02d", minutes, sec);
        }
    }
}
