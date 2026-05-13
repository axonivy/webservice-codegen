package com.axonivy.ivy.tool.cxf.codegen.mojo;

import java.nio.file.Path;
import java.util.Map;

import org.apache.cxf.tools.common.ToolContext;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.axonivy.ivy.tool.cxf.codegen.CxfClientGenerator;
import com.axonivy.ivy.tool.cxf.codegen.CxfClientGenerator.CodegenOpts;

/**
 * <p>
 * Generates a CXF client
 * </p>
 * <p>
 * Command line invocation is supported.
 * </p>
 * <code>
 * mvn com.axonivy.ivy.tool.soap:cxf-client-codegen:generate-cxf-client
 * -Divy.generate.webservice.client.spec=https://petstore3.swagger.io/api/v3/openapi.json
 * -Divy.generate.webservice.client.output=src_generated/soap/petstore
 * -Divy.generate.webservice.client.package=com.swagger.petstore.client
 * </code>
 *
 * @since 1.0.0
 */
@Mojo(name = CxfClientGeneratorMojo.GOAL, requiresProject = false)
public class CxfClientGeneratorMojo extends AbstractMojo {
  public static final String GOAL = "generate-cxf-client";

  @Parameter(property = "ivy.generate.webservice.client.skip", defaultValue = "false")
  boolean skipGenerate;

  /** URI or Path to a WSDL file */
  @Parameter(property = "ivy.generate.webservice.client.wsdl", required = true)
  String wsdl;

  @Parameter(property = "ivy.generate.webservice.client.output", required = true)
  Path outputDir;

  // TODO: doc
  @Parameter(property = "ivy.generate.webservice.client.nsMappings")
  Map<String, String> nsMappings;

  /**
   * If the WSDL service definitions contains similar attributes only differing by
   * their ‘underscore’ name, one of the attributes is gone in the generated CXF
   * client. 
   * Enable this property to enforce the manifestation of the underscore in the generated client.
   * <pre>
   * &lt;xsd:element name="PRICEDATE" type="tns:char1"/&gt;
   * &lt;xsd:element name="PRICE_DATE" type="tns:date10"/&gt;
   * </pre>
   */
  @Parameter(property = "ivy.generate.webservice.client.underscoreNames", defaultValue = "false")
  Boolean underscoreNames;

  // TODO: enable insecure SSL
  @Parameter(property = "ivy.generate.webservice.client.insecureSSL", defaultValue = "false")
  Boolean insecureSSL;

  @Override
  public void execute() throws MojoExecutionException {
    if (skipGenerate) {
      return;
    }

    getLog().info("Generating CXF client sources for WSDL: " + wsdl);

    ToolContext cxfContext;
    try {
      cxfContext = CxfClientGenerator.generate(wsdl, outputDir,
          new CodegenOpts(nsMappings, underscoreNames));
      getLog().info("Generated CXF client context: " + cxfContext);
    } catch (Exception ex) {
      getLog().error("Failed to generate CXF client sources", ex);
    }
    // tmpClientJar -> new JarProjectIntegratorService(tmpClientJar, context.project, targetJar)
    // .integrate(context.monitor),
    // new CodegenOpts(context.codegen.nsMappings(), context.codegen.underscoreNames()));

    // var services = CxfModelConverter.toWsConfigModel(cxfContext);
    // if (services.size() > 1) {
    // LOGGER.warn("Multiple CXF services in model. We only support one service per WSDL.");
    // }
    // if (!services.isEmpty()) {
    // context.config.setService(services.get(0));
    // }
  }

}
