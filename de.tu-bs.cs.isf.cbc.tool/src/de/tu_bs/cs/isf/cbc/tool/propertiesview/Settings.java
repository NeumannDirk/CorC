package de.tu_bs.cs.isf.cbc.tool.propertiesview;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import de.tu_bs.cs.isf.cbc.tool.exceptions.SettingsException;

public final class Settings implements Serializable {
	private static Settings instance;
    private static final long serialVersionUID = 1L;
    private boolean counterExamples;
    private boolean testWarnings;
    private static final String filePath = System.getProperty("user.dir") + "/settings.ser";
    
    public static Settings get() throws SettingsException {
    	if (instance == null) {
    		read();
    	}
    	return instance;
    }
    
    public static void read() throws SettingsException {
        try (FileInputStream fileIn = new FileInputStream(filePath);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn)) {
        	instance = (Settings)objectIn.readObject();
        } catch (Exception e) {
            throw new SettingsException("Cannot read settings from '" + filePath + "'.");
        }
    }
    
    public static boolean canRead() {
        try (FileInputStream fileIn = new FileInputStream(filePath);
                ObjectInputStream objectIn = new ObjectInputStream(fileIn)) {
                return true;
            } catch (Exception e) {
                return false;
            }
    }

    private Settings() {
    }
    
    public void setTestWarnings(boolean testWarnings) {
    	instance.testWarnings = testWarnings;
    }
    
    public boolean testWarningsEnabled() {
    	return instance.testWarnings;
    }

    public boolean getCounterExamples() {
        return instance.counterExamples;
    }

    public void setCounterExamples(boolean counterExamples) {
    	instance.counterExamples = counterExamples;
    }

    public void save() {
        try (FileOutputStream fileOut = new FileOutputStream(filePath);
             ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {
            objectOut.writeObject(instance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}