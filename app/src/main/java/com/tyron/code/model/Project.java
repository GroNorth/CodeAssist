package com.tyron.code.model;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import com.tyron.code.compiler.LibraryChecker;
import com.tyron.code.util.StringSearch;
import android.util.Log;
import javax.lang.model.SourceVersion;
import com.tyron.code.util.Decompress;
import com.tyron.code.ApplicationLoader;

/**
 * Class for storing project data, directories and files
 */
public class Project {
    
    public final File mRoot;
    
    public Map<String, File> javaFiles = new HashMap<>();
    public List<String> jarFiles = new ArrayList<>();

    private Set<File> libraries = new HashSet<>();
    
    /**
     * Creates a project object from specified root
     */
    public Project(File root) {
        mRoot = root;
        
        findJavaFiles(new File(root, "app/src/main/java"));
    }
    
    private void findJavaFiles(File file) {
        File[] files = file.listFiles();
        
        if (files != null) {
            for (File child : files) {
                if (child.isDirectory()) {
                    findJavaFiles(child);
                } else {
                    if (child.getName().endsWith(".java")) {
                        String packageName = StringSearch.packageName(child);
                        Log.d("PROJECT FIND JAVA", "Found " + child.getAbsolutePath());
                        if (packageName.isEmpty()) {
                            Log.d("Error package empty", child.getAbsolutePath());
                        } else {
                            if (SourceVersion.isName(packageName + "." + child.getName().replace(".java", ""))) {
                                javaFiles.put(packageName + "." + child.getName().replace(".java", ""), child);
                            }
                        }
                    }
                }
            }
        }
    }
    
    public Map<String, File> getJavaFiles() {
        if (javaFiles.isEmpty()) {
            findJavaFiles(getJavaDirectory());
        }
        return javaFiles;
    }

    public void searchLibraries() {
        libraries.clear();

        File libPath = new File(getBuildDirectory(), "libs");
        File[] files = libPath.listFiles();
        if (files != null) {
            for (File lib : files) {
                if (lib.isDirectory()) {
                    File check = new File(lib, "classes.jar");
                    if (check.exists()) {
                        libraries.add(check);
                    }
                }
            }
        }
    }
    public Set<File> getLibraries() {
        if (libraries.isEmpty()) {
            LibraryChecker checker = new LibraryChecker(this);
            checker.check();

            searchLibraries();
        }
        return libraries;
    }

    /**
     * Clears all the cached files stored in this project, the next time ths project
     * is opened, it will get loaded again
     */
    public void clear() {
        javaFiles.clear();
        libraries.clear();
    }
    /**
     * Used to check if this project contains the required directories
     * such as app/src/main/java, resources and others
     */
    public boolean isValidProject() {
        File check = new File(mRoot, "app/src/main/java");

        if (!check.exists()) {
            return false;
        }

        if (!getResourceDirectory().exists()) {
            return false;
        }

        return true;
    }
    
    /**
     * Creates a new project configured at mRoot, returns true if the project
     * has been created, false if not.
     */
    public boolean create() {
        
        // this project already exists
        if (isValidProject()) {
            return false;
        }
        
        File java = getJavaDirectory();
        
        if (!java.mkdirs()) {
            return false;
        }
        
		if (!getResourceDirectory().mkdirs()) {
			return false;
		}
		
        Decompress.unzipFromAssets(ApplicationLoader.applicationContext, "test_project.zip",
                java.getAbsolutePath());
		
        if (!getLibraryDirectory().mkdirs()) {
            return false;
        }

        return getBuildDirectory().mkdirs();
    }
	
	public File getResourceDirectory() {
		return new File(mRoot, "app/src/main/res");
	}
    
    public File getJavaDirectory() {
        return new File(mRoot, "app/src/main/java");
    }
    
    public File getLibraryDirectory() {
        return new File(mRoot, "app/libs");
    }
    
    public File getBuildDirectory() {
        return new File(mRoot, "app/build");
    }

    public File getManifestFile() {
        return new File(mRoot, "app/src/main/AndroidManifest.xml");
    }
}
