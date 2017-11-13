package jnachos.kern.mem;

public interface PageReplacementAlgorithm {

	/**
	 * 
	 * @return A physical page frame that should be evicted.
	 */
	public int chooseVictimPage();
}
