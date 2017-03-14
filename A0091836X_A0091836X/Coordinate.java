public class Coordinate {
	public int x;
	public int y;

	public Coordinate(int x, int y){
		this.x = x;
		this.y = y;
	}

	public Coordinate(String str){
		String[] split = str.split(",");
		this.x = Integer.parseInt(split[0]);
		this.y = Integer.parseInt(split[1]);
	}

	public String getCoordinateString(){
		return x + "," + y;
	}

}