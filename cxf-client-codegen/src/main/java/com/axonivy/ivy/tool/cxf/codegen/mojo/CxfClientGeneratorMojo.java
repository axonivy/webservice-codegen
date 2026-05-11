package com.axonivy.ivy.tool.openapi.codegen.mojo;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * <p>
 * Generates a CXF client
 * </p>
 * <p>
 * Command line invocation is supported.
 * </p>
 * <code>
 * mvn com.axonivy.ivy.tool.soap:cxf-client-codegen:generate-cxf-client
 * -Divy.generate.cxf.client.spec=https://petstore3.swagger.io/api/v3/openapi.json
 * -Divy.generate.cxf.client.output=src_generated/soap/petstore
 * -Divy.generate.cxf.client.package=com.swagger.petstore.client
 * </code>
 *
 * @since 1.0.0
 */
@Mojo(name = CxfClientGeneratorMojo.GOAL, requiresProject = false)
public class CxfClientGeneratorMojo extends AbstractMojo {
  public static final String GOAL = "generate-cxf-client";

  @Parameter(property = "ivy.generate.webservice.client.skip", defaultValue = "false")
  boolean skipGenerate;

  // /** URI or Path to an openapi.json or openapi.yaml */
  // @Parameter(property = "ivy.generate.openapi.client.spec", required = true)
  // String openApiSpec;

  // @Parameter(property = "ivy.generate.openapi.client.output", required = true)
  // Path outputDir;

  // @Parameter(property = "ivy.generate.openapi.client.namespace")
  // String namespace;

  // /**
  //  * Generate types for generic 'allOf', 'anyOf' references.
  //  * This can help to build a valid client, if generated sources can't be compiled using the default options
  //  */
  // @Parameter(property = "ivy.generate.openapi.client.resolveFully")
  // Boolean resolveFully;

  @Override
  public void execute() throws MojoExecutionException {
    if (skipGenerate) {
      return;
    }
    getLog().info("Generating CXF client sources...");
  }

}
