package be.hogent.tarsos.peak;

public class Peak
{
	private double position;
	private double height;
	
    /**
     * Creates a new peak
     * @param position the position in cents
     * @param height the height of the peak
     */
    public Peak(double position, double height)
    {
    	this.position = position;
        this.height = height;        
    }

	/**
	 * The height of the peak (number of occurrences)
	 * @return the height of the peak
	 */
	public double getHeight() {
		return height;
	}

	/**
	 * The position of the peak in cents
	 * @return The position of the peak in cents
	 */
	public double getPosition() {
		return position;
	}
}