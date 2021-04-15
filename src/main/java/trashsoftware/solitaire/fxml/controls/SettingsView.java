package trashsoftware.solitaire.fxml.controls;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import trashsoftware.solitaire.util.Configs;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsView implements Initializable {
    @FXML
    ComboBox<Integer> initFinishesBox;
    @FXML
    CheckBox casualModeBox;

    private Stage stage;
    private GameView parent;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        addListeners();
    }

    public void setStage(Stage stage, GameView parent) {
        this.stage = stage;
        this.parent = parent;
    }

    @FXML
    void onConfirm() {
        Configs.writeConfig("initFinishes", initFinishesBox.getSelectionModel().getSelectedItem());
        Configs.writeConfig("casual",
                String.valueOf(casualModeBox.isSelected()));
        parent.newGameAction();

        stage.close();
    }

    @FXML
    void onCancel() {
        stage.close();
    }

    private void addListeners() {
        initFinishesBox.getItems().addAll(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        int curDifficulty = Configs.getInt("initFinishes", 0);
        if (curDifficulty < 0 || curDifficulty >= initFinishesBox.getItems().size()) {
            initFinishesBox.getSelectionModel().select(0);
        } else {
            initFinishesBox.getSelectionModel().select(curDifficulty);
        }
        boolean isCasual = Configs.getBoolean("casual");
        casualModeBox.setSelected(isCasual);
    }
}
