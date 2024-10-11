package com.videoplayer;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;

import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;

public class VideoPlayer {
    private static VideoPlayer instance;
    private int count = 0;

    private JFrame              frame;
    private JMenuBar            menuBar;
    private JMenuItem           openFile, openLink, openFolder;
    private JFXPanel            fxPanel;
    private JFileChooser        chooser;
    private JDialog             dialogBox;
    private JTextArea           dialogLabel;
    private Label               fileNameLabel;
    private Dimension           screenSize;
    private MediaPlayer         mediaPlayer;
    private MediaView           mediaView;
    private SplitPane           root;
    private ScrollPane          thumbnailPane;
    private BorderPane          mediaViewer;
    private Image               img;
    private ImageView           imgView;
    private double              currW, currH;
    private double              screenW, screenH;
    private HBox                controlBox;
    private VBox                thumbnailView;
    private boolean             playing = false;
    private FilesList           files;
    private Image               audioImg;
    private Image               videoImg;

    private String currentFile = "";
    private String selectedFile;
    private String selectedFolder;
    private String inputLink;
    private String savePath;
    final String[] SUPPORTED_FILES = new String[]{"gif", "jpg", "jpeg", "png", "tiff", "tif", "mp4", "mp3", "m4a"};

    public VideoPlayer() {
        frame               = new JFrame();
        fxPanel             = new JFXPanel();
        chooser             = new JFileChooser();
        dialogBox           = new JDialog();
        dialogLabel         = new JTextArea();
        fileNameLabel       = new Label();
        menuBar             = new JMenuBar();
        openFile            = new JMenuItem("Open File");
        openLink            = new JMenuItem("Open Link");
        openFolder          = new JMenuItem("Load Folder");
        files               = new FilesList();
        audioImg            = SwingFXUtils.toFXImage(createAudioImage(), null);
        videoImg            = SwingFXUtils.toFXImage(createVideoImage(), null);

        getScreenSize();
        screenW = screenSize.getWidth();
        screenH = screenSize.getHeight();
        
        chooser.setSize((int)screenW / 2, (int)screenH / 2);
        frame.setSize(4 * (int)screenW / 5, 4 * (int)screenH / 5);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);        
        frame.setMinimumSize(new Dimension((int)screenW / 10, (int)screenH / 10));

        dialogBox.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialogBox.setSize((int)screenW / 4, (int)screenH / 4);
        dialogLabel.setEditable(false);
        dialogLabel.setWrapStyleWord(true);

        openFile.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setDialogTitle("Open file...");
                chooser.setDialogType(JFileChooser.OPEN_DIALOG);
                int choice = chooser.showOpenDialog(frame);

                if(choice != JFileChooser.APPROVE_OPTION) return;

                loadFile(); // Load the file if the operation was not cancelled.
            }   
        });

        openLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                final String link = JOptionPane.showInputDialog("Enter the link that you want to load from: ");
                loadLink(link);
            }   
        });

        openFolder.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogType(JFileChooser.OPEN_DIALOG);
                chooser.setDialogTitle("Choose a folder... ");
                
                int choice = chooser.showOpenDialog(frame);

                if(choice != JFileChooser.APPROVE_OPTION) return;

                 loadFolder();
            }
        });

        menuBar.add(openFolder);
        menuBar.add(openFile);
        menuBar.add(openLink);
        dialogBox.add(dialogLabel);
        frame.add(fxPanel);
        frame.setJMenuBar(menuBar);

        frame.setVisible(true);

        Platform.runLater(() -> initFX(fxPanel, frame));
        instance = this;
    }

    private void initFX(JFXPanel panel, JFrame frame) {
        try {
            mediaView = new MediaView();
            thumbnailView = new VBox();
            controlBox = createControls();
            thumbnailPane = new ScrollPane();
            mediaViewer = new BorderPane();
            StackPane centeredThumbnailContainer = new StackPane();
            centeredThumbnailContainer.getChildren().add(thumbnailView);
            centeredThumbnailContainer.setAlignment(Pos.CENTER);

            thumbnailPane.vvalueProperty().addListener((obs, oldValue, newValue) -> updateVisibleThumbnails());

            mediaView.getStyleClass().add(".mainView");
            thumbnailView.setId("thumbnailView");
            mediaViewer.setId("mediaViewer");

            // Set the StackPane as the content of the ScrollPane
            thumbnailPane.setContent(centeredThumbnailContainer);

            thumbnailPane.setMinSize(screenW / 10 * 0.2, screenH / 10 * 0.2);
            mediaViewer.setMinSize(screenW / 10 * 0.8, screenH / 10 * 0.8);
            mediaViewer.setPrefSize(frame.getContentPane().getWidth() * 0.9, frame.getContentPane().getWidth() * 0.9);
            mediaViewer.setTop(fileNameLabel);
            mediaViewer.setCenter(mediaView);
            mediaViewer.setBottom(controlBox); // Set the HBox containing all controls

            BorderPane.setAlignment(fileNameLabel, Pos.TOP_LEFT);
            BorderPane.setAlignment(mediaView, Pos.CENTER);
            BorderPane.setAlignment(controlBox, Pos.CENTER);

            BorderPane.setMargin(controlBox, new Insets(10));
            BorderPane.setMargin(mediaView, new Insets(10));
            BorderPane.setMargin(fileNameLabel, new Insets(10));
            
            mediaViewer.widthProperty().addListener((observable, oldValue, newValue) -> { resizeMediaViewer(); resizeReturn(); });
            mediaViewer.heightProperty().addListener((observable, oldValue, newValue) -> { resizeMediaViewer(); resizeReturn(); });

            // Adjust the layout to center the media view
            root = new SplitPane();
            root.setOrientation(Orientation.HORIZONTAL);
            root.getItems().add(thumbnailPane);
            root.getItems().add(mediaViewer);
    
            Scene panelScene = new Scene(root);
            panelScene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            fxPanel.setScene(panelScene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HBox createControls() {
        Button playButton = new Button("Play");
        playButton.setOnAction(e -> {
            if (mediaPlayer == null) return;
            if(!playing)    mediaPlayer.play();
            else            mediaPlayer.stop();
            
            playing = !playing;
        });

        Button restartButton = new Button("Restart");
        restartButton.setOnAction(e -> {
            if(mediaPlayer == null) return;
            mediaPlayer.seek(new Duration(0));
        });

        Button saveButton = new Button("Download");
        saveButton.setOnAction(event -> {
            try {
                Thread downloadThread      = new Thread(() -> { 
                    savePath = JOptionPane.showInputDialog("Enter the file path: ");    
                    String path = Downloader.download(inputLink, savePath);
                    if(path != null)
                        System.out.println("Download Finished. The path to the file: " + path);
                });
                downloadThread.start();
                downloadThread.join();
            }
            catch(InterruptedException e) {
                e.printStackTrace();
                System.out.println("Error while closing the thread: " + e);
            }
        });

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> { mediaPlayer.pause(); playing = false;});

        Button prevButton = new Button("Previous");
        prevButton.setOnAction(e -> { loadPrev(); });

        Button nextButton = new Button("Next");
        nextButton.setOnAction(e -> { loadNext(); });

        Button forward10s = new Button("10s > ");
        forward10s.setOnAction(e -> {
            if(mediaPlayer == null) return;
            
            mediaPlayer.seek(mediaPlayer.getCurrentTime().add(new Duration(10000)));
            playing = true;
        });
        Button backward10s = new Button("< 10s");
        backward10s.setOnAction(e -> {
            if(mediaPlayer == null) return;
            double sec = mediaPlayer.getCurrentTime().toSeconds(); // currently elapsed time (in seconds).
            Duration newTime = Duration.seconds(sec < 10 ? 0 : sec - 10); // If seconds elapsed is less than 10, then seek to 0 else currentElapsedTime - 10
            
            mediaPlayer.seek(newTime);
            playing = true;
        });
        Button resetButton = new Button("Reset List");
        resetButton.setOnAction(e -> { 
            files.reset(); 
            
            if(mediaPlayer != null) { 
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
            mediaView = new MediaView();
            mediaView.setId("mainMediaView");
            
            if(imgView != null)
                imgView.setImage(null);
            mediaViewer.getChildren().remove(mediaView);
            mediaViewer.getChildren().remove(imgView);
            thumbnailView.getChildren().clear();
            fileNameLabel.setText("");
            count = 0;
        });

        HBox ret = new HBox( backward10s, playButton, pauseButton, forward10s, saveButton, resetButton, prevButton, nextButton); // Aligning elements horizontally
        ret.setAlignment(Pos.CENTER);
        ret.setSpacing(10);

        return ret;
    }

    private void updateVisibleThumbnails() {
        // Calculate which thumbnails are in the viewport
        double viewportHeight = thumbnailPane.getViewportBounds().getHeight();
        double contentOffset = thumbnailPane.getVvalue() * (thumbnailView.getHeight() - viewportHeight);
    
        // Loop through thumbnailView's children (ImageViews)
        for (javafx.scene.Node node : thumbnailView.getChildren()) {
            if (node instanceof ImageView) {
                ImageView imgView = (ImageView) node;
                double nodeY = imgView.getBoundsInParent().getMinY();
    
                if (nodeY >= contentOffset - 200 && nodeY <= contentOffset + viewportHeight + 200) {
                    if(imgView.isDisabled()) imgView.setDisable(false);
                }
                else
                    imgView.setDisable(true);
            }
        }
    }

    public static void main(String[] args) { 
        new VideoPlayer(); 
    }

    private void getScreenSize() {  screenSize = Toolkit.getDefaultToolkit().getScreenSize();  }

    private void loadFile() {
        selectedFile = chooser.getSelectedFile().getAbsolutePath();
        selectedFile = new File(selectedFile).toURI().toString();
        createThumbnail(selectedFile, getFileExtension(selectedFile), files.getSize());
        if(!files.contains(selectedFile)) files.addItem(selectedFile);
        loadLast();
    }

    private void loadFile(String fileName) {
        if(currentFile == null) currentFile = "";
        if(currentFile.equals(fileName)) return;

        currentFile = fileName;
        inputLink = fileName;
        // Check if there are any files loaded.
        if(fileName == null) {
            dialogLabel.setText("There are no files to cycle through. Open a file, link or a folder.");
            dialogBox.setVisible(true);
            return;
        }
        // Categorising the Selected File
        String ext = getFileExtension(fileName);

        if(ext.equals("mp4")) 
            Platform.runLater(() -> loadVideo(fileName));
        else if(ext.equals("mp3") || ext.equals("m4a"))
            Platform.runLater(() -> loadAudio(fileName));
        else if(ext.equals("jpg")   ||
                ext.equals("jpeg")  ||
                ext.equals("png")   ||
                ext.equals("tiff")  ||
                ext.equals("tif")   ||
                ext.equals("gif"))
            Platform.runLater(() -> loadImage(fileName));
        else {
            dialogLabel.setText("The selected file is unsupported. The selected file is: " + fileName);
            dialogBox.setVisible(true);
        }

        Platform.runLater(() -> updateDisplayName(fileName));
    }

    private void loadLink(final String link) {
        if(link == null) return;

        boolean isDirectLink = isSupported(getFileExtension(link));

        if(isDirectLink) { 
            System.out.println("Direct link found.");
            int i = files.getSize() - 1;
            files.addItem(link);
            createThumbnail(link, getFileExtension(link), i);
            loadIndex(i);
            return;
        }

        try {
            BufferedInputStream reader = new BufferedInputStream(new URL(link).openStream());
            String html = new String(reader.readAllBytes());
            reader.close();
            String segments[] = html.split("src=\""), x;
            final int startSize = files.getSize();

            for(int i = 0; i < segments.length; i++) {
                x = segments[i];
                x = x.substring(0, x.indexOf("\""));
                if(isSupported(getFileExtension(x))) {
                    if(!x.startsWith("https:"))
                        x = "https:" + (x.startsWith("//") ? "" : "//") + x;
                    
                    files.addItem(x);
                    createThumbnail(x, getFileExtension(x), startSize + i);
                }
            }
            loadIndex(startSize);
        }
        catch(IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            dialogLabel.setText(e.getMessage() + " " + sw.toString());
            dialogBox.setVisible(true);
        }
        catch(StringIndexOutOfBoundsException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            dialogLabel.setText(e.getMessage() + " " + sw.toString());
            dialogBox.setVisible(true);
        }
    }

    private void loadFolder() {
        selectedFolder = chooser.getSelectedFile().getAbsolutePath();
        String arr[] = new File(selectedFolder).list();
        final int startSize = files.getSize();

        for(int i = 0; i < arr.length; i++) {
            final String ext = getFileExtension(arr[i]);
            final String format = new File(selectedFolder + "\\" + arr[i]).toURI().toString();
            final int index = startSize + i;

            if(isSupported(ext) && !files.contains(format)) {
                files.addItem(format);
                createThumbnail(format, ext, index);
            }
        }

        loadIndex(startSize);
        System.out.println("Files processed: " + files.getSize() + " Files with thumbnails: " + count);
    }

    private void loadImage(String url) {
        try {
            if(imgView == null) { imgView = new ImageView(); imgView.getStyleClass().add(".mainView"); }
            if(mediaPlayer != null) { mediaPlayer.dispose(); mediaPlayer = null; }

            imgView.setOnMouseClicked(null);

            this.mediaViewer.getChildren().remove(mediaView);
            img = new Image(url);
            currW = img.getWidth();
            currH = img.getHeight();
            resizeReturn();
            imgView.setDisable(false);
            imgView.setImage(img);
            mediaViewer.setCenter(imgView);
        }
        catch(Exception e) {
            System.out.println("Error occured: " + e + "\nURL: " + url);
        }
    }

    private void loadVideo(String url) {
        try {
            if(imgView != null) imgView.setDisable(true);
            
            System.out.println("URL: " + url);
            if(mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
            this.mediaPlayer = new MediaPlayer(new Media(url));
            this.mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            
            this.mediaPlayer.setOnReady(() -> {
                resizeMediaViewer();
                mediaPlayer.play();
                playing = true;
            });
            
            this.mediaView.setMediaPlayer(this.mediaPlayer);
            this.mediaViewer.getChildren().remove(imgView);
            this.mediaViewer.setCenter(mediaView);
            System.out.println("Finished Loading...");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error when loading: " + url);
        }
    }

    private void loadAudio(String url) {
        try {
            System.out.println("URL: " + url);

            if(imgView == null) { imgView = new ImageView(); imgView.getStyleClass().add(".mainView"); }
            imgView.setImage(audioImg);
            imgView.setDisable(false);
            resizeReturn();

            imgView.setOnMouseClicked(new EventHandler<Event>() {
                public void handle(Event e) {
                    if (mediaPlayer == null) return;

                    if(!playing)    mediaPlayer.play();
                    else            mediaPlayer.pause();
                    
                    playing = !playing;
                }
            });
            
            if(mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
            this.mediaPlayer = new MediaPlayer(new Media(url));
            //this.mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            
            this.mediaPlayer.setOnReady(() -> {
                resizeMediaViewer();
                mediaPlayer.play();
                playing = true;
            });
            
            this.mediaView.setMediaPlayer(this.mediaPlayer);
            this.mediaViewer.setCenter(mediaView);
            this.mediaViewer.setCenter(imgView);
            System.out.println("Finished Loading...");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error when loading: " + url);
        }
    }

    public BufferedImage createAudioImage() {
        BufferedImage img = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.setColor(new Color(1, 1, 1));
        g.fillRect(0, 0, 500, 500);
        g.setColor(new Color(0.1f, 0.1f, 0.3f));
        g.fillRoundRect(80, 170, 100, 160, 20, 20);
        g.fillPolygon(new int[]{150, 160, 220, 400, 400, 220, 160, 150}, new int[]{170, 150, 150, 20, 480, 350, 350, 330}, 8);
        g.dispose();
        return img;
    }

    public BufferedImage createVideoImage() {
        BufferedImage img = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.setColor(new Color(1, 1, 1));
        g.fillRect(0, 0, 500, 500);
        g.setColor(new Color(0.1f, 0.1f, 0.3f));
        g.fillPolygon(new int[]{150, 400, 150}, new int[]{100, 250, 400}, 3);
        g.dispose();
        return img;
    }

    private void loadIndex(int i) {
        if(files == null) {
            dialogLabel.setText("There are no files to cycle through.");
            dialogBox.setVisible(true);
        }
        loadFile(files.getIndex(i));
    }

    private void loadNext() {
        if(files == null) {
            dialogLabel.setText("There are no files to cycle through.");
            dialogBox.setVisible(true);
        }
        loadFile(files.getNext());
    }

    private void loadPrev() {
        if(files == null) {
            dialogLabel.setText("There are no files to cycle through.");
            dialogBox.setVisible(true);
        }
        loadFile(files.getPrev());
    }

    private void loadLast() {
        if(files == null) {
            dialogLabel.setText("There are no files to cycle through.");
            dialogBox.setVisible(true);
        }
        loadFile(files.getLast());
    }

    private void resizeMediaViewer() {
        if(mediaPlayer == null) return;
        // Resize mediaView to fit the window, maintaining aspect ratio
        double windowWidth      = mediaViewer.getWidth() - 40;
        double windowHeight     = mediaViewer.getHeight() - 10;
        double videoWidth       = mediaPlayer.getMedia().getWidth();
        double videoHeight      = mediaPlayer.getMedia().getHeight();
        
        double widthScale = windowWidth / videoWidth;
        double heightScale = (windowHeight - 100) / videoHeight; // Subtracting 50 for the button box height
        double scale = Math.min(widthScale, heightScale);
        
        double newWidth = Math.min(windowWidth, videoWidth * scale);
        double newHeight = Math.min(windowHeight, videoHeight * scale);
        
        this.mediaView.setFitWidth(newWidth);
        this.mediaView.setFitHeight(newHeight);
    }

    private void resizeReturn() {
        // Resize mediaView to fit the window, maintaining aspect ratio
        double windowWidth = mediaViewer.getWidth() - 50;
        double windowHeight = mediaViewer.getHeight() - 50;
        double videoWidth = currW;
        double videoHeight =  currH;

        double widthScale = windowWidth / videoWidth;
        double heightScale = (windowHeight - 50) / videoHeight; // Subtracting 50 for the button box height
        double scale = Math.min(widthScale, heightScale);
        
        double newWidth = videoWidth * scale;
        double newHeight = videoHeight * scale;

        if(imgView != null) {
            imgView.setFitWidth(newWidth);
            imgView.setFitHeight(newHeight);
        }
    }

    private void createThumbnail(final String link, final String ext, final int index) {
        if(ext.equals("mp3") || ext.equals("m4a"))
           createAudioThumbnail(index);
        else if(ext.equals("mp4"))
           createVideoThumbnail(link);
        else
           createImageThumbnail(link);
    }

    private void createVideoThumbnail(final String link) {
        final int index = files.getIndex(link);
        final double videoWidth = 280, videoHeight = 210;
        final Media media = new Media(link);
        final MediaPlayer mediaPlayer = new MediaPlayer(media);
        final MediaView mediaView = new MediaView(mediaPlayer);
        WritableImage snapshot = new WritableImage((int) videoWidth, (int) videoHeight);
        ImageView imgView = new ImageView();
        StackPane s = new StackPane();
        
        mediaPlayer.setMute(true);
        mediaView.setFitWidth(videoWidth);
        mediaView.setFitHeight(videoHeight);
        
        double time = 0;

        while(mediaPlayer.statusProperty().get() != Status.READY && time / 1000 < 5.0)
            time += System.currentTimeMillis();
        
        if(time / 1000 >= 5) {
            Platform.runLater(() -> {
                imgView.setImage(videoImg);
                imgView.setPreserveRatio(true);
                imgView.setFitHeight(300);
                imgView.setOnMouseClicked((e) -> loadIndex(index));
                imgView.setId(String.valueOf(index));
                
                s.getChildren().add(imgView);
                s.setStyle("-fx-border-width: 5px; -fx-border-color: black;");
                
                thumbnailView.getChildren().add(s);
                
                count++;
            });
            return;
        }

        mediaPlayer.setOnReady(() -> {
            mediaPlayer.seek(new Duration(1000)); // Seek to 1 second
            mediaView.snapshot(null, snapshot);
            
            Platform.runLater(() -> {
                 // Create a new ImageView
                imgView.setImage(snapshot);
                imgView.setPreserveRatio(true);
                imgView.setFitHeight(300);
                imgView.setOnMouseClicked((e) -> loadIndex(index));
                imgView.setId(String.valueOf(index));

                s.getChildren().add(imgView);
                s.setStyle("-fx-border-width: 5px; -fx-border-color: black;");
                
                thumbnailView.getChildren().add(s);
                
                count++;
            });
            
            mediaPlayer.stop();
            mediaPlayer.dispose();
        });
            
        mediaPlayer.setOnError(() -> {
            files.remove(link);
            System.out.println("Error occured. " + link);
        });
    }

    private void createAudioThumbnail(final int index) {
        ImageView imgView = new ImageView(audioImg);
        imgView.setPreserveRatio(true);
        imgView.setFitHeight(180);
        imgView.setOnMouseClicked((e) -> loadIndex(index));

        Platform.runLater(() -> {StackPane s = new StackPane();
            s.getChildren().add(imgView);
            s.setStyle("-fx-border-width: 5px; -fx-border-color: black;");

            thumbnailView.getChildren().add(s);
            
            count++;
        });
    }
    
    private void createImageThumbnail(final String link) {
        final int index = files.getIndex(link);
        ImageView imgView = new ImageView(new Image(link, 355, 200, true, true));
        imgView.setPreserveRatio(true);
        imgView.setFitHeight(200);
        imgView.setOnMouseClicked((e) -> loadIndex(index));
        imgView.setId(String.valueOf(index));

        Platform.runLater(() -> {
            StackPane s = new StackPane();
            s.getChildren().add(imgView);
            s.setStyle("-fx-border-width: 5px; -fx-border-color: black;");
            thumbnailView.getChildren().add(s);

            count++;
        });
    }
    
    private void updateDisplayName(String fileName) { fileNameLabel.setText(fileName); }

    private boolean isSupported(String ext) {
        if(ext == null) return false;
        for(String i : SUPPORTED_FILES)
            if(i.startsWith(ext))
                return true;
        return false;
    }

    public String getFileExtension(String fileName) { 
        for(String x : SUPPORTED_FILES)
            if(fileName.contains("." + x))
                return x;

        return null; 
    }

    public String getDownloadLink() { return inputLink; }
    public String getSavePath() { return savePath; }
    public static VideoPlayer getInstance() { return instance; }
    
}