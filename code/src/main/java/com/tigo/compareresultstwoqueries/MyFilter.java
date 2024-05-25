/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tigo.compareresultstwoqueries;


import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Log Filter
 * @author Victor Hugo Gonzales
*/
public class MyFilter implements Filter {

	@Override
	public boolean isLoggable(LogRecord log) {
		if(log.getLevel() == Level.CONFIG) return false;
		return true;
	}

}