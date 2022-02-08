package com.example.mystego.moduli.progetto.modulo_compressione;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LZW {
	/**
	 *
	 * @param text stringa da comprimere
	 * @return lista di interi
	 *
	 * Comprime una stringa in un elenco di simboli.
	 */
	public static List<Integer> encode(String text){
		//costruzione del dizionario
		int dictSize = 256;
		Map<String,Integer> dizionario = new HashMap<>();
		for ( int i = 0; i < dictSize ; i++)
			dizionario.put(String.valueOf((char) i), i);
		
		String foundChar = "";
		List<Integer> result = new ArrayList<>();
		for (char c: text.toCharArray()) {
		    String charsToAdd = foundChar + c;
		    if(dizionario.containsKey(charsToAdd))
		    	foundChar = charsToAdd;
		    else {
		    	result.add(dizionario.get(foundChar));
				// aggiunge charsToAdd al dizionario
		    	dizionario.put(charsToAdd, dictSize++);
		    	foundChar = String.valueOf(c);
		    }
		}
		// manda i output foundChar
		if(!foundChar.isEmpty())
			result.add(dizionario.get(foundChar));
		
		return result;
	}

	/**
	 *
	 * @param compressed lista con i valori compressi
	 * @return stringa decompressa
	 *
	 * Decomprime una lista di interi precedentemente compressa
	 */
	public static String decode(List<Integer> compressed){
		// costruisce il dizionario
		int dictSize = 256;
		Map<Integer,String> dizionario = new HashMap<>();
		for ( int i = 0; i < dictSize ; i++)
			dizionario.put(i,String.valueOf((char) i));
		
		String caratteri = String.valueOf((char) compressed.remove(0).intValue());
		StringBuilder result = new StringBuilder(caratteri);
		
		for (int code: compressed) {
		    String entry = dizionario.containsKey(code) 
		    		? dizionario.get(code) : caratteri+ caratteri.charAt(0);
		    result.append(entry);
			assert entry != null;
			// aggiunge caratteri + entry[0] al dizionario
			dizionario.put(dictSize++, caratteri + entry.charAt(0));
		    caratteri = entry;
		}
	
		return result.toString();
		
	}

}
