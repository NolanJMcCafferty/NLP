package nlp.parser;

/**
 * Entry class responsible for storing a single rule entry in the CKY table
 * Includes its consituent (rule), the weight and reference pointers to its children 
 * 
 * @author Daniel Rosenbaum
 * @author Nolan McCafferty
 *
 */
public class Entry {
	
	protected String rule; // the constituent in this entry
	protected double weight; // the probability
	protected Entry leftChild; 
	protected Entry downChild;
	
	
	/**
	 * Create a new entry given the needed info
	 * 
	 * @param r
	 * @param w
	 * @param lChild
	 * @param dChild
	 */
	public Entry(String r, double w, Entry lChild, Entry dChild){
		rule = r;
		weight = w;
		leftChild = lChild;
		downChild = dChild; 
	}

	/**
	 * Update the values for the entry
	 *  
	 * @param w
	 * @param lChild
	 * @param dChild
	 */
	public void update(double w, Entry lChild, Entry dChild){
		weight = w;
		leftChild = lChild;
		downChild = dChild;
	}
}
