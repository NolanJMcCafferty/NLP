package nlp.lm;

/**
 * Class for bigram language model with lambda smoothing
 * 
 * @author Nolan McCafferty
 * @author Daniel Rosenbaum
 *
 */
public class LambdaLMModel extends LMBase implements LMModel {

	// class variable for lambda
	private final double lambda;

	/**
	 * LambdaLMModel Constructor
	 * 
	 * Given a file (a corpus) and a lambda will train a Language Model using Bigrams 
	 * 
	 * @param filename
	 * @param lambda
	 */
	public LambdaLMModel(String filename, Double lambda){
		this.lambda = lambda;
		trainModel(filename);
	}
	

	/**
	 * Given a bigram, return its probability
	 * p(a|b) = count(ab) + lambda/count (a) + (vocab size * lambda)
	 * If a count is not found then its value is 0
	 * @param first
	 * @param second
	 * @return probability
	 */
	@Override
	public double getBigramProb(String first, String second) {
		double bigramProb = 0;
		double lambdaVocabSize = lambda * vocabulary.size();
					
		// get the total number of times this first word starts a bigram
		double total = bigramTotals.get(first) + lambdaVocabSize;
		
		// if the total bigram has been seen 
		if (bigramCounts.get(first).containsKey(second)){
			
			// just return the number of times for the bigram + lambda/total bigrams
			bigramProb = (bigramCounts.get(first).get(second) + lambda)/total;
		}
		
		// we have seen the first word but not the second as a bigram
		else {
			bigramProb = lambda/total;
		}
		
		return bigramProb;
	}
	

	/**
	 * LAMBDA LANGUAGE MODEL
	 * Main method used for testing, training and evaluating! 
	 * @param args
	 */
	public static void main(String[] args){
		
		// train the models
		for (int l = 0; l < 8; l++) {
			double lambda = 1/(Math.pow(10, l));
			System.out.println("Training Lambda Language Model with lambda = " + lambda + " ...");
			LambdaLMModel model = new LambdaLMModel("src/sentences.full_training", lambda);
			
			// generate greedy sentence
			System.out.println(model.generateGreedySentence());
			
			// generate sampled sentences
			for (int i = 0; i < 5; i++) {
				System.out.println(model.generateSamplingSentence());
			}
			// get perplexity
			System.out.println("Perplexity: " + model.getPerplexity("src/sentences.testing"));
		}
	}
}