
public class Team {
	private int index;
	private int score;
	private int id;
	private boolean on_site = true;
	
	public Team (int first, int score) {
		this.score = score;
		this.id = first;
	}
	
	public int getScore() {
		return score;
	}
	public int getId() {
		return id;
	}

	public void addPoints(int points) {
		score += points;
	}
	
}
