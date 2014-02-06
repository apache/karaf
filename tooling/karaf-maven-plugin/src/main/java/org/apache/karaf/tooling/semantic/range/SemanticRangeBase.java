package org.apache.karaf.tooling.semantic.range;

public abstract class SemanticRangeBase implements SemanticRange {

	public boolean equals(Object other) {
		if (other instanceof SemanticRange) {
			SemanticRange that = (SemanticRange) other;
			return this.getLowerBound().equals(that.getLowerBound())
					&& this.getUpperBound().equals(that.getUpperBound());
		}
		return false;
	}

}
