package trashsoftware.solitaire.fxml.controls;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import trashsoftware.solitaire.Main;
import trashsoftware.solitaire.core.solitaireGame.*;
import trashsoftware.solitaire.util.Configs;

import java.io.IOException;
import java.net.URL;
import java.util.*;

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
    AnchorPane basePane;
    @FXML
    MenuItem undoItem, autoFinishItem;
    private double height = cardFullHeight * 6;
    private double frameTime = 20.0;
    private double firstRowY;
    private GraphicsContext graphicsContext;
    private Stage stage;

    private SolitaireGame game;
    private CardLocation selected;
    private boolean started = false;
    private boolean finished = false;

    private DraggedCards draggedCards;
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
        game.undo();
        setButtonsStatus();
        draw();
    }

    @FXML
    void autoFinishAction() {
        while (!game.wining()) {

        }
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

        double x = event.getX();
        double y = event.getY();
        CardLocation location = getCardLocation(x, y);
        if (location == null || location.card == null) {
            draggedCards = INVALID_DRAG;
            return;
        }

        if (location instanceof MainLocation) {
            MainLocation mainLocation = (MainLocation) location;
            double[] cardTopLeft = xyOfMain(mainLocation.col, mainLocation.row);
            SolitaireDeck deck = game.getMainArea()[mainLocation.col];
            if (deck.draggable(mainLocation.row)) {
                SolitaireDeck dragging = new SolitaireDeck(deck.subList(mainLocation.row, deck.size()));
                draggedCards = new DragFromMain(mainLocation,
                        x - cardTopLeft[0],
                        y - cardTopLeft[1],
                        dragging);
                stage.getScene().setCursor(Cursor.CLOSED_HAND);
            } else draggedCards = INVALID_DRAG;
        } else if (location instanceof SpaceLocation) {
            SpaceLocation spaceLocation = (SpaceLocation) location;
            double[] cardTopLeft = xyOfSpace(spaceLocation.pos);
            draggedCards = new DragFromSpace(spaceLocation,
                    x - cardTopLeft[0],
                    y - cardTopLeft[1]);
            stage.getScene().setCursor(Cursor.CLOSED_HAND);
        } else if (location instanceof FinishedLocation) {
            FinishedLocation finishedLocation = (FinishedLocation) location;
            double[] cardTopLeft = xyOfFinished(finishedLocation.pos);
            draggedCards = new DragFromFinished(finishedLocation,
                    x - cardTopLeft[0],
                    y - cardTopLeft[1]);
            stage.getScene().setCursor(Cursor.CLOSED_HAND);
        } else {
            draggedCards = INVALID_DRAG;
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
        if (game.move(move)) {
            System.out.println("Animated move");
            setButtonsStatus();
            return true;
        }
        return false;
    }

    private void singleClick(double x, double y) {
        CardLocation newSelection = getCardLocation(x, y);

        if (selected == null) {
            // newly selected
            if (newSelection != null && newSelection.card != null) {
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
                } else {
                    selected = newSelection;
                    draw();
                    return;
                }
            }
            // clear selection
            selected = null;
            draw();
        }
    }

    private void doubleClick(double x, double y) {
        CardLocation newSelection = getCardLocation(x, y);

        if (newSelection != null && newSelection.card != null) {
            int pos = newSelection.card.getSuit();
            FinishedLocation fl = new FinishedLocation(game.getFinishedArea()[pos].getSurfaceCard(), pos);
            SolitaireMove move = newSelection.createMove(fl);
            if (animatedMoveAction(move)) {
                checkWin();
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
                    newSelection = new FinishedLocation(game.getFinishedArea()[posInFinished].getSurfaceCard(),
                            posInFinished);
                }
            } else if (posInSpace >= 0) {
                newSelection = new SpaceLocation(game.getSpaceArea()[posInSpace], posInSpace);
            }
        } else if (mainCr.length == 1) {
            newSelection = new MainLocation(null, mainCr[0], game.getMainArea()[mainCr[0]].size());
        } else if (mainCr.length == 2) {
            if (mainCr[1] == -1) newSelection = new MainLocation(null, mainCr[0], mainCr[1]);
            else newSelection = new MainLocation(game.getMainArea()[mainCr[0]].get(mainCr[1]), mainCr[0], mainCr[1]);
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
            System.out.println("Wining!");
            finish();
        }
    }

    private void finish() {
        finished = true;
        timer.cancel();
        setButtonsStatus();
    }

    private void setButtonsStatus() {
        undoItem.setDisable(finished || !game.hasMoveToUndo());
        autoFinishItem.setDisable(finished || !game.canAutoFinish());
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

    private double[] xyOfMain(int col, int row) {
        return new double[]{
                mainSpacing + mainCardOccupyWidth() * col,
                firstRowY + row * cardGapHeight
        };
    }

    private double[] xyOfSpace(int pos) {
        return new double[]{
                width - mainSpacing + cardBorder * 2 - (4 - pos) * areaCardOccupyWidth(),
                areaSpacing + cardBorder
        };
    }

    private double[] xyOfFinished(int pos) {
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
                drawCard(drag.srcLocation.card, x, y);
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

        drawCard(card, x, y);
    }

    private void draw() {
        drawGrid();
        drawInfoText();
        drawCardsInMain();
        drawCardsInFinished();
        drawCardsInSpace();
        drawDraggingCards();
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


    private abstract static class CardLocation {
        final Card card;  // nullable

        CardLocation(Card card) {
            this.card = card;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CardLocation that = (CardLocation) o;

            return Objects.equals(card, that.card);
        }

        @Override
        public int hashCode() {
            return card != null ? card.hashCode() : 0;
        }

        abstract SolitaireMove createMove(CardLocation dstLocation);

        abstract boolean isSelected(Card card);
    }

    private abstract static class SingleCardLocation extends CardLocation {
        final int pos;

        SingleCardLocation(Card card, int pos) {
            super(card);

            this.pos = pos;
        }

        @Override
        boolean isSelected(Card card) {
            return card == this.card;
        }
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
            return card.equals(srcLocation.card);
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
    }

    private class MainLocation extends CardLocation {
        private final int col, row;
        private final Set<Card> highlightedCards = new TreeSet<>();

        MainLocation(Card card, int col, int row) {
            super(card);

            this.col = col;
            this.row = row;

            if (card != null) {
                highlightedCards.add(card);
                SolitaireDeck deck = game.getMainArea()[col];
                if (deck.draggable(row)) {
                    for (int i = row + 1; i < deck.size(); ++i) {
                        highlightedCards.add(deck.get(i));
                    }
                }
            }
        }

        @Override
        SolitaireMove createMove(CardLocation dstLocation) {
            if (dstLocation instanceof MainLocation) {
                return new SolitaireMove.MainToMain(game, col, row, ((MainLocation) dstLocation).col);
            } else if (dstLocation instanceof SpaceLocation) {
                return new SolitaireMove.MainToSpace(game, col, row, ((SpaceLocation) dstLocation).pos);
            } else if (dstLocation instanceof FinishedLocation) {
                return new SolitaireMove.MainToFinished(game, col, row, ((FinishedLocation) dstLocation).pos);
            } else {
                throw new SolitaireException();
            }
        }

        @Override
        boolean isSelected(Card card) {
            return highlightedCards.contains(card);
        }

        @Override
        public String toString() {
            return "MainLocation{" +
                    "col=" + col +
                    ", row=" + row +
                    '}';
        }
    }

    private class SpaceLocation extends SingleCardLocation {
        SpaceLocation(Card card, int pos) {
            super(card, pos);
        }

        @Override
        SolitaireMove createMove(CardLocation dstLocation) {
            if (dstLocation instanceof MainLocation) {
                return new SolitaireMove.SpaceToMain(game, pos, ((MainLocation) dstLocation).col);
            } else if (dstLocation instanceof SpaceLocation) {
                return new SolitaireMove.SpaceToSpace(game, pos, ((SpaceLocation) dstLocation).pos);
            } else if (dstLocation instanceof FinishedLocation) {
                return new SolitaireMove.SpaceToFinished(game, pos, ((FinishedLocation) dstLocation).pos);
            } else {
                throw new SolitaireException();
            }
        }

        @Override
        public String toString() {
            return "SpaceLocation{" +
                    "pos=" + pos +
                    '}';
        }
    }

    private class FinishedLocation extends SingleCardLocation {
        FinishedLocation(Card card, int pos) {
            super(card, pos);
        }

        @Override
        SolitaireMove createMove(CardLocation dstLocation) {
            if (dstLocation instanceof MainLocation) {
                return new SolitaireMove.FinishedToMain(game, pos, ((MainLocation) dstLocation).col);
            } else if (dstLocation instanceof SpaceLocation) {
                return new SolitaireMove.FinishedToSpace(game, pos, ((SpaceLocation) dstLocation).pos);
            } else if (dstLocation instanceof FinishedLocation) {
                return new SolitaireMove.FinishedToFinished(game);
            } else {
                throw new SolitaireException();
            }
        }

        @Override
        public String toString() {
            return "FinishedLocation{" +
                    "pos=" + pos +
                    '}';
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
