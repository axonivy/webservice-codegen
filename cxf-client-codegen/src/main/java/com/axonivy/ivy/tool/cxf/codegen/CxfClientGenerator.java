package com.axonivy.ivy.tool.cxf.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.model.JavaServiceClass;
import org.apache.cxf.tools.wsdlto.WSDLToJava;
import org.apache.cxf.tools.wsdlto.WSDLToJavaContainer;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axonivy.ivy.tool.cxf.codegen.binding.IvyGeneratorBindings;
import com.axonivy.ivy.tool.cxf.codegen.binding.JAXWSBindingSerializer;
import com.axonivy.ivy.tool.cxf.codegen.fix.FixCXFSchemaLocation;
import com.axonivy.ivy.tool.cxf.codegen.model.ServiceModelWriter;
import com.axonivy.ivy.tool.cxf.codegen.ssl.InsecureSSL;

/**
 * Plain CXF client jar generator without any Eclipse or Ivy API involved.
 * @author rew
 * @since 7.1.0
 */
public class CxfClientGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(CxfClientGenerator.class);
  private final Path tmpGenDir;
  private boolean insecureSSL;

  public record CodegenOpts(
      String packageName,
      Map<String, String> nsMappings,
      boolean underscoreNames) {
    public static final CodegenOpts DEFAULT = new CodegenOpts(null, Map.of(), false);
  }

  public CxfClientGenerator(Path tmpGenDir) {
    this.tmpGenDir = tmpGenDir;
  }

  public CxfClientGenerator insecureSsl(boolean insecure) {
    this.insecureSSL = insecure;
    return this;
  }

  public ToolContext generate(String wsdlUri, CodegenOpts options) throws Exception {
    List<String> args = Arrays.asList(
        "-d", tmpGenDir.toAbsolutePath().toString(), // outputDir
        "-clientjar", tmpGenDir.resolve("client.jar").getFileName().toString(), // fake it till java sources are generated.
        "-autoNameResolution", // solve conflicts
        "-mark-generated", // @Generated annotation
        "-xjc-Xsetters", // JAXB2_basics plugin. Generates setter methods for collections
        "-xjc-Xcommons-lang", // JAXB2_commons_lang plugin. Generates toString, equals, hashCode methods
        "-xjc-Xcommons-lang:ToStringStyle=SHORT_PREFIX_STYLE",
        wsdlUri);

    Callable<ToolContext> generate = () -> {
      // should be fixed with cxf version 3.2.5
      JAXWSBindingSerializer.register(); // Bug fix for CXF-7695
      WSDLToJava cxfGenerator = new WSDLToJava(args.toArray(new String[args.size()]));
      ToolContext cxfContext = new ToolContext(){
        @Override
        public void remove(String key) {
          super.remove(key);
          if (ToolConstants.SERVICE_LIST.equals(key)) {
            // hack: pretend to be in client-jar mode so far: in order to get much of the local wsdl/xsd processing
            super.remove(ToolConstants.CFG_CLIENT_JAR);
          }
        }
      };

      var bindings = new IvyGeneratorBindings(tmpGenDir);
      cxfContext.put(ToolConstants.CFG_BINDING, bindings.getBindings(options));

      Optional.ofNullable(options.packageName())
          .filter(Predicate.not(String::isBlank))
          .ifPresent(pkg -> cxfContext.put(ToolConstants.CFG_PACKAGENAME, pkg));
      options.nsMappings().forEach((k, v) -> cxfContext.addNamespacePackageMap(k, v));
      cxfGenerator.run(cxfContext);

      generateLocalWsdl(tmpGenDir, cxfContext); // hack; officially only supported in client.jar mode
      FixCXFSchemaLocation.fixLocalWsdlIfNecessary(tmpGenDir); // Bug fix for CXF-7706
      moveLocalWsdlToService(tmpGenDir, cxfContext);
      bindings.cleanUp();

      new ServiceModelWriter().write(cxfContext.getJavaModel(), tmpGenDir);
      return cxfContext;
    };

    return withRedirectConfigurer(
        withSchemaAccProperty(
            withHttpPortFactory(
                new InsecureSSL(insecureSSL).withSSL(
                    generate)))).call();
  }

  private static void moveLocalWsdlToService(Path tmpGenDir, ToolContext cxfContext) throws Exception {
    List<String> serviceNs = cxfContext.getJavaModel().getServiceClasses().values().stream()
        .map(JavaServiceClass::getPackageName)
        .collect(Collectors.toList());
    if (serviceNs.size() != 1) {
      // keep wsdl in root: we don't know which service package to move it to
      return;
    }

    String path = serviceNs.get(0).replace(".", "/");
    Path serviceDirZip = tmpGenDir.resolve(path);
    try (Stream<Path> walker = Files.walk(tmpGenDir, 1)) {
      walker.filter(CxfClientGenerator::isSchemaFile)
          .forEach(schemaPath -> {
            try {
              Files.move(schemaPath, serviceDirZip.resolve(schemaPath.getFileName()));
            } catch (IOException ex) {
              LOGGER.warn("Failed to move service definition files", ex);
            }
          });
    }
  }

  private static void generateLocalWsdl(Path tmpGenDir, ToolContext cxfContext) throws Exception {
    var container = new WSDLToJavaContainer(null, null);
    var generateLocalWSDL = WSDLToJavaContainer.class.getDeclaredMethod("generateLocalWSDL", ToolContext.class);
    cxfContext.put(ToolConstants.CFG_CLASSDIR, tmpGenDir.toAbsolutePath().toString());
    generateLocalWSDL.setAccessible(true);
    generateLocalWSDL.invoke(container, cxfContext);
  }

  private static boolean isSchemaFile(Path zipPath) {
    String entry = zipPath.toString().toLowerCase();
    return entry.endsWith(".wsdl") || entry.endsWith(".xsd");
  }

  private static <T> Callable<T> withHttpPortFactory(Callable<T> callable) {
    return () -> {
      IvyHTTPTransportFactory httpTransportFactory = new IvyHTTPTransportFactory();
      Map<String, DestinationFactory> original = new HashMap<>();
      try {
        original.putAll(httpTransportFactory.register());
        return callable.call();
      } finally {
        httpTransportFactory.reset(original);
      }
    };
  }

  private static Callable<ToolContext> withRedirectConfigurer(Callable<ToolContext> callable) {
    return () -> {
      Properties originalProps = (Properties) System.getProperties().clone();
      System.setProperty("http.autoredirect", Boolean.TRUE.toString());
      Bus originalBus = BusFactory.getDefaultBus(false);
      Bus bus = BusFactory.newInstance().createBus();
      BusFactory.setDefaultBus(bus);
      try {
        bus.setExtension(new RedirectConfigurer(), HTTPConduitConfigurer.class);
        return callable.call();
      } finally {
        bus.shutdown(true);
        BusFactory.setDefaultBus(originalBus);
        System.setProperties(originalProps);
      }
    };
  }

  private static <T> Callable<T> withSchemaAccProperty(Callable<T> callable) {
    return () -> {
      String externalSchemaAccessKey = "javax.xml.accessExternalSchema";
      String oldSchemaAccess = System.getProperty(externalSchemaAccessKey);
      try {
        System.setProperty(externalSchemaAccessKey, "all");
        return callable.call();
      } finally {
        resetProperty(externalSchemaAccessKey, oldSchemaAccess);
      }
    };
  }

  private static void resetProperty(String externalSchemaAccessKey, String oldSchemaAccess) {
    if (oldSchemaAccess == null) {
      System.clearProperty(externalSchemaAccessKey);
    } else {
      System.setProperty(externalSchemaAccessKey, oldSchemaAccess);
    }
  }

  private static class RedirectConfigurer implements HTTPConduitConfigurer {
    @Override
    public void configure(String name, String address, HTTPConduit c) {
      HTTPClientPolicy client = c.getClient();
      if (client == null) {
        client = new HTTPClientPolicy();
      }

      TLSClientParameters tlsClientParameters = c.getTlsClientParameters();
      if (tlsClientParameters == null) {
        tlsClientParameters = new TLSClientParameters();
        tlsClientParameters.setUseHttpsURLConnectionDefaultHostnameVerifier(true);
        tlsClientParameters.setUseHttpsURLConnectionDefaultSslSocketFactory(true);
      }
      c.setTlsClientParameters(tlsClientParameters);

      client.setAutoRedirect(true);
      c.setClient(client);
    }
  }
}
