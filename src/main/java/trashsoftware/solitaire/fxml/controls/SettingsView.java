package trashsoftware.solitaire.fxml.controls;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import trashsoftware.solitaire.util.Configs;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsView implements Initializable {
    @FXML
    ToggleGroup difficultyGroup;
    @FXML
    RadioButton diff1Button, diff2Button, diff3Button;
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
        Configs.writeConfig("difficultyLevel",
                String.valueOf(difficultyGroup.getToggles().indexOf(difficultyGroup.getSelectedToggle())));
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
        int curDifficulty = Configs.getInt("difficultyLevel", 2);
        if (curDifficulty < 0 || curDifficulty >= difficultyGroup.getToggles().size()) {
            difficultyGroup.selectToggle(diff3Button);
        } else {
            difficultyGroup.selectToggle(difficultyGroup.getToggles().get(curDifficulty));
        }
        boolean isCasual = Configs.getBoolean("casual");
        casualModeBox.setSelected(isCasual);
    }
}
