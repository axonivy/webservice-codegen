package com.axonivy.ivy.tool.cxf.codegen;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.model.JavaPort;
import org.apache.cxf.tools.common.model.JavaServiceClass;

import ch.ivyteam.ivy.webservice.webserviceconfig.generated.WsPort;
import ch.ivyteam.ivy.webservice.webserviceconfig.generated.WsService;

public class CxfModelConverter {
  private final ToolContext cxfContext;

  public CxfModelConverter(ToolContext cxfContext) {
    this.cxfContext = cxfContext;
  }

  public static List<WsService> toWsConfigModel(ToolContext cxfContext) {
    return new CxfModelConverter(cxfContext).toConfigServices();
  }

  private List<WsService> toConfigServices() {
    return getCxfJavaServices()
        .stream()
        .map(this::toConfigService)
        .collect(Collectors.toList());
  }

  private Collection<JavaServiceClass> getCxfJavaServices() {
    return cxfContext
        .getJavaModel()
        .getServiceClasses()
        .values();
  }

  private WsService toConfigService(JavaServiceClass cxfJavaService) {
    var configService = new WsService();
    configService.setServiceClass(cxfJavaService.getFullClassName());

    cxfJavaService.getPorts()
        .stream()
        .map(this::toConfigPort)
        .forEach(port -> configService.getPorts().add(port));
    return configService;
  }

  private WsPort toConfigPort(JavaPort cxfJavaPort) {
    var configPort = new WsPort();
    configPort.setLocationUri(cxfJavaPort.getBindingAdress());
    configPort.setName(cxfJavaPort.getName());
    return configPort;
  }
}
