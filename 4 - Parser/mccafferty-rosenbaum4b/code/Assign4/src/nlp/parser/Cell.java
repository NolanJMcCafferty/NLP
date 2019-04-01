package nlp.parser;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Cell class responsible for a single table entry in the CKY table 
 * Holds a map from all constituents to its Entry
 * 
 * @author Daniel Rosenbaum
 * @author Nolan McCafferty
 */
public class Cell {

	protected HashMap<String, Entry> rules; // map from each constituent in the cell to its entry
	
	/**
	 * Create a new empty cell
	 */
	public Cell(){
		rules = new HashMap<String, Entry>();
	}
	
	/**
	 * Add in a new entry to the cell
	 * 
	 * @param rule - the constituent 
	 * @param entry - the corresponding entry
	 */
	public void addEntryToCell(String rule, Entry entry){
		rules.put(rule, entry);
	}
	
	/**
	 * Check to see if the constituent is in the cell
	 * 
	 * @param key
	 * @return boolean 
	 */
	public Boolean contains(String key){
		return rules.containsKey(key);
	}
	
	public Entry getEntry(String key){
		return rules.get(key);
	}
	
	/**
	 * Recursively add unary rules 
	 * 
	 * @param entry - the entry we are updating
	 * @param rule - the LHS we just added 
	 */
	public void addUnaryRules(HashMap<String, ArrayList<GrammarRule>> unaryRules, Entry entry, String rule, double weight){
		
		// see if our constituent is part of any unary rules
		if (unaryRules.containsKey(rule)){
			
			// go through all the unary rules that work
			for (GrammarRule unaryRule : unaryRules.get(rule)){
				
				// put needed info in local variables
				String unaryLHS = unaryRule.getLhs();
				double newWeight = unaryRule.getWeight() + weight;
			
				// see if we already have this rule in the cell rules
				if (rules.containsKey(unaryLHS)){
					
					// get the current entry that we have
					Entry currentEntry = rules.get(unaryLHS);
					
					// only update the entry if it is better probability
					if (currentEntry.weight < newWeight)
						currentEntry.update(newWeight, entry, null);
				}
				
				// otherwise we need to add the rule and recurse on this new rule
				else {
					Entry newEntry = new Entry(unaryLHS, newWeight, entry, null);
					addEntryToCell(unaryLHS, newEntry);
					
					// recursively add unary rules
					addUnaryRules(unaryRules, entry, unaryLHS, newWeight);
				}
			}
		}
	}
}
