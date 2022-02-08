package com.example.mystego.utils;

import java.util.ArrayList;
import java.util.List;

public final class Utils {

	/**
	 *
	 * @param lst lista di interi
	 * @return stringa
	 *
	 * Converte ina lista di interi in stringa
	 */
	public static String listToString(List<Integer> lst) {
		StringBuilder str = new StringBuilder();
		
		for(Integer x : lst)
			str.append(x).append(" ");
			
		return str.toString();
	}

	/**
	 *
	 * @param str stringa
	 * @return lista di interi
	 *
	 * Converte una stringa in una lista di interi
	 */
	public static List<Integer> stringToList(String str){
		List<Integer> lst =  new ArrayList<>();
		String[] string = str.split(" ");
		
		for(String x : string)
			lst.add(Integer.parseInt(x));
		
		return lst;
	}

	/**
	 *
	 * @param lst lista di interi
	 * @return boolean true o false
	 *
	 * controlla se la size in byte della lista è corretta
	 */
	public static boolean itsByteSizeCorrect(List<Integer> lst) {
		int size = Utils.listToString(lst).getBytes().length;
		return size > 245;
	}

}
