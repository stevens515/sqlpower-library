package ca.sqlpower.util;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

import ca.sqlpower.enterprise.client.SPServerInfo;
import ca.sqlpower.object.CleanupExceptions;
import ca.sqlpower.object.SPChildEvent;
import ca.sqlpower.object.SPListener;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.util.UserPrompter.UserPromptOptions;
import ca.sqlpower.util.UserPrompter.UserPromptResponse;
import ca.sqlpower.util.UserPrompterFactory.UserPromptType;

public class SQLPowerUtils {
	
	private static final Logger logger = Logger.getLogger(SQLPowerUtils.class);
	
    /**
     * Checks if the two arguments o1 and o2 are equal to each other, either because
     * both are null, or because o1.equals(o2).
     * 
     * @param o1 One object or null reference to compare
     * @param o2 The other object or null reference to compare
     */
	public static boolean areEqual (Object o1, Object o2) {
		if (o1 == o2) {
			// this also covers (null == null)
			return true;
		} else {
			if (o1 == null || o2 == null) {
				return false;
			} else {
				// pass through to object equals method
				return o1.equals(o2);
			}
		}
	}
	
	/**
	 * Searches through the tree recursively to find if the spo 
	 * is part of the hierarchy.
	 */
	public static boolean hierarchyContains(SPObject root, SPObject child) {
		return SQLPowerUtils.getAncestorList(child).contains(root);
	}

	/**
	 * Replaces double quotes, ampersands, and less-than signs with
	 * their character reference equivalents.  This makes the returned
	 * string be safe for use as an XML content data or as an attribute value
	 * enclosed in double quotes. From the XML Spec at http://www.w3.org/TR/REC-xml/#sec-predefined-ent:
	 * 4.6 Predefined Entities
	 * "Definition: Entity and character references may both be used to escape the left angle bracket, ampersand,
	 * and other delimiters. A set of general entities (amp, lt, gt, apos, quot) is specified for this purpose...]
	 * All XML processors must recognize these entities whether they are declared or not. For interoperability,
	 * valid XML documents should declare these entities..."
	 * 
	 * Also escapes newlines because we need to preserve them in remarks.
	 */
	public static String escapeXML(String src) {
	    if (src == null) return "";
		StringBuffer sb = new StringBuffer(src.length()+10);  // arbitrary amount of extra space
		char ch;
	    
		for (int i = 0, n = src.length(); i < n; i++) {
			ch = src.charAt(i);
	        
	        if (ch == '\'') {
				sb.append("&apos;");
	        } else if (ch == '"') {
				sb.append("&quot;");
	        } else if (ch == '&') {
				sb.append("&amp;");
	        } else if (ch == '<') {
				sb.append("&lt;");
	        } else if (ch == '>') {
				sb.append("&gt;");
	        } else if (ch == 0x03 || ch == 0x1a) {
	        	logger.info("Stripping out illegal characters from " + src + 
	        			" as it will cause the XML to fail.");
	        } else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}
	
	public static String unescapeXML(String src) {
	    if (src == null) return "";
		StringBuffer sb = new StringBuffer(src.length()+10);  // arbitrary amount of extra space
		char ch;
	    
		StringBuffer encoding = null;
		for (int i = 0, n = src.length(); i < n; i++) {
			ch = src.charAt(i);
	        
			if (ch == '&') {
				encoding = new StringBuffer("&");
			} else if (encoding != null && ch == ';') {
				encoding.append(ch);
				if (encoding.toString().equals("&apos;")) {
					sb.append('\'');
				} else if (encoding.toString().equals("&quot;")) {
					sb.append('"');
				} else if (encoding.toString().equals("&amp;")) {
					sb.append('&');
				} else if (encoding.toString().equals("&lt;")) {
					sb.append('<');
				} else if (encoding.toString().equals("&gt;")) {
					sb.append('>');
				} else {
					logger.info("Could not find an encoding for " + encoding.toString());
				}
				encoding = null;
			} else if (encoding != null) {
				encoding.append(ch);
			} else {
				sb.append(ch);
			}
//	        if (ch == '\'') {
//				sb.append("&apos;");
//	        } else if (ch == '"') {
//				sb.append("&quot;");
//	        } else if (ch == '&') {
//				sb.append("&amp;");
//	        } else if (ch == '<') {
//				sb.append("&lt;");
//	        } else if (ch == '>') {
//				sb.append("&gt;");
//	        } else if (ch == 0x03 || ch == 0x1a) {
//	        	logger.info("Stripping out illegal characters from " + src + 
//	        			" as it will cause the XML to fail.");
//	        } else {
//				sb.append(ch);
//			}
		}
		return sb.toString();
	}
	
	/**
	 * Replaces double quotes, ampersands, less-than and greater-than signs with
	 * their character reference equivalents. This makes the returned string be
	 * safe for use as an HTML content data.
	 * 
	 */
	public static String escapeHTML(String src) {
	    if (src == null) return "";
		StringBuffer sb = new StringBuffer(src.length()+10);  // arbitrary amount of extra space
		char ch;
	    
		for (int i = 0, n = src.length(); i < n; i++) {
			ch = src.charAt(i);
	        
	        if (ch == '"') {
				sb.append("&quot;");
	        } else if (ch == '&') {
				sb.append("&amp;");
	        } else if (ch == '<') {
				sb.append("&lt;");
	        } else if (ch == '>') {
				sb.append("&gt;");
	        } else if (ch == 0x03 || ch == 0x1a) {
	        	logger.info("Stripping out illegal characters from " + src + 
	        			" as it will cause the XML to fail.");
	        } else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	/**
	 * Escapes newlines because we need to preserve them in remarks and can be used elsewhere.
	 */
	public static String escapeNewLines(String src) {
	    if (src == null) return "";
		StringBuffer sb = new StringBuffer(src.length()+10);  // arbitrary amount of extra space
		char ch;
	    
		for (int i = 0, n = src.length(); i < n; i++) {
			ch = src.charAt(i);
	        
	        sb.append(ch == '\n' ? "&amp;crlf;" : ch);
		}
		return sb.toString();
	}
	
	/**
	 * Unescapes the clrf entities put in by the escapeXML function.
	 */
	public static String unEscapeNewLines(String src) {
	    if (src == null) return "";
		StringBuffer sb = new StringBuffer(src.length()+10);  // arbitrary amount of extra space
		char ch;
	    
		for (int i = 0, n = src.length(); i < n; i++) {
			ch = src.charAt(i);
	        
	        if (ch == '&') {
	        	if (src.length() - i > 6 && "&crlf;".equals(src.substring(i,i+6))) {
	        		sb.append("\n");
	        		i += 5;
				}
	        	else {
					sb.append(ch);
	        	}
	        }
	        else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}
	
    /**
     * Logs the given property change event at the DEBUG level. The format is:
     * <pre>
     *  <i>Message</i>: <i>propertyName</i> "<i>oldValue</i>" -&gt; "<i>newValue</i>"
     * </pre>
     * 
     * @param logger The logger to log to
     * @param message The message to prefix the property change information
     * @param evt The event to print the details of
     */
    public static void logPropertyChange(
            @Nonnull Logger logger, @Nullable String message, @Nonnull PropertyChangeEvent evt) {
        if (logger.isDebugEnabled()) {
            logger.debug(message + ": " + evt.getPropertyName() +
                    " \"" + evt.getOldValue() + "\" -> \"" + evt.getNewValue() + "\"");
        }
    }

	/**
	 * Copies the contents of a given {@link InputStream} into a given
	 * {@link OutputStream}.
	 * 
	 * @param source
	 *            The {@link InputStream} to copy data from
	 * @param output
	 *            The {@link OutputStream} to copy the data to
	 * @return The total number of bytes that got copied.
	 * @throws IOException
	 *             If an I/O error occurs
	 */
    public static long copyStream(InputStream source, OutputStream output) throws IOException {
    	int next;
    	long total = 0;
    	while ((next = source.read()) != -1) {
    		output.write(next);
    		total++;
    	}
    	output.flush();
    	return total;
    }

	/**
	 * This method will recursively clean up a given object and all of its
	 * descendants.
	 * 
	 * @param o
	 *            The object to clean up, including its dependencies.
	 * @return A collection of exceptions and errors that occurred during
	 *         cleanup if any occurred.
	 */
    public static CleanupExceptions cleanupSPObject(SPObject o) {
        CleanupExceptions exceptions = new CleanupExceptions();
        exceptions.add(o.cleanup());
        for (SPObject child : o.getChildren()) {
            exceptions.add(cleanupSPObject(child));
        }
        return exceptions;
    }
    
    /**
     * This method returns a list of all of the ancestors of the given
     * {@link SPObject}. The order of the ancestors is such that the highest
     * ancestor is at the start of the list and the parent of the object itself
     * is at the end of the list.
     */
    public static List<SPObject> getAncestorList(SPObject o) {
        List<SPObject> ancestors = new ArrayList<SPObject>();
        SPObject parent = o.getParent();
        while (parent != null) {
            ancestors.add(0, parent);
            parent = parent.getParent();
        }
        return ancestors;
    }
    
	/**
	 * Locates the SPObject inside the root SPObject which has the given
	 * UUID, returning null if the item is not found. Throws ClassCastException
	 * if in item is found, but it is not of the expected type.
	 * 
	 * Note: If you are using an ArchitectProject, it is better to use its
	 * getObjectInTree method, since that uses a HashMap, not recursion.
	 * 
	 * @param <T>
	 *            The expected type of the item
	 * @param uuid
	 *            The UUID of the item
	 * @param expectedType
	 *            The type of the item with the given UUID. If you are uncertain
	 *            what type of object it is, or you do not want a
	 *            ClassCastException in case the item is of the wrong type, use
	 *            <tt>SPObject.class</tt> for this parameter.
	 * @return The item, or null if no item with the given UUID exists in the
	 *         descendent tree rooted at the given root object.
	 */
    public static <T extends SPObject> T findByUuid(SPObject root, String uuid, Class<T> expectedType) {
        return expectedType.cast(findRecursively(root, uuid));
    }
    
    /**
     * Performs a preorder traversal of the given {@link SPObject} and its
     * descendants, returning the first SPObject having the given UUID.
     * Returns null if no such SPObject exists under startWith.
     * 
     * Note: If you are using an ArchitectProject, it is better to use its
     * getObjectInTree method, since that uses a HashMap, not recursion.
     * 
     * @param startWith
     *            The SPObject to start the search with.
     * @param uuid
     *            The UUID to search for
     * @return the first SPObject having the given UUID in a preorder
     *         traversal of startWith and its descendants. Returns null if no
     *         such SPObject exists.
     */
    private static SPObject findRecursively(SPObject startWith, String uuid) {
    	if (startWith == null) {
    		throw new IllegalArgumentException("Cannot search a null object for children with the uuid " + uuid);
    	}
        if (uuid.equals(startWith.getUUID())) {
            return startWith;
        }
        
        List<? extends SPObject> children;
        if (startWith instanceof SQLObject) {
        	children = ((SQLObject) startWith).getChildrenWithoutPopulating();
        } else {
        	children = startWith.getChildren();
        }
        
        for (SPObject child : children) {
            SPObject found = findRecursively(child, uuid);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
    
    public static Map<String, SPObject> buildIdMap(SPObject startWith) {
    	Map<String, SPObject> idMap = new HashMap<String, SPObject>();
    	if (startWith == null) {
    		throw new IllegalArgumentException("Root object is null");
    	}
    	idMap.put(startWith.getUUID(), startWith);
        
        List<? extends SPObject> children;
        if (startWith instanceof SQLObject) {
        	children = ((SQLObject) startWith).getChildrenWithoutPopulating();
        } else {
        	children = startWith.getChildren();
        }
        
        for (SPObject child : children) {
        	idMap.putAll(buildIdMap(child));
        }
        return idMap;
    }

	/**
	 * Adds the given listeners to the hierarchy of {@link SPObject}s rooted at
	 * <code>root</code>.
	 * 
	 * @param root
	 *            The object at the top of the subtree to listen to. Must not be
	 *            null.
	 * @param spcl
	 *            The SQL Power child listener to add to root and all its
	 *            SPObject descendants. If you do not want {@link SPChildEvent}s,
	 *            you can provide null for this parameter.
	 */
    public static void listenToHierarchy(SPObject root, SPListener spcl) {
        root.addSPListener(spcl);
        if (root.allowsChildren()) {
            List<? extends SPObject> children;
            if (root instanceof SQLObject) {
                children = ((SQLObject) root).getChildrenWithoutPopulating();
            } else {
                children = root.getChildren();
            }
        	for (SPObject wob : children) {
        		listenToHierarchy(wob, spcl);
        	}
        }
    }

	/**
	 * This method is similar to listenToHierarchy but only listens to the 
	 * first two levels in the tree, i.e. the listener is not added to the 
	 * grand children of the root. See 
	 * {@link #lisenToHierachy(MatchMakerListener listener, MatchMakerObject root)}
	 */
	public static void listenToShallowHierarchy(SPListener listener, SPObject root) {
		root.addSPListener(listener);
		logger.debug("listenToShallowHierarchy: \"" + root.getName() + "\" (" +
				root.getClass().getName() + ") children: " + root.getChildren());
		for (SPObject spo : root.getChildren()) {
			spo.addSPListener(listener);
		}
	}

	/**
	 * Removes the given listeners from the hierarchy of {@link SPObject}s
	 * rooted at <code>root</code>.
	 * 
	 * @param root
	 *            The object at the top of the subtree to unlisten to. Must not
	 *            be null.
	 * @param spcl
	 *            The SQL Power child listener to remove from root and all its
	 *            SPObject descendants. If you do not want to unlisten to
	 *            {@link SPChildEvent}s, you can provide null for this
	 *            parameter.
	 */
    public static void unlistenToHierarchy(SPObject root, SPListener spcl) {
        root.removeSPListener(spcl);
        if (root.allowsChildren()) {
            List<? extends SPObject> children;
            if (root instanceof SQLObject) {
                children = ((SQLObject) root).getChildrenWithoutPopulating();
            } else {
                children = root.getChildren();
            }
        	for (SPObject wob : children) {
        		unlistenToHierarchy(wob, spcl);
        	}
        }
    }
    
	/**
	 * Prints the subtree rooted at the given {@link SPObject} to the given
	 * output stream. This is only intended for debugging; any machine parsing
	 * of the output of this method is incorrect!
	 * 
	 * @param out
	 *            the target of the debug information (often System.out)
	 * @param startWith
	 *            the root object for the dump
	 */
	public static void printSubtree(PrintWriter out, SPObject startWith) {
		printSubtree(out, startWith, 0);
	}

	/**
	 * Recursive subroutine of {@link #printSubtree(PrintWriter, SPObject)}.
	 * 
	 * @param out
	 *            The print stream to print to
	 * @param startWith
	 *            The object to print (and whose children to process
	 *            recursively)
	 * @param indentDepth
	 *            The amount of indent to print before printing the object
	 *            information
	 */
	private static void printSubtree(PrintWriter out, SPObject startWith, int indentDepth) {
		out.printf("%s%s \"%s\" (%s)\n",
				spaces(indentDepth * 2), startWith.getClass().getSimpleName(),
				startWith.getName(), startWith.getUUID());
		for (SPObject child : startWith.getChildren()) {
			printSubtree(out, child, indentDepth + 1);
		}
	}
	
	/**
	 * Creates a string consisting of the desired number of spaces.
	 * 
	 * @param n
	 *            The number of spaces in the string.
	 * @return A string of length n which consists entirely of spaces.
	 */
	private static String spaces(int n) {
		StringBuilder sb = new StringBuilder(n);
		for (int i = 0; i < n; i++) {
			sb.append(" ");
		}
		return sb.toString();
	}
	
    /**
     * Returns the human-readable summary of the given service info object.
     * Anywhere a server is referred to within the Wabit, this method should be
     * used to convert the service info object into the string the user sees.
     * 
     * @param si
     *            The service info to summarize.
     * @return The Wabit's canonical human-readable representation of the given
     *         service info.
     */
    public static String serviceInfoSummary(SPServerInfo si) {
        return si.getName() + " (" + si.getServerAddress() + ":" + si.getPort() + ")";
    }
    
    /**
     * This method will display the cleanup errors to the user. If the
     * user prompter factory given is null the errors will be logged instead.
     */
    public static void displayCleanupErrors(@Nonnull CleanupExceptions cleanupObject, 
            UserPrompterFactory upf) {
        if (upf != null) {
            if (!cleanupObject.isCleanupSuccessful()) {
                StringBuffer message = new StringBuffer();
                message.append("The following errors occurred during closing\n");
                for (String error : cleanupObject.getErrorMessages()) {
                    message.append("   " + error + "\n");
                }
                for (Exception exception : cleanupObject.getExceptions()) {
                    message.append("   " + exception.getMessage() + "\n");
                    logger.error("Exception during cleanup", exception);
                }
                UserPrompter up = upf.createUserPrompter(
                        message.toString(),
                        UserPromptType.MESSAGE, UserPromptOptions.OK, UserPromptResponse.OK,
                        null, "OK");
                up.promptUser();
            }
        } else {
            logCleanupErrors(cleanupObject);
        }
    }
    
    /**
     * Logs the exceptions and errors. This is useful if there is no available
     * user prompter.
     */
    public static void logCleanupErrors(@Nonnull CleanupExceptions cleanupObject) {
        for (String error : cleanupObject.getErrorMessages()) {
            logger.debug("Exception during cleanup, " + error);
        }
        for (Exception exception : cleanupObject.getExceptions()) {
            logger.error("Exception during cleanup", exception);
        }
    }

    /**
     * Resolves/decodes a path that can be any kind of URI, including a local
     * file reference. This method recognizes a superset of the path specs that
     * {@link #resolveConfiguredFilePath(ServletContext, String)} does.
     * <p>
     * The syntax for the path argument is documented in detail below:
     * <br><br>
     * Information for configuring file/URL locations in this file:
     * <br><br>
     * Configuration parameters described as accepting "any location" are
     * interpreted as follows:
     * <br>
     * <ol>
     * <li>If the value contains "://" then it is taken as an absolute URI, and
     * is fed to the java.net.URI constructor as-is
     * <br><br>
     * NOTE: if you are referencing a local file, you may find option 3, below,
     * more convenient and reliable than crafting a correct file:// URI
     * yourself, especially if the file's pathname contains spaces.<br><br></li>
     * 
     * <li>If the value starts with "webapp:" then it is taken as a location
     * inside this web application. Note that items in the WEB-INF directory are
     * accessible, as are the publicly-readable items outside of WEB-INF.
     * <br><br>
     * Caveat: This only works if the webapp is deployed in "exploded" form
     * (served from actual files and not straight out of the .war file)
     * <br><br>
     * This format is used for the default value, so the caveat applies by
     * default.<br><br></li>
     * 
     * <li>Otherwise, the value is taken as a filesystem location which may be
     * absolute or relative. The value is fed to the java.io.File constructor
     * as-is, and is then converted to a URI using File.toURI().</li>
     * </ol>
     * <br>
     * Configuration parameters described as accepting
     * "local filesystem locations" are interpreted using only steps 2 and 3
     * above. It is an error to specify a location that contains the "://"
     * sequence of characters in it.
     * 
     * @param context
	 *            If the path starts with webapp: this context will be used to
	 *            find where the file is located.
	 * @param relativeDir
	 *            If the path is a relative one we will look for the path
	 *            starting from this directory. If this directory is not
	 *            specified then the default execution directory will be used.
     * @param path
     * @return
     */
    public static URI resolveConfiguredPath(ServletContext context, File relativeDir, String path) {
        if (path.contains("://")) {
            try {
                return new URI(path);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(
                        "Configured path " + path + " looks like a URI, but isn't. " +
                        "Check your typing.", e);
            }
        } else {
            return resolveConfiguredFilePath(context, relativeDir, path).toURI();
        }
    }
    
    /**
	 * @see #resolveConfiguredPath(ServletContext, File, String)
	 */
    public static URI resolveConfiguredPath(ServletContext context, String path) {
    	return resolveConfiguredPath(context, null, path);
    }

    /**
	 * Resolves/decodes a path that can be any kind of URI, including a local
	 * file reference. Compared with
	 * {@link #resolveConfiguredPath(ServletContext, String)}, this method
	 * recognizes the subset of path specs that refer to the local filesystem
	 * and do not contain the substring "://" (so file:// URIs are not
	 * recognized by this method).
	 * <p>
	 * The syntax for the path argument is documented in detail on
	 * {@link #resolveConfiguredPath(ServletContext, String)}.
	 * 
	 * @param context
	 *            If the path starts with webapp: this context will be used to
	 *            find where the file is located.
	 * @param relativeDir
	 *            If the path is a relative one we will look for the path
	 *            starting from this directory. If this directory is not
	 *            specified then the default execution directory will be used.
	 * @param path
	 * @return
	 */
    public static File resolveConfiguredFilePath(ServletContext context, File relativeDir, String path) {
        if (path.startsWith("webapp:")) {
            if (context == null) {
                throw new IllegalArgumentException(
                        "Path specifications starting with 'webapp:' are only " +
                        "allowed in web applications");
            }
            String contextPath = path.substring("webapp:".length());
            String realPath = context.getRealPath(contextPath);
            if (realPath == null) {
                throw new IllegalArgumentException(
                    "Webapp path " + path + " could not be resolved. " +
                    "Check that it is valid, and ensure this webapp has " +
                    "been deployed in \"exploded\" mode.");
            }
            File file = new File(realPath);
            if (!file.exists()) {
                throw new IllegalArgumentException(
                        "Webapp path " + path + " resolved to " + file.getAbsolutePath() +
                        ", which does not exist. Check that the path is valid, " +
                        "and ensure this webapp has been deployed in \"exploded\" mode.");
            }
            return file;
        } else {
            File file = new File(path);
            if (relativeDir != null && !file.isAbsolute()) {
            	file = new File(relativeDir, path);
            }
            return file;
        }
    }

	/**
	 * This method mirrors the
	 * {@link #resolveConfiguredPath(ServletContext, String)} except it
	 * takes a file that is the existing directory that contains the file we are
	 * resolving. This allows us to translate files normally using the webapp:
	 * file type when we do not have a servlet context.
	 * 
	 * @param rootFile
	 * @param path
	 * @return
	 */
    public static URI resolveConfiguredPath(File rootFile, String path) {
        if (path.contains("://") || path.startsWith("file:")) {
            try {
                return new URI(path);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(
                        "Configured path " + path + " looks like a URI, but isn't. " +
                        "Check your typing.", e);
            }
        } else {
            return resolveConfiguredFilePath(rootFile, path).toURI();
        }
    }

    /**
	 * This method mirrors the
	 * {@link #resolveConfiguredFilePath(ServletContext, String)} except it
	 * takes a file that is the existing directory that contains the file we are
	 * resolving. This allows us to translate files normally using the webapp:
	 * file type when we do not have a servlet context.
	 * 
	 * @param rootFile
	 * @param path
	 * @return
	 */
    public static File resolveConfiguredFilePath(File rootFile, String path) {
        if (path.startsWith("webapp:")) {
            if (rootFile == null) {
                throw new IllegalArgumentException(
                        "Path specifications starting with 'webapp:' must have a root" +
                        " file specified");
            }
            if (!rootFile.exists()) {
            	throw new IllegalArgumentException("The root directory " + rootFile + " does not exist.");
            }
            if (!rootFile.isDirectory()) {
            	throw new IllegalStateException("The root directory " + rootFile + " is not a directory.");
            }
            String contextPath = path.substring("webapp:".length());
            File file = new File(rootFile, contextPath);
            if (!file.exists()) {
                throw new IllegalArgumentException(
                        "Webapp path " + path + " resolved to " + file.getAbsolutePath() +
                        ", which does not exist. Check that the path is valid, " +
                        "and ensure this webapp has been deployed in \"exploded\" mode.");
            }
            return file;
        } else {
            File file = new File(path);
            if (rootFile != null && !file.isAbsolute()) {
            	file = new File(rootFile, path);
            }
            return file;
        }
    }
    
	/**
	 * Returns the first ancestor of <tt>so</tt> which is of the given type, or
	 * <tt>null</tt> if <tt>so</tt> doesn't have an ancestor whose class is
	 * <tt>ancestorType</tt>.
	 * 
	 * @param so
	 *            The object for whose ancestor to look. (Thanks, Winston).
	 * @return The nearest ancestor of type ancestorType, or null if no such
	 *         ancestor exists.
	 */
	public static <T extends SPObject> T getAncestor(SPObject so, Class<T> ancestorType) {
	    while (so != null) {
	        if (so.getClass().equals(ancestorType) || ancestorType.isAssignableFrom(so.getClass())) 
	        	return ancestorType.cast(so);
	        so = so.getParent();
	    }
	    return null;
	}
	
    /**
     * Follows the chain of exceptions (using the getCause() method) to find the
     * root cause, which is the exception whose getCause() method returns null.
     * 
     * @param t The Throwable for which you want to know the root cause.  Must not
     * be null.
     * @return The ultimate cause of t.  This may be t itself.
     */
    public static Throwable rootCause(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t;
    }
    
    /**
     * Writes a stack trace to a string for user readability.
     * @param throwable
     * @return
     */
	public static String exceptionStackToString(final Throwable throwable) {
		// Details information
        Throwable t = throwable;
        StringWriter stringWriter = new StringWriter();
        final PrintWriter traceWriter = new PrintWriter(stringWriter);
        do {
            t.printStackTrace(traceWriter);
            if (SQLPowerUtils.rootCause(t) instanceof SQLException) {
                t = ((SQLException) SQLPowerUtils.rootCause(t)).getNextException();
                if (t != null) {
                    traceWriter.println("Next Exception:"); //$NON-NLS-1$
                }
            } else {
                t = null;
            }
        } while (t != null);
        traceWriter.close();
		return stringWriter.toString();
	}
    
}