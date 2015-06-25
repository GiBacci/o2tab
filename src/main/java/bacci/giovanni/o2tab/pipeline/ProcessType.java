package bacci.giovanni.o2tab.pipeline;

public enum ProcessType {
	POOLING("Pooling"),
	DEREPLICATION("Dereplication"),
	ASSEMBLY("Assembling"),
	OTUCLUST("OTU clustering"),
	MAPPING("Read mapping"),
	TABLING("OTU tabling");
	
	private String name;
	
	private ProcessType(String name){
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() {
		return name;
	}
	
	
}
