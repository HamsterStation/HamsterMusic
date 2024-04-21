package com.Hamster.player;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;

import com.leewyatt.rxcontrols.controls.RXAudioSpectrum;
import com.leewyatt.rxcontrols.controls.RXLrcView;
import com.leewyatt.rxcontrols.controls.RXMediaProgressBar;
import com.leewyatt.rxcontrols.pojo.LrcDoc;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

public class PlayerController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private AnchorPane drawerPane;

    @FXML
    private BorderPane sliderPane;

    @FXML
    private StackPane soundBtn;
    @FXML
    private ListView<File> listView;
    @FXML
    private RXAudioSpectrum audioSpectrum;

    @FXML
    private RXLrcView lrcView;

    @FXML
    private ToggleButton playButton;

    @FXML
    private RXMediaProgressBar progressBar;

    @FXML
    private Label timeLabel;

    private Timeline showAnim ;
    private Timeline hideAnim ;
    private ContextMenu soundPopup;
    private MediaPlayer player;
    private Slider soundSlider;


    @FXML
    void initialize() {
        initAnim();
        initSound();
        initListView();
    }

    private void initListView() {
        listView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<File> call(ListView<File> fileListView) {
                return new ListCell<>(){
                    @Override
                    protected void updateItem(File item, boolean empty) {
                        super.updateItem(item, empty);
                        if(item == null || empty){
                            setGraphic(null);
                            return;
                        }
                        BorderPane pane = new BorderPane();
                        String name = item.getName().substring(0,item.getName().length()-4);
                        Label label = new Label(name);
                        BorderPane.setAlignment(label, Pos.CENTER_LEFT);//让歌曲名字在左边
                        pane.setLeft(label); // 将标签设置到BorderPane的左侧
                        setGraphic(pane); // 设置单元格的图形
                    }
                };
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((ob,oldFile,newFile) ->{
            if(newFile != null){
                if(player != null){
                    player.stop();//确保每次一首歌
                    player.dispose();
                    player = null;
                }
                player = new MediaPlayer(new Media(newFile.toURI().toString()));
                player.setVolume(soundSlider.getValue()/100);//设定音量控制
                //设置歌词
                String lrcPath = newFile.getAbsolutePath().replaceAll("mp3$","lrc");
                File lrcFile = new File(lrcPath);
                if(lrcFile.exists()){
                    try {
                        byte[] bytes = Files.readAllBytes(lrcFile.toPath());
                        lrcView.setLrcDoc(LrcDoc.parseLrcDoc(new String(bytes,EncodingDetect.detect(bytes))));//确保读出来的歌词不会乱码
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                //设置歌词进度
                lrcView.currentTimeProperty().bind(player.currentTimeProperty());

                //设置频谱可视化
                player.setAudioSpectrumListener(new AudioSpectrumListener() {
                    @Override
                    public void spectrumDataUpdate(double v, double v1, float[] floats, float[] floats1) {
                        audioSpectrum.setMagnitudes(floats);
                    }
                });
                //设置进度条总时长 和歌曲总时间绑定
                progressBar.durationProperty().bind(player.getMedia().durationProperty());
                player.currentTimeProperty().addListener((ob1, ov1, nv1) ->{
                    progressBar.setCurrentTime(nv1);
                } );

                //进度条的调整
                progressBar.setOnMouseClicked(event->{
                    player.seek(progressBar.getCurrentTime());
                });
                progressBar.setOnMouseDragged(event->{
                    player.seek(progressBar.getCurrentTime());
                });
                player.setOnEndOfMedia(this::PlayNextAction);
                playButton.setSelected(true);

                player.play();
            }
        });



    }

    private void initSound() {
        soundPopup = new ContextMenu(new SeparatorMenuItem());//传个分割符 能确保show出来
        Parent soundRoot = null;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/sound.fxml"));
        try {
            soundRoot = fxmlLoader.load();
            //这里通过root查找namespace的方式找到他俩的id，将他俩的value绑定
           ObservableMap<String,Object> namespace =fxmlLoader.getNamespace();
           soundSlider = (Slider) namespace.get("soundSlider");
           Label soundNum = (Label) namespace.get("soundNum");
           soundNum.textProperty().bind(soundSlider.valueProperty().asString("%.0f%%"));
            //拖动改变音量
           soundSlider.valueProperty().addListener((ob,ov,nv)-> {
                if(player != null){
                    player.setVolume(nv.doubleValue()/100);
                }
           });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        soundPopup.getScene().setRoot(soundRoot);
    }

    private void initAnim() {
        showAnim = new Timeline(new KeyFrame(Duration.millis(300),new KeyValue(sliderPane.translateXProperty(),0)));
        hideAnim = new Timeline(new KeyFrame(Duration.millis(300),new KeyValue(sliderPane.translateXProperty(),300)));
        hideAnim.setOnFinished(event-> drawerPane.setVisible(false));
    }

    private Stage findStage() {
        return (Stage) drawerPane.getScene().getWindow();
    }

    @FXML
    void onHideSliderPaneAction(MouseEvent event) {
        showAnim.stop();
        hideAnim.play();
    }

    @FXML
    void onShowSliderPaneAction(MouseEvent event) {
        drawerPane.setVisible(true);
        hideAnim.stop();
        showAnim.play();
    }

    @FXML
    void onCloseAction(MouseEvent event) {
        Platform.exit();//平台退出
    }

    @FXML
    void onFullAction(MouseEvent event) {
        Stage stage = findStage();
        stage.setFullScreen(!stage.isFullScreen());//看看是否是全屏状态，如果是全屏取消全屏
    }

    @FXML
    void onMiniAction(MouseEvent event) {
        Stage stage = findStage();
        stage.setIconified(true);//最小化
    }
    @FXML
    void onSoundPopupAction (MouseEvent event) {
        Stage stage = findStage();
        Bounds bounds = soundBtn.localToScreen(soundBtn.getBoundsInLocal());
        soundPopup.show(stage,
                bounds.getMinX()-20,
                bounds.getMinY()-165);
    }

    @FXML
    void onAddMusicAction(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();//文件选择器
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("mp3","*.mp3"));
        List<File> fileList = fileChooser.showOpenMultipleDialog(findStage());
        ObservableList<File> items = listView.getItems();
        if(fileList != null){
            fileList.forEach(file -> {
                if (!items.contains(file)){
                    items.add(file);
                }
            });
        }
    }

    @FXML
    void onPlayAction(ActionEvent event) {
        if (player != null){
            if(playButton.isSelected()){
                player.play();
            }else{
                player.pause();
            }
        }
    }
    void PlayNextAction(){
        int index = listView.getSelectionModel().getSelectedIndex();
        int size = listView.getItems().size();
        //如果是最后一首歌下一首就是播放第一首
        index =(index==size -1)?0:index + 1;
        listView.getSelectionModel().select(index);
    }
    @FXML
    void onPlayNextAction(MouseEvent event) {
        int size = listView.getItems().size();
        if (size <2){
            return;
        }
        int index = listView.getSelectionModel().getSelectedIndex();
        //如果是最后一首歌下一首就是播放第一首
        index =(index==size -1)?0:index + 1;
        listView.getSelectionModel().select(index);
    }

    @FXML
    void onPlayPrevAction(MouseEvent event) {
        int size = listView.getItems().size();
        if (size <2){
            return;
        }
        int index = listView.getSelectionModel().getSelectedIndex();
        //如果是最后一首歌下一首就是播放第一首
        index =(index==0)?size -1:index -1;
        listView.getSelectionModel().select(index);
    }


}
