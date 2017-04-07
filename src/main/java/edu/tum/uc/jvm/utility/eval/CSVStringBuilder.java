package edu.tum.uc.jvm.utility.eval;

/**
 * This class is a wrapper around a <code>StringBuilder</code> adding CSV separators and quotation marks, whenever a new
 * line or a coherent string is inserted.
 * 
 * @author alex
 *
 */
public class CSVStringBuilder {

    /**
     * The <code>StringBuilder</code> instance.
     */
    private StringBuilder sb = new StringBuilder();

    /**
     * Clears all characters.
     * @return This instance.
     */
    public CSVStringBuilder reset() {
	sb = new StringBuilder();
	return this;
    }

    /**
     * Inserts "\n" (new line)
     * @return This instance.
     */
    public CSVStringBuilder newLine() {
	sb.append("\n");
	return this;
    }

    /**
     * Appends a given string and wraps it in " and "; .
     * @param s The string to append.
     * @return This instance.
     */
    public CSVStringBuilder append(String s) {
	sb.append("\"");
	sb.append(s);
	sb.append("\"");
	sb.append(";");
	return this;
    }

    /**
     * Returns a string conforming to CSV format.
     */
    public String toString() {
	return sb.toString();
    }

}
