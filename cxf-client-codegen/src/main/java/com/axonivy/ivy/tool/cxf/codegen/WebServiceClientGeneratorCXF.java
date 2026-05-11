package com.axonivy.ivy.tool.cxf.codegen;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.tools.common.ToolContext;

import ch.ivyteam.ivy.IvyConstants;
import ch.ivyteam.ivy.java.restricted.JarProjectIntegratorService;
import ch.ivyteam.ivy.java.restricted.classpath.ClasspathService;
import ch.ivyteam.ivy.project.model.Project;
import ch.ivyteam.ivy.webservice.call.IWebserviceClientCodeGenerator;
import ch.ivyteam.ivy.webservice.exec.cxf.WebServiceCallLibraryCXF;
import ch.ivyteam.ivy.webservice.webserviceconfig.generated.WsConfig;
import ch.ivyteam.log.Logger;
import ch.ivyteam.util.io.resource.FilePath;
import ch.ivyteam.util.io.resource.Resource;

public class WebServiceClientGeneratorCXF implements IWebserviceClientCodeGenerator {

  private static final Logger LOGGER = Logger.getLogger(WebServiceClientGeneratorCXF.class);

  @Override
  public boolean isResponsible(String wsClientLibraryId) {
    return WebServiceCallLibraryCXF.ID.equals(wsClientLibraryId);
  }

  @Override
  public String suggestPackageName(String namespaceInWsdl) {
    String namespace = StringUtils.isBlank(namespaceInWsdl) ? "undefined.targetnamespace" : namespaceInWsdl;
    return PackageUtils.parsePackageName(namespace, "") + ".client";
  }

  @Override
  public void generateClient(GeneratorContext context) throws Exception {
    var targetJar = FilePath.of(IvyConstants.DIRECTORY_LIB_WS_CLIENT).append(getClientJarName(context.config.getId()));
    ToolContext cxfContext = CxfClientGenerator.generate(context.wsdl.toASCIIString(),
        tmpClientJar -> new JarProjectIntegratorService(tmpClientJar, context.project, targetJar)
            .integrate(context.monitor),
        new CodegenOpts(context.codegen.nsMappings(), context.codegen.underscoreNames()));

    var services = CxfModelConverter.toWsConfigModel(cxfContext);
    if (services.size() > 1) {
      LOGGER.warn("Multiple CXF services in model. We only support one service per WSDL.");
    }
    if (!services.isEmpty()) {
      context.config.setService(services.get(0));
    }
  }

  private static String getClientJarName(String webServiceId) {
    return "cxfClient_" + webServiceId + ".jar";
  }

  @Override
  public void removeClient(Project project, WsConfig config, Consumer<Resource> verifier) {
    var libWsDir = project.fs().root().folder(IvyConstants.DIRECTORY_LIB_WS_CLIENT);
    var clientJar = libWsDir.file(getClientJarName(config.getId()));
    if (clientJar.exists()) {
      verifier.accept(clientJar);
      clientJar.delete();
      new ClasspathService(project).removeJar(clientJar);
    }
  }

}
