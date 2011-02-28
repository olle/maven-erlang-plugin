package eu.lindenbaum.maven;

import java.io.File;
import java.util.Locale;

import eu.lindenbaum.maven.util.ErlConstants;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;

/**
 * A base class for all {@link MavenReport}s that need to operate on values
 * provided by the {@link PropertiesImpl} bean.
 * 
 * @author Tobias Schlager <tobias.schlager@lindenbaum.eu>
 * @see PackagingType
 * @see Properties
 */
public abstract class ErlangReport extends AbstractMavenReport {
  /**
   * {@link MavenProject} to process.
   * 
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * Doxia Site Renderer.
   * 
   * @component
   * @required
   * @readonly
   */
  private Renderer renderer;

  /**
   * {@link ArtifactRepository} storing dependencies of this
   * {@link MavenProject}.
   * 
   * @parameter expression="${localRepository}"
   * @required
   * @readonly
   */
  private ArtifactRepository repository;

  /**
   * The projects working directory root.
   * 
   * @parameter expression="${basedir}"
   * @required
   * @readonly
   */
  private File base;

  /**
   * The projects build directory.
   * 
   * @parameter expression="${project.build.directory}"
   * @required
   * @readonly
   */
  private File target;

  /**
   * The cookie to use for the java and the backend node.
   * 
   * @parameter expression="${cookie}"
   */
  private String cookie;

  /**
   * The erlang command used to start an erlang backend node. The path must
   * exist and the destination must be executable. If the given command does not
   * fullfill these requirements <code>erl</code> is used (assuming the command
   * is part of the hosts <code>PATH</code>). The path must not contain any
   * arguments.
   * 
   * @parameter expression="${erlCommand}"
   */
  private String erlCommand;

  @Override
  protected final MavenProject getProject() {
    return this.project;
  }

  @Override
  protected final Renderer getSiteRenderer() {
    return this.renderer;
  }

  /**
   * This may be overwritten by implementing reports, default return value is
   * the absolute path of {@link #getReportOutputDirectory()}.
   */
  @Override
  protected String getOutputDirectory() {
    return this.target.getAbsolutePath();
  }

  /**
   * Injects the needed {@link Properties} into the abstract
   * {@link #execute(Log, Locale, Properties)} method to be implemented by
   * subclasses.
   */
  @Override
  protected final void executeReport(Locale locale) throws MavenReportException {
    try {
      Properties properties = getProperties();
      execute(getLog(), locale, properties);
    }
    catch (MojoExecutionException e) {
      throw new MavenReportException(e.getMessage());
    }
    catch (MojoFailureException e) {
      throw new MavenReportException(e.getMessage());
    }
  }

  /**
   * Returns the command to use to start an erlang backend node, also known as
   * the {@code erl} executable.
   * 
   * @return the user configured {@link #erlCommand} or simply {@code erl} if
   *         {@link #erlCommand} was not configured or denotes an invalid path.
   */
  private String getErlCommand() {
    if (this.erlCommand != null) {
      File cmd = new File(this.erlCommand);
      if (cmd.isFile() && cmd.canExecute()) {
        return cmd.getAbsolutePath();
      }
    }
    return ErlConstants.ERL;
  }

  /**
   * Returns properties built from the mojo parameters of this report and based
   * on the packaging type of this project.
   * 
   * @return properties for this report
   */
  protected Properties getProperties() {
    PackagingType type = PackagingType.fromString(this.project.getPackaging());
    String cmd = getErlCommand();
    getLog().debug("Using command: " + cmd);
    return new PropertiesImpl(type, this.project, this.repository, this.base, this.target, cmd, this.cookie);
  }

  /**
   * Will be invoked when {@link #execute()} gets invoked on the base class.
   * 
   * @param log logger to be used for output logging
   * @param l the demanded locale as passed to {@link #executeReport(Locale)}
   * @param p to be passed by the base class.
   * @see AbstractMavenReport#executeReport(Locale)
   */
  protected abstract void execute(Log log, Locale l, Properties p) throws MojoExecutionException,
                                                                  MojoFailureException,
                                                                  MavenReportException;

  /**
   * Returns whether this report can generate any output.
   * 
   * @return {@code true} if the project is an Erlang application or library
   *         application, {@code false} otherwise
   */
  @Override
  public final boolean canGenerateReport() {
    PackagingType type = PackagingType.fromString(getProject().getPackaging());
    return type == PackagingType.ERLANG_OTP || type == PackagingType.ERLANG_STD;
  }
}
