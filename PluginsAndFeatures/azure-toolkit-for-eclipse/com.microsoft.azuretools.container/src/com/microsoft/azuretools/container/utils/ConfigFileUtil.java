package com.microsoft.azuretools.container.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public class ConfigFileUtil {
    private static String configFileName = "azuretools-config.properties";
    
    public static Properties loadConfig(IProject project) {
        Properties prop = new Properties();
        IFile file = project.getFile(configFileName);
        if(!file.exists()) return prop;
        try {
            prop.load(file.getContents());
        } catch (IOException | CoreException e) {
            e.printStackTrace();
        }
        return prop;
    }
    
    public static void saveConfig(IProject project, Properties prop){
        IFile file = project.getFile(configFileName);
        
        try {
            ByteArrayOutputStream ous = new ByteArrayOutputStream();
            prop.store(ous, null);
            file.create(new ByteArrayInputStream(ous.toByteArray()), true, null);
        } catch (IOException | CoreException io) {
            io.printStackTrace();
        } 
    }
    
}
