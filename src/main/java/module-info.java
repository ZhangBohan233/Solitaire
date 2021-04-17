module Solitaire {
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.controls;
    requires org.json;

    exports trashsoftware.solitaire;
    exports trashsoftware.solitaire.core.solitaireGame;
    exports trashsoftware.solitaire.fxml.controls;

//    opens trashsoftware.solitaire.core;
    opens trashsoftware.solitaire.fxml.controls;
}