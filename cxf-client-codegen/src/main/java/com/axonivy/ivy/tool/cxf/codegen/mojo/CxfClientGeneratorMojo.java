package com.axonivy.ivy.tool.cxf.codegen.mojo;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

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

  /** URI or Path to a WSDL file */
  @Parameter(property = "ivy.generate.webservice.client.wsdl", required = true)
  String wsdl;

  // TODO: doc
  @Parameter(property = "ivy.generate.webservice.client.nsMappings")
  Map<String, String> nsMappings;

  // TODO: doc
  @Parameter(property = "ivy.generate.webservice.client.underscoreNames", defaultValue = "false")
  Boolean underscoreNames;

  @Override
  public void execute() throws MojoExecutionException {
    if (skipGenerate) {
      return;
    }
    getLog().info("Generating CXF client sources...");


    //     var targetJar = FilePath.of(IvyConstants.DIRECTORY_LIB_WS_CLIENT).append(getClientJarName(context.config.getId()));
    ToolContext cxfContext = CxfClientGenerator.generate(wsdl, 
        clientJar -> getLog().info("Generated CXF client jar: " + clientJar), CxfClientGenerator.CodegenOpts.DEFAULT),
        new CodegenOpts(nsMappings, underscoreNames);
    System.out.println("Generated CXF client context: " + cxfContext);
//         tmpClientJar -> new JarProjectIntegratorService(tmpClientJar, context.project, targetJar)
//             .integrate(context.monitor),
//         new CodegenOpts(context.codegen.nsMappings(), context.codegen.underscoreNames()));

//     var services = CxfModelConverter.toWsConfigModel(cxfContext);
//     if (services.size() > 1) {
//       LOGGER.warn("Multiple CXF services in model. We only support one service per WSDL.");
//     }
//     if (!services.isEmpty()) {
//       context.config.setService(services.get(0));
//     }
  }

}
