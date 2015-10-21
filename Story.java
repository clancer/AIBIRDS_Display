import java.util.ArrayList;
import java.util.LinkedList;


public class Story {
	
	
	public LinkedList<String> dirs;
	
	public int team_index_a = 0;
	public int team_index_b = 1;
	public int team_id_a = 123123;
	public int team_id_b = 456456;
	
	public int bid_a = 0;
	public int bid_b = 0;
	
    public int[][] coinstack_a = {
    		{0, 0, 0},
    		{0, 0, 0},
    		{0, 0, 0}
    };
    public int[][] coinstack_b = {
    		{0, 0, 0},
    		{0, 0, 0},
    		{0, 0, 0}
    };

	public boolean a_won_bid;

	public boolean a_goes_first;
    
    public Story(int first_id, int second_id, int bid_a, int bid_b, boolean a_goes_first, LinkedList<String> screenshot_dirs) {
    	dirs = screenshot_dirs;
    	team_id_a = first_id;
    	team_id_b = second_id;

    	team_index_a = MainDisplay.teamIndex.indexOf(team_id_a);

    	team_index_b = MainDisplay.teamIndex.indexOf(team_id_b); 
    	
    	this.bid_a = bid_a;
    	int pos = 0;
    	while (bid_a >= 500) {
    		int x = pos % 3;
    		int y = pos / 3;
    		if (Math.random() >= 0.5) {
    			bid_a -= 500;
    			coinstack_a[x][y]++;
    		}
    		pos = (pos+1) % 9;
    	}
    	this.bid_b = bid_b;
    	pos = 0;
    	while (bid_b >= 500) {
    		int x = pos % 3;
    		int y = pos / 3;
    		if (Math.random() >= 0.5) {
    			bid_b -= 500;
    			coinstack_b[x][y]++;
    		}
    		pos = (pos+1) % 9;
    	}
    	this.a_goes_first = a_goes_first;
    }
}
