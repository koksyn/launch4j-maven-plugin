/*
 * Maven Launch4j Plugin
 * Copyright (c) 2006 Paul Jungwirth
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.akathist.maven.plugins.launch4j;

import net.sf.launch4j.config.Config;
import net.sf.launch4j.config.ConfigPersister;
import net.sf.launch4j.config.ConfigPersisterException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Wraps a jar in a Windows executable.
 */
@Mojo(
        name = "launch4j",
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.RUNTIME,
        threadSafe = true
)
public class Launch4jMojo extends AbstractMojo {

    private static final String LAUNCH4J_ARTIFACT_ID = "launch4j";

    private static final String LAUNCH4J_GROUP_ID = "net.sf.launch4j";

    /**
     * Maven Session.
     */
    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    /**
     * The dependencies required by the project.
     */
    @Parameter(defaultValue = "${project.artifacts}", required = true, readonly = true)
    private Set<Artifact> dependencies;

    /**
     * The user's current project.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * The user's plugins (including, I hope, this one).
     */
    @Parameter(defaultValue = "${project.build.plugins}", required = true, readonly = true)
    private List<Artifact> plugins;

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component(role = RepositorySystem.class)
    private RepositorySystem factory;

    /**
     * The user's local repository.
     */
    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
    private ArtifactRepository localRepository;

    /**
     * The artifact resolver used to grab the binary bits that launch4j needs.
     */
    @Component(role = ArtifactResolver.class)
    private ArtifactResolver resolver;

    /**
     * The dependencies of this plugin.
     * Used to get the Launch4j artifact version.
     */
    @Parameter(defaultValue = "${plugin.artifacts}")
    private List<Artifact> pluginArtifacts;

    /**
     * The base of the current project.
     */
    @Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
    private File basedir;

    /**
     * Whether you want a gui or console app.
     * Valid values are "gui" and "console."
     * If you say gui, then launch4j will run your app from javaw instead of java
     * in order to avoid opening a DOS window.
     * Choosing gui also enables other options like taskbar icon and a splash screen.
     */
    @Parameter
    private String headerType;

    /**
     * The name of the Launch4j native configuration file
     * The path, if relative, is relative to the pom.xml.
     */
    @Parameter
    private File infile;

    /**
     * The name of the executable you want launch4j to produce.
     * The path, if relative, is relative to the pom.xml.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.artifactId}.exe")
    private File outfile;

    /**
     * The jar to bundle inside the executable.
     * The path, if relative, is relative to the pom.xml.
     * <p/>
     * If you don't want to wrap the jar, then this value should be the runtime path
     * to the jar relative to the executable. You should also set dontWrapJar to true.
     * <p/>
     * You can only bundle a single jar. Therefore, you should either create a jar that contains
     * your own code plus all your dependencies, or you should distribute your dependencies alongside
     * the executable.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.jar")
    private String jar;

    /**
     * Whether the executable should wrap the jar or not.
     */
    @Parameter(defaultValue = "false")
    private boolean dontWrapJar;

    /**
     * The title of the error popup if something goes wrong trying to run your program,
     * like if java can't be found. If this is a console app and not a gui, then this value
     * is used to prefix any error messages, as in ${errTitle}: ${errorMessage}.
     */
    @Parameter(defaultValue = "${project.name}")
    private String errTitle;

    /**
     * downloadUrl (?).
     */
    @Parameter
    private String downloadUrl;

    /**
     * supportUrl (?).
     */
    @Parameter
    private String supportUrl;

    /**
     * Constant command line arguments to pass to your program's main method.
     * Actual command line arguments entered by the user will appear after these.
     */
    @Parameter
    private String cmdLine;

    /**
     * Changes to the given directory, relative to the executable, before running your jar.
     * If set to <code>.</code> the current directory will be where the executable is.
     * If omitted, the directory will not be changed.
     */
    @Parameter
    private String chdir;

    /**
     * Priority class of windows process.
     * Valid values are "normal" (default), "idle" and "high".
     *
     * @see <a href="http://msdn.microsoft.com/en-us/library/windows/desktop/ms685100(v=vs.85).aspx">MSDN: Scheduling Priorities</a>
     */
    @Parameter(defaultValue = "normal")
    private String priority;


    /**
     * If true, the executable waits for the java application to finish before returning its exit code.
     * Defaults to false for gui applications. Has no effect for console applications, which always wait.
     */
    @Parameter(defaultValue = "false")
    private boolean stayAlive;

    /**
     * If true, when the application exits, any exit code other than 0 is considered a crash and
     * the application will be started again.
     */
    @Parameter(defaultValue = "false")
    private boolean restartOnCrash;

    /**
     * The icon to use in the taskbar. Must be in ico format.
     */
    @Parameter
    private File icon;

    /**
     * Object files to include. Used for custom headers only.
     */
    @Parameter
    private List<String> objs;

    /**
     * Win32 libraries to include. Used for custom headers only.
     */
    @Parameter
    private List<String> libs;

    /**
     * Variables to set.
     */
    @Parameter
    private List<String> vars;

    /**
     * Details about the supported jres.
     */
    @Parameter
    private Jre jre;

    /**
     * Details about the classpath your application should have.
     * This is required if you are not wrapping a jar.
     */
    @Parameter
    private ClassPath classPath;

    /**
     * Details about whether to run as a single instance.
     */
    @Parameter
    private SingleInstance singleInstance;

    /**
     * Details about the splash screen.
     */
    @Parameter
    private Splash splash;

    /**
     * Lots of information you can attach to the windows process.
     */
    @Parameter
    private VersionInfo versionInfo;

    /**
     * If set to true, it will prevent filling out the VersionInfo params with default values.
     */
    @Parameter(defaultValue = "false")
    private boolean disableVersionInfoDefaults;

    /**
     * Various messages you can display.
     */
    @Parameter
    private Messages messages;

    /**
     * Windows manifest file (a XML file) with the same name as .exe file (myapp.exe.manifest)
     */
    @Parameter
    private File manifest;

    /**
     * If set to true it will save final config into a XML file
     */
    @Parameter(defaultValue = "false")
    private boolean saveConfig = false;

    /**
     * If {@link #saveConfig} is set to true, config will be written to this file
     */
    @Parameter(defaultValue = "${project.build.directory}/launch4j-config.xml")
    private File configOutfile;

    /**
     * If set to true, a synchronized block will be used to protect resources
     */
    @Parameter(defaultValue = "false")
    private boolean parallelExecution = false;

    /**
     * If set to true, execution of the plugin will be skipped
     */
    @Parameter(defaultValue = "false")
    private boolean skip = false;

    @Override
    public void execute() throws MojoExecutionException {
        if (parallelExecution) {
            synchronized (Launch4jMojo.class) {
                doExecute();
            }
        } else {
            doExecute();
        }
    }

    private void doExecute() throws MojoExecutionException {
        if (this.skipExecution()) {
            getLog().debug("Skipping execution of the plugin.");
            return;
        }

        if (!disableVersionInfoDefaults) {
            tryFillOutVersionInfoByDefaults();
        }

        // Setup environment
        final File workDir = setupBuildEnvironmentAndGetWorkDir();

        // Setup configuration
        final boolean infileExists = tryCheckInfileExists();
        final ConfigPersister configPersister = ConfigPersister.getInstance();
        final Config config = generateConfiguration(configPersister, workDir, infileExists);
        final File configBaseDir = generateConfigBaseDir(infileExists);
        configPersister.setAntConfig(config, configBaseDir);

        // Log configuration
        if (getLog().isDebugEnabled()) {
            logConfigState(config);
        }

        tryBuildExecutable(workDir);

        if (saveConfig) {
            trySaveConfigIntoXMLFile(configPersister);
        }
    }

    /**
     * Checks if execution of the plugin should be skipped
     *
     * @return true to skip execution
     */
    private boolean skipExecution() {
        getLog().debug("skip = " + this.skip);
        getLog().debug("skipLaunch4j = " + System.getProperty("skipLaunch4j"));

        return skip || System.getProperty("skipLaunch4j") != null;
    }

    private void tryFillOutVersionInfoByDefaults() throws MojoExecutionException {
        try {
            if (versionInfo == null) {
                versionInfo = new VersionInfo();
            }
            versionInfo.tryFillOutByDefaults(project, outfile);
        } catch (RuntimeException exception) {
            throw new MojoExecutionException("Cannot fill out VersionInfo by defaults", exception);
        }
    }

    /**
     * Prepares a little directory for launch4j to do its thing. Launch4j needs a bunch of object files
     * (in the w32api and head directories) and the ld and windres binaries (in the bin directory).
     * The tricky part is that launch4j picks this directory based on where its own jar is sitting.
     * In our case, the jar is going to be sitting in the user's ~/.m2 repository. That's okay: we know
     * maven is allowed to write there. So we'll just add our things to that directory.
     * <p/>
     * This approach is not without flaws.
     * It risks two processes writing to the directory at the same time.
     * But fortunately, once the binary bits are in place, we don't do any more writing there,
     * and launch4j doesn't write there either.
     * Usually ~/.m2 will only be one system or another.
     * But if it's an NFS mount shared by several system types, this approach will break.
     * <p/>
     * Okay, so here is a better proposal: package the plugin without these varying binary files,
     * and put each set of binaries in its own tarball. Download the tarball you need to ~/.m2 and
     * unpack it. Then different systems won't contend for the same space. But then I'll need to hack
     * the l4j code so it permits passing in a work directory and doesn't always base it on
     * the location of its own jarfile.
     *
     * @return the work directory.
     */
    private File setupBuildEnvironmentAndGetWorkDir() throws MojoExecutionException {
        FileSystemSetup fileSystemSetup = new FileSystemSetup(getLog());
        fileSystemSetup.createParentFolderQuietly(outfile);
        Artifact binaryBits = chooseBinaryBits();
        retrieveBinaryBits(binaryBits);
        return unpackWorkDir(binaryBits);
    }

    private boolean tryCheckInfileExists() throws MojoExecutionException {
        if (infile != null) {
            if (infile.exists()) {
                return true;
            } else {
                throw new MojoExecutionException("Launch4j native configuration file [" + infile.getAbsolutePath() + "] does not exist!");
            }
        }

        return false;
    }

    private Config generateConfiguration(ConfigPersister configPersister, File workDir, boolean infileExists) throws MojoExecutionException {
        if (infileExists) {
            return tryGetOverwrittenNativeConfiguration(configPersister);
        } else {
            return generateConfigurationFromProperties(workDir);
        }
    }

    private File generateConfigBaseDir(boolean infileExists) {
        if (infileExists) {
            return infile.getParentFile();
        } else {
            return basedir;
        }
    }

    /**
     * Just prints out how we were configured.
     */
    private void logConfigState(Config config) {
        Log log = getLog();

        log.debug("headerType = " + config.getHeaderType());
        log.debug("outfile = " + config.getOutfile());
        log.debug("jar = " + config.getJar());
        log.debug("dontWrapJar = " + config.isDontWrapJar());
        log.debug("errTitle = " + config.getErrTitle());
        log.debug("downloadUrl = " + config.getDownloadUrl());
        log.debug("supportUrl = " + config.getSupportUrl());
        log.debug("cmdLine = " + config.getCmdLine());
        log.debug("chdir = " + config.getChdir());
        log.debug("priority = " + config.getPriority());
        log.debug("stayAlive = " + config.isStayAlive());
        log.debug("restartOnCrash = " + config.isRestartOnCrash());
        log.debug("icon = " + config.getIcon());
        log.debug("objs = " + config.getHeaderObjects());
        log.debug("libs = " + config.getLibs());
        log.debug("vars = " + config.getVariables());
        if (config.getSingleInstance() != null) {
            log.debug("singleInstance.mutexName = " + config.getSingleInstance().getMutexName());
            log.debug("singleInstance.windowTitle = " + config.getSingleInstance().getWindowTitle());
        } else {
            log.debug("singleInstance = null");
        }
        if (config.getJre() != null) {
            log.debug("jre.path = " + config.getJre().getPath());
            log.debug("jre.minVersion = " + config.getJre().getMinVersion());
            log.debug("jre.maxVersion = " + config.getJre().getMaxVersion());
            log.debug("jre.requiresJdk = " + config.getJre().getRequiresJdk());
            log.debug("jre.requires64Bit = " + config.getJre().getRequires64Bit());
            log.debug("jre.initialHeapSize = " + config.getJre().getInitialHeapSize());
            log.debug("jre.initialHeapPercent = " + config.getJre().getInitialHeapPercent());
            log.debug("jre.maxHeapSize = " + config.getJre().getMaxHeapSize());
            log.debug("jre.maxHeapPercent = " + config.getJre().getMaxHeapPercent());
            log.debug("jre.opts = " + config.getJre().getOptions());
        } else {
            log.debug("jre = null");
        }
        if (config.getClassPath() != null) {
            log.debug("classPath.mainClass = " + config.getClassPath().getMainClass());
        }
        if (classPath != null) {
            log.debug("classPath.addDependencies = " + classPath.addDependencies);
            log.debug("classPath.jarLocation = " + classPath.jarLocation);
            log.debug("classPath.preCp = " + classPath.preCp);
            log.debug("classPath.postCp = " + classPath.postCp);
        } else {
            log.info("classpath = null");
        }
        if (config.getSplash() != null) {
            log.debug("splash.file = " + config.getSplash().getFile());
            log.debug("splash.waitForWindow = " + config.getSplash().getWaitForWindow());
            log.debug("splash.timeout = " + config.getSplash().getTimeout());
            log.debug("splash.timoutErr = " + config.getSplash().isTimeoutErr());
        } else {
            log.debug("splash = null");
        }
        if (config.getVersionInfo() != null) {
            log.debug("versionInfo.fileVersion = " + config.getVersionInfo().getFileVersion());
            log.debug("versionInfo.txtFileVersion = " + config.getVersionInfo().getTxtFileVersion());
            log.debug("versionInfo.fileDescription = " + config.getVersionInfo().getFileDescription());
            log.debug("versionInfo.copyright = " + config.getVersionInfo().getCopyright());
            log.debug("versionInfo.productVersion = " + config.getVersionInfo().getProductVersion());
            log.debug("versionInfo.txtProductVersion = " + config.getVersionInfo().getTxtProductVersion());
            log.debug("versionInfo.productName = " + config.getVersionInfo().getProductName());
            log.debug("versionInfo.companyName = " + config.getVersionInfo().getCompanyName());
            log.debug("versionInfo.internalName = " + config.getVersionInfo().getInternalName());
            log.debug("versionInfo.originalFilename = " + config.getVersionInfo().getOriginalFilename());
            log.debug("versionInfo.language = " + config.getVersionInfo().getLanguage());
            log.debug("versionInfo.languageIndex = " + config.getVersionInfo().getLanguageIndex());
            log.debug("versionInfo.trademarks = " + config.getVersionInfo().getTrademarks());
        } else {
            log.debug("versionInfo = null");
        }
        if (config.getMessages() != null) {
            log.debug("messages.startupErr = " + config.getMessages().getStartupErr());
            log.debug("messages.jreNotFoundErr = " + config.getMessages().getJreNotFoundErr());
            log.debug("messages.jreVersionErr = " + config.getMessages().getJreVersionErr());
            log.debug("messages.launcherErr = " + config.getMessages().getLauncherErr());
            log.debug("messages.instanceAlreadyExistsMsg = " + config.getMessages().getInstanceAlreadyExistsMsg());
        } else {
            log.debug("messages = null");
        }
    }

    private void tryBuildExecutable(File baseDirectory) throws MojoExecutionException {
        try {
            ExecutableBuilder executableBuilder = new ExecutableBuilder(getLog());
            executableBuilder.build(baseDirectory);
        } catch (RuntimeException exception) {
            throw new MojoExecutionException("Failed to build the executable", exception);
        }
    }

    private void trySaveConfigIntoXMLFile(ConfigPersister configPersister) throws MojoExecutionException {
        try {
            configPersister.save(configOutfile);
        } catch (ConfigPersisterException e) {
            throw new MojoExecutionException("Cannot save config into a XML file", e);
        }
    }

    /**
     * Decides which platform-specific bundle we need, based on the current operating system.
     */
    private Artifact chooseBinaryBits() throws MojoExecutionException {
        String plat;
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        getLog().debug("OS = " + os);
        getLog().debug("Architecture = " + arch);

        // See here for possible values of os.name:
        // http://lopica.sourceforge.net/os.html
        if (os.startsWith("Windows")) {
            plat = "win32";
        } else if ("Linux".equals(os)) {
            if ("amd64".equals(arch)) {
                plat = "linux64";
            } else {
                plat = "linux";
            }
        } else if ("Solaris".equals(os) || "SunOS".equals(os)) {
            plat = "solaris";
        } else if ("Mac OS X".equals(os) || "Darwin".equals(os)) {
            plat = "mac";
        } else {
            throw new MojoExecutionException("Sorry, Launch4j doesn't support the '" + os + "' OS.");
        }

        return factory.createArtifactWithClassifier(LAUNCH4J_GROUP_ID, LAUNCH4J_ARTIFACT_ID,
                getLaunch4jVersion(), "jar", "workdir-" + plat);
    }

    /**
     * Downloads the platform-specific parts, if necessary.
     */
    private void retrieveBinaryBits(Artifact a) throws MojoExecutionException {
        ProjectBuildingRequest configuration = session.getProjectBuildingRequest();
        configuration.setRemoteRepositories(project.getRemoteArtifactRepositories());
        configuration.setLocalRepository(localRepository);
        configuration.setProject(session.getCurrentProject());

        getLog().debug("Retrieving artifact: " + a + " stored in " + a.getFile());

        try {
            resolver.resolveArtifact(configuration, a).getArtifact();
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Illegal Argument Exception", e);
        } catch (ArtifactResolverException e) {
            throw new MojoExecutionException("Can't retrieve platform-specific components", e);
        }
    }

    /**
     * Unzips the given artifact in-place and returns the newly-unzipped top-level directory.
     * Writes a marker file to prevent unzipping more than once.
     */
    private File unpackWorkDir(Artifact artifact) throws MojoExecutionException {
        Artifact localArtifact = localRepository.find(artifact);
        if (localArtifact == null || localArtifact.getFile() == null) {
            throw new MojoExecutionException("Cannot obtain file path to " + artifact);
        }
        getLog().debug("Unpacking " + localArtifact + " into " + localArtifact.getFile());
        File platJar = localArtifact.getFile();
        File dest = platJar.getParentFile();
        File marker = new File(dest, platJar.getName() + ".unpacked");
        String n = platJar.getName();
        File workdir = new File(dest, n.substring(0, n.length() - 4));

        // If the artifact is a SNAPSHOT, then a.getVersion() will report the long timestamp,
        // but getFile() will be 1.1-SNAPSHOT.
        // Since getFile() doesn't use the timestamp, all timestamps wind up in the same place.
        // Therefore, we need to expand the jar every time, if the marker file is stale.
        if (marker.exists() && marker.lastModified() > platJar.lastModified()) {
            // if (marker.exists() && marker.platJar.getName().indexOf("SNAPSHOT") == -1) {
            getLog().info("Platform-specific work directory already exists: " + workdir.getAbsolutePath());
        } else {
            // trying to use plexus-archiver here is a miserable waste of time:
            try (JarFile jf = new JarFile(platJar)) {
                Enumeration<JarEntry> en = jf.entries();
                while (en.hasMoreElements()) {
                    JarEntry je = en.nextElement();
                    File outFile = new File(dest, je.getName());
                    if (!outFile.toPath().normalize().startsWith(dest.toPath().normalize())) {
                        throw new RuntimeException("Bad zip entry");
                    }
                    File parent = outFile.getParentFile();
                    if (parent != null) parent.mkdirs();
                    if (je.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        try (InputStream in = jf.getInputStream(je)) {
                            try (FileOutputStream fout = new FileOutputStream(outFile)) {
                                byte[] buf = new byte[1024];
                                int len;
                                while ((len = in.read(buf)) >= 0) {
                                    fout.write(buf, 0, len);
                                }
                            }
                        }
                        outFile.setLastModified(je.getTime());
                    }
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Error unarchiving " + platJar, e);
            }

            try {
                marker.createNewFile();
                marker.setLastModified(new Date().getTime());
            } catch (IOException e) {
                getLog().warn("Trouble creating marker file " + marker, e);
            }
        }

        setPermissions(workdir);
        return workdir;
    }

    private Config tryGetOverwrittenNativeConfiguration(ConfigPersister configPersister) throws MojoExecutionException {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Trying to load Launch4j native configuration using file=" + infile.getAbsolutePath());
        }

        try {
            // load launch4j config file from <infile>
            configPersister.load(infile);
        } catch (ConfigPersisterException e) {
            getLog().error(e);
            throw new MojoExecutionException("Could not load Launch4j native configuration file", e);
        }

        // retrieve the loaded configuration for manipulation
        Config config = configPersister.getConfig();
        overwriteConfiguration(config);

        return config;
    }

    private Config generateConfigurationFromProperties(File workDir) throws MojoExecutionException {
        final Config config = new Config();

        config.setHeaderType(headerType);
        config.setOutfile(outfile);
        config.setJar(getJar());
        config.setDontWrapJar(dontWrapJar);
        config.setErrTitle(errTitle);
        config.setDownloadUrl(downloadUrl);
        config.setSupportUrl(supportUrl);
        config.setCmdLine(cmdLine);
        config.setChdir(chdir);
        config.setPriority(priority);
        config.setStayAlive(stayAlive);
        config.setRestartOnCrash(restartOnCrash);
        config.setManifest(manifest);
        config.setIcon(icon);
        config.setHeaderObjects(relativizeAndCopy(workDir, objs));
        config.setLibs(relativizeAndCopy(workDir, libs));
        config.setVariables(vars);

        if (classPath != null) {
            config.setClassPath(classPath.toL4j(dependencies));
        }
        if (jre != null) {
            jre.deprecationWarning(getLog());
            config.setJre(jre.toL4j());
        }
        if (singleInstance != null) {
            config.setSingleInstance(singleInstance.toL4j());
        }
        if (splash != null) {
            config.setSplash(splash.toL4j());
        }
        if (versionInfo != null) {
            config.setVersionInfo(versionInfo.toL4j());
        }
        if (messages != null) {
            messages.deprecationWarning(getLog());
            config.setMessages(messages.toL4j());
        }

        return config;
    }

    private void overwriteConfiguration(Config config) {
        // overwrite several properties analogous to the ANT task
        // https://sourceforge.net/p/launch4j/git/ci/master/tree/src/net/sf/launch4j/ant/Launch4jTask.java#l84

        String jarDefaultValue = project.getBuild().getDirectory() + "/" + project.getBuild().getFinalName() + ".jar";
        if (jar != null && !jar.equals(jarDefaultValue)) {
            getLog().debug("Overwriting config file property 'jar' (='" + config.getJar().getAbsolutePath() + "') with local value '" + getJar().getAbsolutePath() + "'");
            // only overwrite when != defaultValue (should be != null anytime because of the default value)
            config.setJar(getJar());
        }

        File outFileDefaultValue = new File(project.getBuild().getDirectory() + "/" + project.getArtifactId() + ".exe");
        if (outfile != null && !outfile.getAbsolutePath().equals(outFileDefaultValue.getAbsolutePath())) {
            // only overwrite when != defaultValue (should be != null anytime because of the default value)
            getLog().debug("Overwriting config file property 'outfile' (='" + config.getOutfile().getAbsolutePath() + "') with local value '" + outfile.getAbsolutePath() + "'");
            config.setOutfile(outfile);
        }

        if (versionInfo != null) {
            if (versionInfo.fileVersion != null) {
                getLog().debug("Overwriting config file property 'versionInfo.fileVersion' (='" + config.getVersionInfo().getFileVersion() + "') with local value '" + versionInfo.fileVersion + "'");
                config.getVersionInfo().setFileVersion(versionInfo.fileVersion);
            }
            if (versionInfo.txtFileVersion != null) {
                getLog().debug("Overwriting config file property 'versionInfo.txtFileVersion' (='" + config.getVersionInfo().getTxtFileVersion() + "') with local value '" + versionInfo.txtFileVersion + "'");
                config.getVersionInfo().setTxtFileVersion(versionInfo.txtFileVersion);
            }
            if (versionInfo.productVersion != null) {
                getLog().debug("Overwriting config file property 'versionInfo.productVersion' (='" + config.getVersionInfo().getProductVersion() + "') with local value '" + versionInfo.productVersion + "'");
                config.getVersionInfo().setProductVersion(versionInfo.productVersion);
            }
            if (versionInfo.txtProductVersion != null) {
                getLog().debug("Overwriting config file property 'versionInfo.txtProductVersion' (='" + config.getVersionInfo().getTxtProductVersion() + "') with local value '" + versionInfo.txtProductVersion + "'");
                config.getVersionInfo().setTxtProductVersion(versionInfo.txtProductVersion);
            }
        }
    }

    /**
     * Chmods the helper executables ld and windres on systems where that is necessary.
     */
    private void setPermissions(File workdir) {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            try {
                new ProcessBuilder("chmod", "755", workdir + "/bin/ld").start().waitFor();
                new ProcessBuilder("chmod", "755", workdir + "/bin/windres").start().waitFor();
            } catch (InterruptedException e) {
                getLog().warn("Interrupted while chmodding platform-specific binaries", e);
            } catch (IOException e) {
                getLog().warn("Unable to set platform-specific binaries to 755", e);
            }
        }
    }

    /**
     * If custom header objects or libraries shall be linked, they need to sit inside the launch4j working dir.
     */
    private List<String> relativizeAndCopy(File workdir, List<String> paths) throws MojoExecutionException {
        if (paths == null) return null;

        List<String> result = new ArrayList<>();
        for (String path : paths) {
            Path source = basedir.toPath().resolve(path);
            Path dest = workdir.toPath().resolve(basedir.toPath().relativize(source));

            if (!source.startsWith(basedir.toPath())) {
                throw new MojoExecutionException("File must reside in the project directory: " + path);
            }

            if (Files.exists(source)) {
                try {
                    Files.createDirectories(dest.getParent());
                    Path target = Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                    result.add(workdir.toPath().relativize(target).toString());
                } catch (IOException e) {
                    throw new MojoExecutionException("Can't copy file to workdir", e);
                }
            } else {
                result.add(path);
            }
        }

        return result;
    }

    /**
     * A version of the Launch4j used by the plugin.
     * We want to download the platform-specific bundle whose version matches the Launch4j version,
     * so we have to figure out what version the plugin is using.
     *
     * @return version of Launch4j
     * @throws MojoExecutionException when version is null
     */
    private String getLaunch4jVersion() throws MojoExecutionException {
        String version = null;

        for (Artifact artifact : pluginArtifacts) {
            if (LAUNCH4J_GROUP_ID.equals(artifact.getGroupId()) &&
                    LAUNCH4J_ARTIFACT_ID.equals(artifact.getArtifactId())
                    && "core".equals(artifact.getClassifier())) {

                version = artifact.getVersion();
                getLog().debug("Found launch4j version " + version);
                break;
            }
        }

        if (version == null) {
            throw new MojoExecutionException("Impossible to find which Launch4j version to use");
        }

        return version;
    }

    private File getJar() {
        return new File(jar);
    }

    @Override
    public String toString() {
        return "Launch4jMojo{" +
                "headerType='" + headerType + '\'' +
                ", infile=" + infile +
                ", outfile=" + outfile +
                ", jar='" + jar + '\'' +
                ", dontWrapJar=" + dontWrapJar +
                ", errTitle='" + errTitle + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", supportUrl='" + supportUrl + '\'' +
                ", cmdLine='" + cmdLine + '\'' +
                ", chdir='" + chdir + '\'' +
                ", priority='" + priority + '\'' +
                ", stayAlive=" + stayAlive +
                ", restartOnCrash=" + restartOnCrash +
                ", icon=" + icon +
                ", objs=" + objs +
                ", libs=" + libs +
                ", vars=" + vars +
                ", jre=" + jre +
                ", classPath=" + classPath +
                ", singleInstance=" + singleInstance +
                ", splash=" + splash +
                ", versionInfo=" + versionInfo +
                ", disableVersionInfoDefaults=" + disableVersionInfoDefaults +
                ", messages=" + messages +
                ", manifest=" + manifest +
                ", saveConfig=" + saveConfig +
                ", configOutfile=" + configOutfile +
                ", parallelExecution=" + parallelExecution +
                ", skip=" + skip +
                '}';
    }
}
