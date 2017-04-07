package edu.tum.uc.jvm.extractor;

import java.util.Map;

public interface IExtractor {
	/**
	 * Extracts necessary information from object obj
	 * and writes them into passed map m 
	 */
	public Map<?,?> extract(Object obj);

}
