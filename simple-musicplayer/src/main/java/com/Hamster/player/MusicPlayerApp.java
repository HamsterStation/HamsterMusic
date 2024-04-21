package com.Hamster.player;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MusicPlayerApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/player.fxml"));//要有相对路径
        stage.setScene(new Scene(root,null));
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.getIcons().add(new Image("/image/bc7510358f9badeec66157c90d7df9e.jpg"));
        stage.setFullScreenExitHint("");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
