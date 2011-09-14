package com.atlassian.maven.plugins.amps.product;


import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.atlassian.maven.plugins.amps.MavenContext;
import com.atlassian.maven.plugins.amps.MavenGoals;
import com.atlassian.maven.plugins.amps.Product;
import com.atlassian.maven.plugins.amps.util.ProjectUtils;
import com.atlassian.maven.plugins.amps.util.ZipUtils;

/**
 * This abstract class is common to real applications (which inherit from AbstractProductHandler, like JIRA or Confluence)
 * and the fake application Studio.
 *
 * This class handles common operations
 *
 * @since 3.6
 */
public abstract class AmpsProductHandler implements ProductHandler
{

    protected final MavenGoals goals;
    protected final MavenProject project;
    protected final MavenContext context;
    protected final Log log;

    protected AmpsProductHandler(MavenContext context, MavenGoals goals)
    {
        this.project = context.getProject();
        this.context = context;
        this.goals = goals;
        this.log = context.getLog();
    }

    /**
     * Copies and creates a zip file of the previous run's home directory minus any installed plugins.
     *
     * @param homeDirectory
     *            The path to the previous run's home directory.
     * @param targetZip
     *            The path to the final zip file.
     * @param product
     *            The product
     *
     * @since 3.1-m3
     */
    public void createHomeZip(final File homeDirectory, final File targetZip, final Product product) throws MojoExecutionException
    {
        if (homeDirectory == null || !homeDirectory.exists())
        {
            String homePath = "null";
            if (homeDirectory != null)
            {
                homePath = homeDirectory.getAbsolutePath();
            }
            context.getLog().info("home directory doesn't exist, skipping. [" + homePath + "]");
            return;
        }


        try
            {
            /*
             * The zip has /someRootFolder/{productId}-home/
             */
            final File appDir = getBaseDirectory(product);
            final File tmpDir = new File(appDir, "tmp-resources");
            final File genDir = new File(tmpDir, "generated-home");
            final String entryBase = "generated-resources/" + product.getId() + "-home";

            if (genDir.exists())
            {
                FileUtils.deleteDirectory(genDir);
            }

            genDir.mkdirs();
            FileUtils.copyDirectory(homeDirectory, genDir, true);

            cleanupProductHomeForZip(product, genDir);
            ZipUtils.zipDir(targetZip, genDir, entryBase);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error zipping home directory", e);
        }
    }

    /**
     * Prepares the home directory to snapshot:
     * <ul>
     * <li>Removes all unnecessary files</li>
     * <li>Perform product-specific clean-up</li>
     * <ul>
     * This is a reference implementation. It is probable that each application has a different set of directories to delete.
     * @param product the product details
     * @param homeDirectory an image of the home which will be zipped. This is not the working home, so you're free to remove files and parametrise them.
     * @throws IOException
     */
    public void cleanupProductHomeForZip(Product product, File homeDirectory) throws MojoExecutionException, IOException
    {
        try {
            // we want to get rid of the plugins folders.
            FileUtils.deleteDirectory(new File(homeDirectory, "plugins"));
            FileUtils.deleteDirectory(new File(homeDirectory, "bundled-plugins"));
        }
        catch (IOException ioe)
        {
            throw new MojoExecutionException("Could not delete home/plugins/ and /home/bundled-plugins/", ioe);
        }
    }

    public File getBaseDirectory(Product ctx)
    {
        return ProjectUtils.createDirectory(new File(project.getBuild().getDirectory(), ctx.getInstanceId()));
    }

    public File getHomeDirectory(Product ctx)
    {
        return new File(getBaseDirectory(ctx), "home");
    }

    public File getSnapshotDirectory(Product product)
    {
        return getHomeDirectory(product);
    }

    protected File createHomeDirectory(Product ctx)
    {
        return ProjectUtils.createDirectory(getHomeDirectory(ctx));
    }
}