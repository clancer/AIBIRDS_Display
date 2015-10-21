import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.GroupLayout.Alignment;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
 
public class MainDisplay extends Application {
    
	
	private static String server_ip = "127.0.0.1";
	private static Socket requestSocket;
	private static ObjectInputStream ins;
	//private InputStream ins;
	private static OutputStream outs;
	
	ReplayComponent replayComponent;
	
	public static ArrayList<Integer> teamIndex = new ArrayList<Integer>(); 
	public static ArrayList<Pair> pairs = new ArrayList<Pair>();
	public static ArrayList<ArrayList<Pair>> pairings = new ArrayList<ArrayList<Pair>>();
	public static int round = 0;
	
	
	 //dummy data
	 private ObservableList<Team> teams = FXCollections.observableArrayList(

	 );
	 

	 
	 private static String[] color_classes = {
		 "red",
		 "blue",
		 "purple",
		 "orange"
	 };
	 

	
    @Override
    public void start(Stage primaryStage) throws InterruptedException {

    	
    	
        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction(new EventHandler<ActionEvent>() {
 
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Hello World!");
            }
        });
        
        StackPane root = new StackPane();
        root.getChildren().add(btn);

        
        BorderPane border = new BorderPane();


        border.setCenter(addReplayPane());
        border.setRight(addScoreboard());
        border.setBottom(addNewsPane());
        
        
        Scene scene = new Scene(border, 980, 730);
        

        //scene.getStylesheets().add(MainDisplay.class.getResource("/styles.css").toExternalForm());
        try {
            URL.setURLStreamHandlerFactory(new StringURLStreamHandlerFactory());
        } catch (Error e) {
        	System.out.println("url factory error");
        }

        
        scene.getStylesheets().setAll("internal:stylesheet.css");
        
        primaryStage.setTitle("Welcome to The Angry Birds AI Competition");
        primaryStage.setScene(scene);
        
        
        final long startNanoTime = System.nanoTime();
        
        new AnimationTimer()
        {
            public void handle(long currentNanoTime)
            {
                double t = (currentNanoTime - startNanoTime) / 1000000000.0; 
     
                //model.update(t);
                
                replayComponent.update(t);
                
                replayComponent.draw(t);
                
            }
        }.start();
        
        
        primaryStage.show();

        
		Thread socketThread = new Thread(new Runnable () {

			@Override
			public void run() {
				try {
					requestSocket = new Socket(server_ip, 2005);
					ins = new ObjectInputStream(requestSocket.getInputStream());
					outs = requestSocket.getOutputStream();
					
					while (true) {
						int message = ins.readInt();
						if (message == 1) {
							ArrayList<ArrayList<ArrayList<Integer>>> temp_pairings = (ArrayList<ArrayList<ArrayList<Integer>>>) ins.readObject();
							
							for (ArrayList<ArrayList<Integer>> temp_pairs : temp_pairings) {
								ArrayList<Pair> temp_pairs_builder = new ArrayList<Pair>();
								for (ArrayList<Integer> temp_pair : temp_pairs) {
									System.out.println(temp_pair.get(0) + " paired with " + temp_pair.get(1));
									temp_pairs_builder.add(new Pair(temp_pair.get(0), temp_pair.get(1)));

								}
								pairings.add(temp_pairs_builder);
							}
						
							pairs = pairings.get(round);
							for (Pair pair : pairs) {
								teams.add(new Team(pair.first, 0));
								teams.add(new Team(pair.second, 0));
							}
							 for (Team team : teams) {
								 teamIndex.add(team.getId());
							 }
						} else if (message == 3) {
							round++;
							pairs = pairings.get(round);
							
		                	Platform.runLater(new Runnable() {
		                        @Override public void run() {
		                        	System.out.println("round over");
		                        	Team leader = table.getItems().get(0);
		                        	list.getItems().add(0, "Round " + round + " is over! Team: " + leader.getId() + " leads with " + leader.getScore() + " points!");
		                        	
		                        }
		                    });
							
							
						} else if (message == 2) {
							boolean first_goes_first = ins.readBoolean();
							boolean first_won = ins.readBoolean();
							int first_id = ins.readInt();
							int second_id = ins.readInt();
							int first_bid = ins.readInt();
							int second_bid = ins.readInt();
							int first_score = ins.readInt();
							int second_score = ins.readInt();
							LinkedList<String> dirs = (LinkedList<String>) ins.readObject();
							
		                	Platform.runLater(new Runnable() {
		                        @Override public void run() {
		                        	int winner_id = (first_score > second_score ? first_id : second_id);
		                        	int dif = Math.abs(first_score - second_score);
		                        	list.getItems().add(0, "Team: " + winner_id + " just got a " + dif + " point advantage over their opponent");
		                        	System.out.println("updating scores");
		                        	ArrayList<Team> temp = new ArrayList<Team>();
		                        	temp.addAll(teams);
		                        	for (Team team : temp) {
		                        		if (team.getId() == first_id) {
		                        			team.addPoints(first_score);
		                        		} else if (team.getId() == second_id) {
		                        			team.addPoints(second_score);
		                        		}
		                        	}
		                        	Collections.sort(temp, new Comparator<Team>() {
										@Override
										public int compare(Team	o1, Team o2) {
											return o2.getScore() - o1.getScore();
										}
		                        		 });
		                        	table.getItems().clear();
		                        	for (Team team : temp) {
		                        		table.getItems().add(team);
		                        	}
		                        }
		                    });
							
							replayComponent.story_queue.add(new Story(first_id, second_id, first_bid, second_bid, first_goes_first, dirs));
						}
						
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		});
		socketThread.start();
        
    }
    
    ListView<String> list;
    
    BlockingQueue<String> bq = new LinkedBlockingQueue<String>();
    
 public static void main(String[] args) {
		if(args.length == 1 && args[0].contains("."))
		{	
			server_ip = args[0]; 
		}
        launch(args);
    }
 
 public VBox addReplayPane() {
	 VBox stack = new VBox();
	 stack.setPrefSize(ReplayComponent.CANVAS_WIDTH, ReplayComponent.CANVAS_HEIGHT+100);
	 stack.setStyle("-fx-background-color: #F2EAE7");
	 
	 
	 Canvas canvas = new Canvas(ReplayComponent.CANVAS_WIDTH, ReplayComponent.CANVAS_HEIGHT);
	 
	 canvas.setStyle("-fx-effect: innershadow(gaussian, #F2EAE7, 10, 0.5, 0, 0);");
	 
	 replayComponent = new ReplayComponent(canvas);
	 
	 Label welcome_label = new Label("    Welcome to The Angry Birds AI Competition");
	 welcome_label.setFont(Font.font( "Roboto", FontWeight.NORMAL, 28 ));
	 
	 Label news_label = new Label("    Recent News");
	 news_label.setFont(Font.font( "Roboto", FontWeight.NORMAL, 28 ));
	 
	 
	 stack.getChildren().add(welcome_label);
	 
	 stack.getChildren().add(canvas);
	 
	 stack.getChildren().add(news_label);
	 
	 return stack;
 }
 
 public StackPane addNewsPane() {
	 StackPane stack = new StackPane();
	 stack.setPrefSize(980, 220);
	 stack.setStyle("-fx-background-color: green");
	 
	 list = new ListView<String>();
	 ObservableList<String> items =FXCollections.observableArrayList (
	     );
	 list.setItems(items);
	 
	  new Timer().scheduleAtFixedRate(new TimerTask() {
		  @Override
		  public void run() {
			  bq.add("test");
		  }
		}, 1000, 1000);
	 
	 stack.getChildren().add(list);
	 
	 return stack;
 }
 

 
 
 TableView<Team> table;
 
 public VBox addScoreboard() {
	 // dummy data
	 //pairs.add(new Pair(123123,456456));
	 //pairs.add(new Pair(787878,999999));
	 
     
     Label label = new Label("    Scoreboard");
     label.setFont(Font.font( "Roboto", FontWeight.NORMAL, 28 ));
     
     table = new TableView<Team>();
     
     TableColumn teamIDCol = new TableColumn("Team_ID");
     teamIDCol.setMinWidth(50);
     teamIDCol.setCellValueFactory(
    		 new PropertyValueFactory<Team, Integer>("id"));
     
     
     TableColumn scoreCol = new TableColumn("Score");
     scoreCol.setMinWidth(200);
     scoreCol.setCellValueFactory(
    		 new PropertyValueFactory<Team, Integer>("score"));
     
     
     table.setItems(teams);
     table.getColumns().addAll(teamIDCol, scoreCol);
     table.setFixedCellSize(55);
     table.setPrefSize(300, 700);
     table.setMouseTransparent(true);
     table.setSelectionModel(new NullTableViewSelectionModel(table));
     //table.
     
     //table.getColumns().get(0).
     
     table.setRowFactory(new Callback<TableView<Team>, TableRow<Team>>() {
         @Override
         public TableRow<Team> call(TableView<Team> tableView) {
             final TableRow<Team> row = new TableRow<Team>() {
                 @Override
                 protected void updateItem(Team team, boolean empty){
                     super.updateItem(team, empty);
                     if (!empty) {
                     getStyleClass().clear();
                     int pair_index = 0;
                     for (Pair p : pairs) {
                    	 if (team.getId() == p.first || team.getId() == p.second) {
                    		 getStyleClass().add(color_classes[pair_index]);
                    	 }
                    	 pair_index++;
                     }
                     }
                     //if (highlightRows.contains(getIndex())) {
                     //    if (! getStyleClass().contains("highlightedRow")) {
                     //        getStyleClass().add("highlightedRow");
                     //    }
                     //} else {
                     //   getStyleClass().removeAll(Collections.singleton("highlightedRow"));
                     //}
                 }
             };
             /*highlightRows.addListener(new ListChangeListener<Integer>() {
                 @Override
                 public void onChanged(Change<? extends Integer> change) {
                     if (highlightRows.contains(row.getIndex())) {
                         if (! row.getStyleClass().contains("highlightedRow")) {
                             row.getStyleClass().add("highlightedRow");
                         }
                     } else {
                         row.getStyleClass().removeAll(Collections.singleton("highlightedRow"));
                     }
                 }
             });*/
             return row;
         }
     });
     
     
     VBox vbox = new VBox();
     vbox.getStyleClass().add("test");
	 vbox.setPrefSize(300, 700);
	 vbox.setStyle("-fx-background-color: #F2EAE7");
     vbox.setSpacing(5);
     //vbox.setPadding(new Insets(10, 0, 0, 0));
     vbox.getChildren().addAll(label, table);
     




	 return vbox;
	 
	 
 }

}

class StringURLConnection extends URLConnection {
	static String css = ".red {" +
			"-fx-background-color: #D67777;" +
			"-fx-table-cell-border-color: #D67777;" +
			"}" +
			".blue {" +
			"	-fx-background-color: #6581CE;" +
			"	-fx-table-cell-border-color: #6581CE;" +
			"}" +
			".purple {" +
			"	-fx-background-color: #BD93D6;" +
			"	-fx-table-cell-border-color: #BD93D6;" +
			"}" +
			".orange {" +
			"	-fx-background-color: #F7BB7B;" +
			"	-fx-table-cell-border-color: #F7BB7B;" +
			"}" +
			".table-cell {" +
			"	-fx-alignment: CENTER-LEFT;" +
			"	-fx-font: 16px \"Roboto\";" +
			"}" +
			"" +
			".table-row-cell:filled:selected { " +
			"  -fx-background: -fx-control-inner-background ;" +
			"  -fx-background-color: -fx-table-cell-border-color, -fx-background ;" +
			"  -fx-background-insets: 0, 0 0 1 0 ;" +
			"  -fx-table-cell-border-color: derive(-fx-color, 5%);" +
			"}" +
			".table-row-cell:odd:filled:selected {" +
			"  -fx-background: -fx-control-inner-background-alt ;" +
			"}";
	
    public StringURLConnection(URL url){
        super(url);
    }
    @Override public void connect() throws IOException {}
    @Override public InputStream getInputStream() throws IOException {
        return new StringBufferInputStream(css);
    }
}
class StringURLStreamHandlerFactory implements URLStreamHandlerFactory {
    URLStreamHandler streamHandler = new URLStreamHandler(){
        @Override protected URLConnection openConnection(URL url) throws IOException {
            if (url.toString().toLowerCase().endsWith(".css")) {
                return new StringURLConnection(url);
            }
            throw new FileNotFoundException();
        }
    };
    @Override public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("internal".equals(protocol)) {
            return streamHandler;
        }
        return null;
    }
}

