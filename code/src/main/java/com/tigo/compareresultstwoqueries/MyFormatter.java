/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tigo.compareresultstwoqueries;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.time.LocalDateTime; // Import the LocalDateTime class
import java.time.format.DateTimeFormatter; // Import the DateTimeFormatter class

/**
 * Log Formatter
 * @author Victor Hugo Gonzales
 * 
*/
public class MyFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
 /*       return record.getThreadID()+"::"+record.getSourceClassName()+"::"
                +record.getSourceMethodName()+"::"
                +new Date(record.getMillis())+"::"
                +record.getMessage()+"\n";
*/
    LocalDateTime myDateObj = LocalDateTime.now();
    DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

          return //record.getLongThreadID()+"::" +
                 myDateObj.format(myFormatObj)+"::"+
                 record.getLevel() + "::" +      
                 record.getMessage() +"\n"
                 //(record.getLevel().toString() == "SEVERE"? "::"+record.: "\n" )  
                  ;
          
    }
}
