package de.tu_bs.cs.isf.cbc.tool.helper;

import java.io.File;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.graphiti.mm.pictograms.Diagram;

import de.tu_bs.cs.isf.cbc.cbcmodel.CbCFormula;
import de.tu_bs.cs.isf.cbc.util.FileUtil;
import de.tu_bs.cs.isf.commands.toolbar.handler.family.MetaClass;
import diagnostics.DataCollector;
import diagnostics.DataType;

/**
 * Util class for testing purposes.
 * @author Fynn
 */
public class FileHandler {
	private static FileHandler instance;
	
	public static FileHandler getInstance() {
		if (instance == null) {
			instance = new FileHandler();
		}
		return instance;
	}
	
	private URI projectPath = null;
	
	public void clearLog(final URI projectPath) {
		instance.projectPath = projectPath;
		var logFile = new File(FileUtil.getProjectLocation(projectPath) + "\\tests\\log.txt");
		if (logFile.exists()) {
			logFile.delete();
		} 
	}
	
	public void log(final URI projectPath, String text) {
		instance.projectPath = projectPath;
		try {
			var logFile = new File(FileUtil.getProjectLocation(projectPath) + "\\tests\\log.txt");
			if (!logFile.exists()) {
				logFile.createNewFile();
			} 
			if (text.charAt(text.length()-1) != '\n') text += "\n";
			Files.write(Paths.get(logFile.getPath()), text.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
		}
	}
	
	public void log(String text) {	
		if (instance.projectPath == null) {
			return;
		}
		try {
			var logFile = new File(FileUtil.getProjectLocation(instance.projectPath) + "\\tests\\log.txt");
			if (!logFile.exists()) {
				logFile.createNewFile();
			} 
			if (text.charAt(text.length()-1) != '\n') text += "\n";
			Files.write(Paths.get(logFile.getPath()), text.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
		}
	}
		
	public boolean writeToFile(String location, String fileName, String content) {
		try {
			var dir = new File(location);	
			if (!dir.exists()) {
				dir.mkdirs();
			}
			var javaFile = new File(location + "/" + fileName);
			if (!javaFile.exists()) {
				javaFile.createNewFile();
			} 
			Files.write(Paths.get(javaFile.getPath()), new byte[] {}, StandardOpenOption.TRUNCATE_EXISTING);
			Files.write(Paths.get(javaFile.getPath()), content.getBytes(), StandardOpenOption.WRITE);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean createFile(final URI projectPath, String className, String code) {
		return createFile(projectPath, "tests", className + ".java", code);
	}
	
	public boolean createFile(final URI projectPath, final String folderName, final String fileName, final String code) {
		var project = FileUtil.getProject(projectPath);
		var folder = project.getFolder(folderName);
		try {
			if (!folder.exists()) {
				folder.create(true, true, null);
			}
			var javaFile = new File(folder.getLocation().toOSString() + "/" + fileName);
			if (!javaFile.exists()){
				javaFile.createNewFile();
		    }
			writeToFile(folder.getLocation().toOSString(), fileName, code);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (CoreException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean deleteFolder(final URI projectPath, final String folderName) {
			var project = FileUtil.getProject(projectPath);
			var folder = project.getFolder(folderName);
			if (!folder.exists()) {
				return false;
			}
			var files = FileUtil.getFiles(folder, "");
			for (var file : files) {
				deleteSpecificFile(file.getLocation().toOSString());
			}
			var folderObj = new File(FileUtil.getProjectLocation(projectPath) + "/" + folderName);
			try {
				Files.delete(Paths.get(folderObj.getPath()));
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
	}
	
	public boolean createFolder(final URI projectPath, final String folderName) {
			var project = FileUtil.getProject(projectPath);
			try {
				project.getFolder(folderName).create(true, true, null);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			return true;
	}
	
	public boolean deleteSpecificFile(final String fullFilePath) {
		try {
			var javaFile = new File(fullFilePath);
			if (!javaFile.exists()){
				return false;
		    }
			Files.delete(Paths.get(javaFile.getPath()));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean deleteFile(final URI projectPath, String className) {
		try {
			var dir = new File(FileUtil.getProjectLocation(projectPath) + "\\tests");	
			if (!dir.exists()) {
				return false;
			}
			var javaFile = new File(FileUtil.getProjectLocation(projectPath) + "\\tests\\" + className + ".java");
			if (!javaFile.exists()){
				return false;
		    }
			Files.delete(Paths.get(javaFile.getPath()));
			javaFile = new File(FileUtil.getProjectLocation(projectPath) + "\\tests\\" + className + ".class");
			if (javaFile.exists()){
				Files.delete(Paths.get(javaFile.getPath()));
		    }
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean writeToFile(final URI projectPath, final String className, String content) {
		writeToFile(FileUtil.getProjectLocation(projectPath) + "\\tests", className, content);
		return true;
	}
	
	private IFile findDiagram(IContainer folder, String diagramName) {
		try {
			IResource[] members = folder.members();
			for (final IResource resource : members) {
				if (resource instanceof IContainer) {
					IFile foundFile = findDiagram((IContainer) resource, diagramName);
					if (foundFile != null) {
						return foundFile;
					}
				} else if (resource instanceof IFile) {

					final IFile file = (IFile) resource;
					if (file.getName().equals(diagramName + ".diagram")) {
						return file;
					}
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Diagram loadDiagramFromClass(final URI projectPath, final String folderName, final String className, final String diagramName) {
		if (className.isBlank() || diagramName.isBlank()) {
			return null;
		}
		final ResourceSet rSet = new ResourceSetImpl();
		final IContainer folder = FileUtil.getProject(projectPath).getFolder(folderName);
		if (folder == null) {
			return null;
		}
		final IFile file = FileUtil.getProject(projectPath).getFolder("src/" + className).getFile(diagramName + ".diagram");
		if (file == null) {
			return null;
		}
		return GetDiagramUtil.getDiagramFromFile(file, rSet);
	}
	
	public Diagram loadDiagramFromClass(final URI projectPath, final String folderName, final String diagramName) {
		if (diagramName.isBlank()) {
			return null;
		}
		final ResourceSet rSet = new ResourceSetImpl();
		final IContainer folder = FileUtil.getProject(projectPath).getFolder(folderName);
		if (folder == null) {
			return null;
		}
		final IFile file = findDiagram(folder, diagramName);
		if (file == null) {
			return null;
		}
		return GetDiagramUtil.getDiagramFromFile(file, rSet);
	}
	
	public boolean isSPL(final URI projectPath) {
		return isSPL(projectPath, false);
	}
	
	public boolean isSPL(final URI uri, boolean isFile) {
		final var project = FileUtil.getProject(uri);
		try {
			if (project.getNature("de.ovgu.featureide.core.featureProjectNature") != null) {
				if (isFile && uri.path().contains(MetaClass.FOLDER_NAME)) {
					return false;
				}
				return true;
			} else {
				return false;
			}
		} catch (CoreException | NullPointerException e) {
			e.printStackTrace();
			return false;
		}	
	}
	
	public boolean saveConfig(final URI projectPath, final CbCFormula formula, final Features features, boolean isTestCase) {
		final String className = ClassHandler.getClassName(formula);
		final String currentConfigName = features.getCurConfigName();
		String code;

		if (isTestCase) {
			code = ClassHandler.classExists(projectPath, className + "Test");
		} else {
			code = ClassHandler.classExists(projectPath, className);
		}
		if (!createFile(projectPath, currentConfigName, code)) {
			return false;
		}
		return true;
	}
	
	private String getClassPath(final URI projectPath) {
		var path = projectPath.toPlatformString(true);
		path = path.substring(path.indexOf("/") + 1, path.lastIndexOf("/") + 1);
		path = path.substring(path.indexOf("/") + 1, path.length());
		return path;
	}
	
	public void saveDiagnosticData(final URI projectPath, final DataCollector dataCollector) {
		var path = getClassPath(projectPath);
		final String diagFolder =  path + dataCollector.getFolderName();
		createFolder(projectPath, diagFolder);
		if (dataCollector.getType() != DataType.PATH) { 
			// TODO Differentiate between SPLs and None SPLs
			createFile(projectPath, diagFolder, "DiagnosticData", dataCollector.getTestCaseRep()); 
			return;
		}
		if (!isSPL(projectPath)) {
			createFile(projectPath, diagFolder, "DiagnosticData", dataCollector.getPathsRep()); 
		}
		for (var config : dataCollector.getConfigNames()) {
			createFolder(projectPath, diagFolder + "/" + config);
			createFile(projectPath, diagFolder + "/" + config, "DiagnosticData", dataCollector.getConfigRep(config));
		}
	}
}
