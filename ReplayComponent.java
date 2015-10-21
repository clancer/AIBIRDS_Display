import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;


public class ReplayComponent {
	public enum ReplayState {Bid, Play, Wait};
	
	
	public static final String baseDir = "../ABServer1.0_Full/DB/";
	
	public ReplayState state = ReplayState.Wait;
	
	public LinkedList<Story> story_queue;
	public LinkedList<LinkedList<byte[]>> current_screenshots;
	
	public Iterator<LinkedList<byte[]>> current_screenshot_group_i;
	public Iterator<byte[]> current_screenshot_i;
	public byte[] current_screenshot;
	
	public boolean a_shooting = false;
	public boolean loaded_screenshots = false;
	
	public double next_screenshot = 0.0;
	
	public int team_index_a = 0;
	public int team_index_b = 1;
	public int team_id_a = 123123;
	public int team_id_b = 456456;
	
	
	public Canvas canvas;
	public GraphicsContext gc;
	
    final static int CANVAS_WIDTH = (840 * 7) /8;
    final static int CANVAS_HEIGHT = (480 * 7) /8;
    
    private int[][] coinstack_a = {
    		{5, 5, 5},
    		{3, 2, 3},
    		{2, 1, 3}
    };
    private int[][] coinstack_b = {
    		{5, 5, 5},
    		{3, 2, 3},
    		{2, 1, 3}
    };
    
    
    private int coin_display_count = 0;
    private int coin_active_frame = 0;
    private int current_coins_a = 0;
    private int current_coins_b = 0;
    
	Image[] flock;
    
    Image coin;
	
	int width;
	int height;
	
	double transition_timer;
	boolean transition_timer_on = false;
	
	private Story story;

	private double bid_start = 0;
	
	
	public ReplayComponent (Canvas canvas) {
		loadResources();
		
	    this.canvas = canvas;
	    gc = canvas.getGraphicsContext2D();
	    
	    current_screenshots = new LinkedList<LinkedList<byte[]>>();
	    
	    
	    story_queue = new LinkedList<Story>();
	}
	
	public void update(double t) {
		switch(state) {
		case Bid:
			// run through coin stack animation then transition to play
			int sum_a = 0;
			int sum_b = 0;
			for (int x = 0; x < story.coinstack_a.length; x++) {
				for (int y = 0; y < story.coinstack_b.length; y++) {
					sum_a += story.coinstack_a[x][y];
					sum_b += story.coinstack_b[x][y];
				}
			}
			
			int max_coin_sum = Math.max(sum_a, sum_b);
		
				
			coin_display_count  = ((int) Math.ceil((t-bid_start)*4));
			coin_active_frame = (int) (((t-bid_start) * 100) % 25);
			
			current_coins_a = Math.min(coin_display_count, sum_a);
			current_coins_b = Math.min(coin_display_count, sum_b);
			
			if (current_coins_a < current_coins_b) {
				b_color = green;
				a_color = red; 
			} else {
				a_color = green;
				b_color = red;
			}
			
			if (transition_timer_on) {
				if (story.a_goes_first) {
					a_color = green;
					b_color = red;
				} else {
					b_color = green;
					a_color = red;
				}
			}
			
			if (coin_display_count > max_coin_sum && !transition_timer_on) {
				transition_timer_on = true;
				transition_timer = t + 2;
			}
			
			if (transition_timer_on && transition_timer < t) {
				transition_timer_on = false;
				state = ReplayState.Play;
			}
			break;
		case Play:

			// run through frames then transition to wait
			if (!loaded_screenshots && !transition_timer_on) {
				current_screenshots = new LinkedList<LinkedList<byte[]>>();
				try {
					//ArrayList<String> dirs = new ArrayList<String>();
					//dirs.add("DB/20/0_10_2_1438229806177");
					//dirs.add("DB/20/1_10_2_1438229806253");
					//dirs.add("DB/20/2_10_2_1438229806319");
					//dirs.add("DB/20/3_10_2_1438229806362");
					
					
					//dirs.add(baseDir+"10/0_10_2_1438230801497");
					//dirs.add(baseDir+"10/1_10_2_1438230801522");
					//dirs.add(baseDir+"10/2_10_2_1438230801574");
					//dirs.add(baseDir+"10/3_10_2_1438230801628");
					//dirs.add("DB/10/4_10_3_1438229881742");
					
					for (String screenshots_dir : story.dirs) {
						//String screenshots_dir = "DB/30/10_1_1437536367518/";
		
						LinkedList<byte[]> screenshot_buffer = new LinkedList<byte[]>();
						
						//screenshots_dir = "..\\ABServer1.0_Full\\DB\\" + screenshots_dir.substring(5);
						
						if (screenshots_dir != "") {
							System.out.println(" Reterive screenshots at : " + screenshots_dir);
					
							File file = new File(screenshots_dir);
							System.out.println(file.isDirectory());
							if(file.isDirectory())
							{
								
								File[] _file = file.listFiles();
								Arrays.sort( _file, new Comparator<File>() {
								    public int compare( File a, File b ) {
								        Integer _a = Integer.parseInt(a.getName());
								        Integer _b = Integer.parseInt(b.getName());
								        return _a.compareTo(_b);
								    }
								} );
								for (int i = 0; i < _file.length; i++)
								{
									
									ObjectInputStream objectInputStream = new ObjectInputStream
											(new BufferedInputStream(new FileInputStream(_file[i])));
									@SuppressWarnings("unchecked")
									LinkedList<byte[]> screenshots = (LinkedList<byte[]>) objectInputStream.readObject();
									screenshot_buffer.addAll(screenshots);
									System.out.println("Read File (SIFS run): " + screenshots.size());
									objectInputStream.close();
								}
							}

						}
						current_screenshots.add(screenshot_buffer);

					}
					loaded_screenshots = true;
					System.out.println(" Loaded Screenshots ");
					next_screenshot = t;
					current_screenshot_group_i = current_screenshots.iterator();
					current_screenshot_i = current_screenshot_group_i.next().iterator();
					current_screenshot = current_screenshot_i.next();
				} catch (IOException | ClassNotFoundException e) {
					System.out.println("Error loading screenshots");
				}
			} else if (!transition_timer_on) {
				if (t > next_screenshot) {
					next_screenshot += 0.1;
					
					if (current_screenshot_i.hasNext()) {
						current_screenshot = current_screenshot_i.next();
					} else {
						if (current_screenshot_group_i.hasNext()) {
							current_screenshot_i = current_screenshot_group_i.next().iterator();
							current_screenshot = current_screenshot_i.next();
							a_shooting = !a_shooting;
						} else {
							transition_timer_on = true;
							transition_timer = t + 2;
						}
					}
					
					//current_screenshots.removeFirst();
					//if (current_screenshots.isEmpty()) {
					//	transition_timer_on = true;
					//	transition_timer = t + 2;
					//}
				}
			}
			if (transition_timer_on && transition_timer < t) {
				state = ReplayState.Wait;
				transition_timer_on = false;
			}
			break;
		case Wait:
			// display waiting screen
			// attempt to take a new story from queue
			if (story_queue.size() > 0) {
				story = story_queue.poll();
				a_shooting = story.a_goes_first;
				loaded_screenshots = false;
				bid_start  = t;
				state = ReplayState.Bid;
			}
			break;
		}
	}
	
	public static final Color red = Color.rgb(234, 52, 0);//.rgb(176, 92, 104);
	public static final Color green = Color.rgb(102, 176, 50);//.rgb(156, 178, 110);
	public final static Color gold = Color.rgb(248, 214, 52);
	
	static final int b_offset_y = CANVAS_HEIGHT/2;
	
	private Color a_color, b_color;
	
	public void draw(double t) {
		switch(state) {
		case Bid:
			gc.setFill (Color.WHITE );
			gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
			
			Font theFont = Font.font( "Roboto", FontWeight.NORMAL, 28 );
			
			
			/* Screen A */
			
			
			gc.setFill( a_color );
			gc.fillRect(0, 0, CANVAS_WIDTH, b_offset_y);
			
			
		    

		    
		    gc.setFill( gold );
		    gc.setFont( theFont );
		    if (transition_timer_on) {
		    	gc.fillText( "$" + NumberFormat.getNumberInstance(Locale.US).format(story.bid_a), 50, b_offset_y/2 + 70 );
		    } else {
		    	gc.fillText( "$" + NumberFormat.getNumberInstance(Locale.US).format(current_coins_a*500), 50, b_offset_y/2 + 70 );
		    }
		    
		    
		    
		    gc.drawImage(flock[story.team_index_a], 200,0, flock[story.team_index_a].getWidth() * 0.75, flock[story.team_index_a].getHeight() * 0.75);
		    
		    gc.setFill( Color.BLACK );
		    gc.setFont( theFont );
		    gc.fillText( "Team: " + story.team_id_a, 50, 40);
		    
		    draw_coinstack(story.coinstack_a, 50, b_offset_y/2, coin_display_count, coin_active_frame);
		    
		    /* Screen B */
		    
		    
		    gc.setFill( b_color );
			gc.fillRect(0, b_offset_y, CANVAS_WIDTH, CANVAS_HEIGHT);
		    
		    gc.setFill( gold );
		    gc.setFont( theFont );
		    if (transition_timer_on) {
		    	gc.fillText( "$" + NumberFormat.getNumberInstance(Locale.US).format(story.bid_b), 50, b_offset_y + b_offset_y/2 + 70 );
		    } else {
		    	gc.fillText( "$" + NumberFormat.getNumberInstance(Locale.US).format(current_coins_b*500), 50, b_offset_y + b_offset_y/2 + 70 );
		    }
			
		    gc.drawImage(flock[story.team_index_b], 200, b_offset_y + 0, flock[story.team_index_b].getWidth() * 0.75, flock[story.team_index_b].getHeight() * 0.75);
		    
		    gc.setFill( Color.BLACK );
		    gc.setFont( theFont );
		    gc.fillText( "Team: " + story.team_id_b, 50, b_offset_y + 40);
		    
		    draw_coinstack(story.coinstack_b, 50, b_offset_y + (b_offset_y/2), coin_display_count, coin_active_frame);
		    
		    /* Separators */
		    
		    gc.setFill( Color.BLACK );
		    gc.setStroke( Color.BLACK );
		    gc.setLineWidth(2);
		    
		    gc.strokeLine(0, b_offset_y, CANVAS_WIDTH, b_offset_y);
		    
		    
			break;
		case Play:
			if (!current_screenshots.isEmpty()) {

				
				Image screenshot_image = new Image(new ByteArrayInputStream(current_screenshot));//SwingFXUtils.toFXImage(ImageIO.read(current_screenshots.getFirst()));
				
				gc.drawImage(screenshot_image, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
				
				gc.setFill(Color.rgb(19, 23, 196, 0.5));
				gc.fillRect(0, 0, 200, 70);
				
			    gc.setFill( Color.BLACK );
			    gc.setFont( Font.font( "Roboto", FontWeight.BOLD, 28 ) );
			    gc.fillText( "Shooting", 75, 45);
				
				if (a_shooting) {
					gc.drawImage(flock[story.team_index_a], 10,  -10, flock[story.team_index_a].getWidth() * 0.3, flock[story.team_index_a].getHeight() * 0.3);
				} else {
					gc.drawImage(flock[story.team_index_b], 10,  -10, flock[story.team_index_b].getWidth() * 0.3, flock[story.team_index_b].getHeight() * 0.3);
				}
			}
			
			break;
		case Wait:
			break;
		}

	}
	
	private void draw_coinstack(int[][] coinstack, int offsetx, int offsety, int count, int active_frame) {
		//System.out.println(count + "  - " + active_frame);
		int total = 0;
		for (int y = 0; y < coinstack[0].length; y++) {
			for (int x = 0; x < coinstack.length; x++) {
				for (int h = 0; h < coinstack[y][x]; h++) {
					total++;
					if (total < count) {
						draw_coin(
								offsetx,
								offsety,
								x,
								y,
								h
						);
					} else if (total == count) {
						draw_coin(
								offsetx,
								offsety,
								x,
								y,
								h,
								active_frame
						);
					} else {
						return;
					}
				}
			}
		}
	}
	

	
	private void draw_coin(int offsetx, int offsety, int x, int y, int h) {
		gc.drawImage(coin, 
				offsetx + (x*22) + (y % 2) * 11,
				offsety + (y*10) - (h*4),
				40,
				40
		);
			
	}
	private void draw_coin(int offsetx, int offsety, int x, int y, int h, int frame) {
		draw_coin(offsetx,offsety,x,y,h+((25-frame)*2));
	}

	public void loadResources() {
		BufferedImage flock_sprite = null;
		try {
			coin = new Image(getClass().getResourceAsStream("goldCoin1.png"));
			
			flock_sprite = ImageIO.read(getClass().getResourceAsStream("flock.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		flock = new Image[8];
		flock[0] = SwingFXUtils.toFXImage(flock_sprite.getSubimage(0, 0, 210, flock_sprite.getHeight()), null);
		flock[1] = SwingFXUtils.toFXImage(flock_sprite.getSubimage(210, 0, 115, flock_sprite.getHeight()), null);
		flock[2] = SwingFXUtils.toFXImage(flock_sprite.getSubimage(330, 0, 100, flock_sprite.getHeight()), null);
		flock[3] = SwingFXUtils.toFXImage(flock_sprite.getSubimage(440, 0, 130, flock_sprite.getHeight()), null);
		flock[4] = SwingFXUtils.toFXImage(flock_sprite.getSubimage(570, 0, 90, flock_sprite.getHeight()), null);
		flock[5] = SwingFXUtils.toFXImage(flock_sprite.getSubimage(660, 0, 85, flock_sprite.getHeight()), null);
		flock[6] = SwingFXUtils.toFXImage(flock_sprite.getSubimage(745, 0, 100, flock_sprite.getHeight()), null);
		flock[7] = SwingFXUtils.toFXImage(flock_sprite.getSubimage(840, 0, 70, flock_sprite.getHeight()), null);
		
	}
}
