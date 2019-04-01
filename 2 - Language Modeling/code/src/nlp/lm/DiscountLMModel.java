package nlp.lm;

/**
 * Class for bigram language model with absolute discount smoothing
 * 
 * @author Nolan McCafferty
 * @author Daniel Rosenbaum
 *
 */
public class DiscountLMModel extends LMBase implements LMModel {

	private double discount;
	
	public DiscountLMModel(String filename, double discount){
		trainModel(filename);
		this.discount = discount;

	}

	/**
	 * Given a bigram, return its probability
	 * If the bigram has been seen, P(a | b) = (C(ab) - D)/C(b)
	 * Else, P(a | b) = alpha(b) * P(a)
	 * @param first
	 * @param second
	 * @return probability
	 */
	@Override
	public double getBigramProb(String first, String second) {
		double bigramProb = 0.0;
		
		// if we have seen the bigram (i.e. count(xy) > 0)
		if (bigramCounts.containsKey(first) && bigramCounts.get(first).containsKey(second)){
			bigramProb = (bigramCounts.get(first).get(second) - discount)/(bigramTotals.get(first));
		}
		// alpha (x) * P_absolute(y)
		else {
			// calculate reserved mass
		
			// all words at this point are either seen or have prev. been replaced by <unk>
			double options = bigramCounts.get(first).size();
			double total = bigramTotals.get(first);
			double reserved_mass = (options*discount)/total;
			
			// calculate denominator (1 - sum of P > 0)
			// for each second word bigram option from first 
			double positiveProbs = 0.0;	
			for (String possibleSecond : bigramCounts.get(first).keySet()){
				positiveProbs += unigramCounts.get(possibleSecond)/totalNumWords;
			}

			double alpha =  reserved_mass/(1 - positiveProbs);	
			bigramProb = alpha * unigramCounts.get(second)/totalNumWords;
		}
		return bigramProb;
	}
	
	/**
	 * ABSOLUTE DISCOUNTING LANGUAGE MODEL
	 * Main method used for testing, training and evaluating! 
	 * calculates the perplexities for each discount value
	 * generates sentences for best discount model
	 * @param args
	 */
	public static void main(String[] args){
		// train the models
		double[] discounts = {.99, .9, .75, .5, .25, .1};
		
		for (int i = 0; i < discounts.length; i++) {
			System.out.println("Training Discount Language Model with discount = " + discounts[i] + " ...");
			DiscountLMModel model = new DiscountLMModel("src/sentences", discounts[i]);
			
			// get perplexity
			System.out.println("Perplexity: " + model.getPerplexity("src/sentences"));
		}
		
		// generate sentences with best discount model
		System.out.println("Training Discount Language Model with discount = .5 ...");
		DiscountLMModel model = new DiscountLMModel("src/sentences.training", 0.5);
		
		// generate greedy sentence
		System.out.println(model.generateGreedySentence());

		// generate sampled sentences
		for (int k = 0; k < 5; k++) {
			System.out.println(model.generateSamplingSentence());
		}
		
	}
}
