package com.axonivy.ivy.tool.cxf.codegen.mojo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// import javax.jws.WebParam.Mode;

// import org.apache.commons.lang3.ArrayUtils;
// import org.apache.commons.lang3.Strings;
// import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.cxf.tools.common.ToolContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
// import org.osgi.framework.BundleException;

import com.axonivy.ivy.tool.cxf.codegen.CxfClientGenerator;

// import ch.ivyteam.eclipse.util.EclipsePlatformUtils;
// import ch.ivyteam.ivy.webservice.call.IWebserviceClientCodeGenerator.CodegenOpts;
// import ch.ivyteam.ivy.webservice.datamodel.IWebServiceOperation;
// import ch.ivyteam.ivy.webservice.datamodel.WebServiceOperationsCollector;
// import ch.ivyteam.ivy.webservice.datamodel.WsParameterDesc;
// import ch.ivyteam.ivy.webservice.exec.cxf.codegen.CxfClientGenerator;
// import ch.ivyteam.ivy.webservice.exec.cxf.codegen.CxfModelConverter;
// import ch.ivyteam.ivy.webservice.webserviceconfig.generated.WsPort;
// import ch.ivyteam.ivy.webservice.webserviceconfig.generated.WsService;
// import ch.ivyteam.util.net.SSLUtil;

@SuppressWarnings("restriction")
class TestCxfClientCodegen {

  @TempDir
  Path tmpDir;

  @Test
  void generateEchoServiceOnline(@TempDir Path tmpDir) throws Exception {
    var meta = CxfClientGenerator.generate(
        "http://test-webservices.ivyteam.io:8080/axis2/services/IvyEchoService?wsdl", tmpDir, 
        CxfClientGenerator.CodegenOpts.DEFAULT);

    assertThat(meta.getJavaModel().getServiceClasses()).containsOnlyKeys("IvyEchoService");

    var servicePath = tmpDir.resolve("ch/ivyteam/test/ws");
    assertThat(servicePath.resolve("IvyEchoService.java")).exists();
    assertThat(servicePath.resolve("IvyEchoServicePortType.java")).exists();
    assertThat(servicePath.resolve("IvyEchoService.wsdl"))
        .as("contains service descriptor as offline resources")
        .exists();
  }

  // @Test
  // void generateEchoServiceOnline_https() throws Exception {
  //   var fileRef = new AtomicReference<>(Path.of(""));
  //   boolean insecure = SSLUtil.isInsecureSSLenabled();
  //   assertThat(insecure).isFalse();
  //   try {
  //     SSLUtil.enableInsecureSSL(true);
  //     ToolContext meta = generate("https://test-webservices.ivyteam.io:8443/axis2/services/IvyEchoService?wsdl",
  //         client -> {
  //           assertThat(client).exists();
  //           List<Path> entries = getJarContents(client);
  //           Path servicePath = entries.get(0).resolve("ch/ivyteam/test/ws/");
  //           assertThat(entries).as("contains service descriptor as offline resources").contains(servicePath.resolve("IvyEchoService.wsdl"));
  //           assertThat(entries).as("contains service binary").contains(servicePath.resolve("IvyEchoService.class"));
  //           assertThat(entries).as("contains service source").contains(servicePath.resolve("IvyEchoService.java"));
  //           fileRef.set(client);
  //         });
  //     assertThat(meta.getJavaModel().getServiceClasses()).containsOnlyKeys("IvyEchoService");
  //     assertThat(fileRef.get()).as("client jar is cleaned up after consumption").doesNotExist();
  //   } finally {
  //     SSLUtil.enableInsecureSSL(insecure);
  //   }
  // }

  @Test
  void generateWithPackageMapping(@TempDir Path tmpDir) throws Exception {
    Map<String, String> mapping = new HashMap<>();
    String packageName = "ch.ivyteam.testmapping.service";
    mapping.put("urn:ws.test.ivyteam.ch", packageName);
    mapping.put("urn:schema.ws.test.ivyteam.ch", "ch.ivyteam.testmapping.schema");
    ToolContext meta = CxfClientGenerator.generate(
      "http://test-webservices.ivyteam.io:8080/axis2/services/IvyEchoService?wsdl", tmpDir,
        new CxfClientGenerator.CodegenOpts(mapping, false));


    String pathPrefix = (packageName + "/").replace('.', '/');
    Path servicePath = tmpDir.resolve(pathPrefix);
    assertThat(servicePath.resolve("IvyEchoService.java")).exists();
    assertThat(servicePath.resolve("IvyEchoService.wsdl")).exists();
    assertThat(meta.getJavaModel().getServiceClasses())
      .containsOnlyKeys("IvyEchoService");
  }

  // private static List<Path> getJarContents(Path clientJar) {
  //   URI uri = URI.create("jar:" + clientJar.toUri());
  //   try (FileSystem zipFs = FileSystems.newFileSystem(uri, Map.of())) {
  //     return Files.walk(zipFs.getPath("/")).collect(Collectors.toList());
  //   } catch (IOException ex) {
  //     throw new RuntimeException("Failed to assert client JAR content", ex);
  //   }
  // }

  // @Test
  // void executeGeneratedClient() throws Exception {
  //   generate("http://test-webservices.ivyteam.io:8080/axis2/services/IvyEchoService?wsdl",
  //       client -> {
  //         try (URLClassLoader clientCl = newClientJarClassLoader(client)) {
  //           Class<?> serviceClass = clientCl.loadClass("ch.ivyteam.test.ws.IvyEchoService");
  //           Object instance = serviceClass.getDeclaredConstructor().newInstance();
  //           Object endpoint = MethodUtils.invokeMethod(instance, "getIvyEchoServiceHttpSoap11Endpoint");
  //           Class<?> requestClass = clientCl.loadClass("ch.ivyteam.test.ws.schema.EchoString");
  //           Object request = requestClass.getDeclaredConstructor().newInstance();
  //           MethodUtils.invokeMethod(request, "setMessage", "hi CXF");
  //           Object result = MethodUtils.invokeMethod(endpoint, "echoString", request);
  //           Class<?> responseClass = clientCl.loadClass("ch.ivyteam.test.ws.schema.EchoStringResponse");
  //           assertThat(result).isInstanceOf(responseClass);
  //           Object returnValue = MethodUtils.invokeMethod(result, "getReturn");
  //           assertThat(returnValue).isEqualTo("hi CXF");
  //         } catch (Exception ex) {
  //           throw new RuntimeException("failed to invoke CXF service", ex);
  //         }
  //       });
  // }

  // /**
  //  * Regression test that verifies that our preliminary solution to generate correct exception type works.
  //  * We filed a fix for CXF 3.2.2-SNAPSHOT
  //  *
  //  * ISSUE XIVY-2399 Fix Code generation error when service contains exception types.
  //  * ISSUE CXF-6134 Apache CXF generating constructor with duplicate argument names causing compilation error
  //  * @throws Exception
  //  */
  // @Test
  // void generatePortalConnector() throws Exception {
  //   generate(this.getClass().getResource("portalWebStartable.wsdl").toString(), client -> {
  //     assertThat(client).exists();
  //     List<Path> entries = getJarContents(client);
  //     Path service = entries.get(0).resolve("ch/ivy/ws/addon");
  //     assertThat(entries).contains(service.resolve("Exception.class"));
  //     assertThat(entries).contains(service.resolve("Throwable.class"));
  //     assertThat(entries).contains(service.resolve("WebServiceProcessTechnicalException.class"));
  //   });
  // }

  // /**
  //  * ISSUE XIVY-2426 Inscribe native Webservice types with well known ivy scripting types
  //  * @throws Exception
  //  */
  // @Test
  // void generateNativeXmlTypes() throws Exception {
  //   generate(this.getClass().getResource("nativeXsdTypes.wsdl").toString(), client -> {
  //     try (URLClassLoader clientCl = newClientJarClassLoader(client)) {
  //       Class<?> nativeParams = clientCl.loadClass("ch.ivyteam.testservice.types.XsdNativeTypes");
  //       assertThat(getParamType(nativeParams, "dateTime"))
  //           .isEqualTo(ch.ivyteam.ivy.scripting.objects.DateTime.class.getName());
  //       assertThat(getParamType(nativeParams, "time"))
  //           .isEqualTo(ch.ivyteam.ivy.scripting.objects.Time.class.getName());
  //       assertThat(getParamType(nativeParams, "date"))
  //           .isEqualTo(ch.ivyteam.ivy.scripting.objects.Date.class.getName());
  //     } catch (Exception ex) {
  //       throw new RuntimeException("failed to invoke CXF service", ex);
  //     }
  //   });
  // }

  // /**
  //  * ISSUE XIVY-2856 Call WS methods that can only be generated in wrapper style
  //  * @throws Exception
  //  */
  // @Test
  // void generateCuraden_inOutParams() throws Exception {
  //   var jarKeeper = new JarKeeper();
  //   ToolContext context = generate(getClass().getResource("curadenUntouched.wsdl").toString(), jarKeeper::keep);

  //   List<WsService> services = CxfModelConverter.toWsConfigModel(context);
  //   assertThat(services).isNotEmpty();

  //   WsService service = services.iterator().next();
  //   try (URLClassLoader classloader = craftClassloader(jarKeeper.clientJar)) {
  //     var collector = new WebServiceOperationsCollector(classloader, service.getServiceClass());

  //     var operations = collector.getOperations("asAxonIVYObj");
  //     var firstOp = findOperation(operations, "getLanguage");

  //     assertThat(firstOp.getName()).isEqualTo("getLanguage");
  //     assertThat(firstOp.getParameters()).isNotEmpty();
  //     assertThat(firstOp.getParameters().stream().filter(param -> param.getMode() == Mode.IN).map(WsParameterDesc::getName))
  //         .as("params without a mode are 'normal' input params")
  //         .contains("ipSprcd");

  //     assertThat(firstOp.getParameters().stream()
  //         .filter(param -> param.getMode() != null)
  //         .map(WsParameterDesc::getName))
  //             .as("params with non 'input' mode are still recognized as such")
  //             .contains("opERROR", "opMESSAGE", "ttSprcd");

  //     assertThat(firstOp.getResultType()).isEqualTo("void");
  //   }
  // }

  // @Test
  // void generate_listSetter_toString_equals_hashCode() throws Exception {
  //   generate(this.getClass().getResource("listAndBoolean.wsdl").toString(), client -> {
  //     try (URLClassLoader clientCl = newClientJarClassLoader(client)) {
  //       Class<?> requestClass = clientCl.loadClass("wsbindin.Call");
  //       assertThat(requestClass).hasDeclaredMethods(
  //           "getNames", "setNames",
  //           "isMale", "setMale",
  //           "toString", "equals", "hashCode");

  //       Method getterMethod = getMethod(requestClass, "getNames");
  //       assertThat(getterMethod.getReturnType()).isEqualTo(List.class);

  //       Method setterMethod = getMethod(requestClass, "setNames");
  //       assertThat(setterMethod.getParameterTypes()).containsOnly(List.class);

  //       Class<?> responseClass = clientCl.loadClass("wsbindin.CallResponse");
  //       assertThat(responseClass).hasDeclaredMethods(
  //           "toString", "equals", "hashCode");

  //     } catch (Exception ex) {
  //       throw new RuntimeException(ex);
  //     }
  //   });
  // }

  // private Method getMethod(Class<?> requestClass, String methodName) {
  //   return Arrays.stream(requestClass.getDeclaredMethods())
  //       .filter(method -> methodName.equals(method.getName()))
  //       .findAny().orElse(null);
  // }

  // private static URLClassLoader newClientJarClassLoader(Path client)
  //     throws BundleException, IOException, MalformedURLException {
  //   URL[] scriptingCp = EclipsePlatformUtils.getClassPath(EclipsePlatformUtils.getBundle(ch.ivyteam.ivy.scripting.objects.DateTime.class));
  //   URL[] cleanCp = Arrays.stream(scriptingCp)
  //       .filter(url -> new File(url.getFile()).exists()) // test setup related: OSGI.jar uri does not exists and must therefore be filtered.
  //       .toArray(URL[]::new);
  //   return new URLClassLoader(ArrayUtils.add(cleanCp, client.toUri().toURL()), WSDLToJava.class.getClassLoader());
  // }

  // private static String getParamType(Class<?> nativeParams, String fieldName) throws NoSuchFieldException {
  //   return nativeParams.getDeclaredField(fieldName).getType().getName();
  // }

  // @Test
  // void generateWithComplexTypes() throws Exception {
  //   String packageName = "com.microsoft.schemas.serialization.arrays";
  //   String packageName2 = "com.microsoft.schemas._2003._10.serialization";
  //   Map<String, String> mapping = new HashMap<>();
  //   mapping.put("http://schemas.microsoft.com/2003/10/Serialization/Arrays", packageName);
  //   AtomicReference<Path> fileRef = new AtomicReference<>(Path.of(""));
  //   ToolContext meta = CxfClientGenerator.generate(this.getClass().getResource("infoshare/InfoShare.wsdl").toString(),
  //       client -> {
  //         assertThat(client).exists();
  //         String pathPrefix = (packageName + "/").replace('.', '/');
  //         String pathPrefix2 = (packageName2 + "/").replace('.', '/');
  //         List<Path> entries = getJarContents(client);
  //         Path root = entries.get(0);
  //         Path servicePath = root.resolve("com/kendox/infoshare");
  //         assertThat(entries).as("contains service descriptor as offline resources")
  //             .contains(servicePath.resolve("InfoShare.wsdl"));
  //         assertThat(entries).as("contains service descriptor imports as offline resources")
  //             .contains(servicePath.resolve("schema1.xsd"));
  //         assertThat(entries).as("contains mapped type class").contains(root.resolve(pathPrefix + "ArrayOfstring.class"));
  //         assertThat(entries).as("contains non-mapped (default) type class").contains(root.resolve(pathPrefix2 + "ObjectFactory.class"));
  //         fileRef.set(client);
  //       },
  //       new CodegenOpts(mapping, false));
  //   assertThat(meta.getJavaModel().getServiceClasses()).containsOnlyKeys("InfoShare");
  // }

  // @Test
  // void translateCxfGeneratorMetaToConfigModel() throws Exception {
  //   var wsdlFile = createLocalWsdl();
  //   var jarKeeper = new JarKeeper();
  //   ToolContext meta = generate(wsdlFile.toUri().toASCIIString(), jarKeeper::keep);
  //   List<WsService> services = CxfModelConverter.toWsConfigModel(meta);
  //   assertThat(services.size()).isEqualTo(1);
  //   WsService echoService = services.get(0);
  //   assertThat(echoService.getServiceClass()).isEqualTo("ch.ivyteam.test.ws.IvyEchoService");
  //   assertThat(echoService.getPorts().stream().map(WsPort::getName)).containsOnly(
  //       "IvyEchoServiceHttpsEndpoint",
  //       "IvyEchoServiceHttpsSoap11Endpoint",
  //       "IvyEchoServiceHttpsSoap12Endpoint",
  //       "IvyEchoServiceHttpEndpoint",
  //       "IvyEchoServiceHttpSoap11Endpoint",
  //       "IvyEchoServiceHttpSoap12Endpoint");
  //   assertThat(echoService.getPorts().stream().map(WsPort::getLocationUri)).containsOnly(
  //       "https://test-webservices.ivyteam.io:8443/axis2/services/IvyEchoService.IvyEchoServiceHttpsSoap11Endpoint/",
  //       "http://test-webservices.ivyteam.io:8080/axis2/services/IvyEchoService.IvyEchoServiceHttpSoap11Endpoint/",
  //       "https://test-webservices.ivyteam.io:8443/axis2/services/IvyEchoService.IvyEchoServiceHttpsSoap12Endpoint/",
  //       "http://test-webservices.ivyteam.io:8080/axis2/services/IvyEchoService.IvyEchoServiceHttpSoap12Endpoint/",
  //       "http://test-webservices.ivyteam.io:8080/axis2/services/IvyEchoService.IvyEchoServiceHttpEndpoint/",
  //       "https://test-webservices.ivyteam.io:8443/axis2/services/IvyEchoService.IvyEchoServiceHttpsEndpoint/");
  //   WsPort soap11 = echoService.getPorts().get(0);
  //   assertThat(soap11.getLocationUri()).contains("test-webservices.ivyteam.io:8443");
  //   assertThat(soap11.getName()).isEqualTo("IvyEchoServiceHttpsSoap11Endpoint");
  //   WsService service = services.iterator().next();

  //   try (URLClassLoader classloader = craftClassloader(jarKeeper.clientJar)) {
  //     var collector = new WebServiceOperationsCollector(classloader, service.getServiceClass());
  //     var operations = collector.getOperations("IvyEchoServiceHttpsSoap11Endpoint");
  //     assertThat(operations.stream().map(IWebServiceOperation::getName)).containsOnly(
  //         "waitFor", "getSessionId", "echoDate", "getHelloMessage",
  //         "returnNullObject", "returnVoid", "logMessageToStdOut", "echoObject", "echoString");
  //     var echoString = findOperation(operations, "echoString");
  //     assertThat(inputParams(echoString)).hasSize(1);
  //     assertThat(inputParams(echoString).get(0).getName()).isEqualTo("parameters");
  //     assertThat(inputParams(echoString).get(0).getType().getName())
  //         .isEqualTo("ch.ivyteam.test.ws.schema.EchoString");
  //     assertThat(echoString.getResultType()).isEqualTo("ch.ivyteam.test.ws.schema.EchoStringResponse");
  //   }
  // }

  // // ISSUE XIVY-3546 CXF WebService Creation with undefined element
  // @Test
  // void generateWithUndefinedNames() throws Exception {
  //   var jarKeeper = new JarKeeper();
  //   ToolContext context = generate(getClass().getResource("undefinedElement.wsdl").toString(), jarKeeper::keep);

  //   List<WsService> services = CxfModelConverter.toWsConfigModel(context);
  //   assertThat(services).isNotEmpty();

  //   WsService service = services.iterator().next();
  //   try (URLClassLoader classloader = craftClassloader(jarKeeper.clientJar)) {
  //     var collector = new WebServiceOperationsCollector(classloader, service.getServiceClass());

  //     var operations = collector.getOperations("PersonServicePort");
  //     var firstOp = findOperation(operations, "addPerson");

  //     assertThat(firstOp.getName()).isEqualTo("addPerson");
  //     assertThat(inputParams(firstOp))
  //         .hasSize(1)
  //         .allMatch(param -> "parameters".equals(param.getName()));
  //     assertThat(firstOp.getResultType()).contains("AddPersonResponse");
  //   }
  // }

  // private IWebServiceOperation findOperation(List<IWebServiceOperation> operations, String name) {
  //   return operations.stream()
  //       .filter(operation -> operation.getName().equals(name))
  //       .findAny()
  //       .orElseThrow(() -> new RuntimeException("could not find operation " + name));
  // }

  // private List<WsParameterDesc> inputParams(IWebServiceOperation op) {
  //   return op.getParameters().stream()
  //       .filter(p -> p.getMode() == Mode.IN || p.getMode() == Mode.INOUT)
  //       .collect(Collectors.toList());
  // }

  // /**
  //  * ISSUE XIVY-3117 CXF WebService Creation with included xsd
  //  * @throws Exception
  //  *
  //  *           Fetching the initial WSDL and handling optional redirects to HTTPS is
  //  *           well supported by our CXF client generator.
  //  *
  //  *           However, for fetching internal XSD refs to HTTP, we need additional flags.
  //  *
  //  *           e.g. <xs:include schemaLocation="http://test-webservices.ivyteam.io/wsdl/geresResidentWsdl/GeresResidentInfo.xsd" />
  //  *           which redirects to 'https://test-webservices.ivyteam.io/wsdl/geresResidentWsdl/GeresResidentInfo.xsd
  //  *
  //  *           seeCxfClientGenerator.withRedirectConfigurer
  //  *
  //  */
  // @Test
  // void generateGeres_httpsRedirect() throws Exception {
  //   var jarKeeper = new JarKeeper();

  //   Path httpsWsdl = httpsXsdRedirect();
  //   ToolContext context = generate(httpsWsdl.toString(), jarKeeper::keep);

  //   List<WsService> services = CxfModelConverter.toWsConfigModel(context);
  //   assertThat(services).isNotEmpty();

  //   WsService service = services.iterator().next();
  //   try (URLClassLoader classloader = craftClassloader(jarKeeper.clientJar)) {
  //     var collector = new WebServiceOperationsCollector(classloader, service.getServiceClass());
  //     var operations = collector.getOperations("ResidentInfoPort");
  //     var firstOp = findOperation(operations, "ResidentInfoFast");
  //     assertThat(firstOp.getName()).isEqualTo("ResidentInfoFast");
  //     assertThat(inputParams(firstOp))
  //         .hasSize(1)
  //         .allMatch(param -> "parameters".equals(param.getName()));
  //     assertThat(firstOp.getResultType()).contains("ResidentInfoFastResponse");
  //   }
  // }

  // private Path httpsXsdRedirect() throws IOException {
  //   var httpsWsdl = tmpDir.resolve("residentInfo.wsdl");
  //   try (var is = TestCxfClientCodegen.class.getResourceAsStream("geres/GeresResidentInfo_v1801.wsdl")) {
  //     var wsdl = new String(is.readAllBytes(), StandardCharsets.UTF_8);
  //     wsdl = Strings.CS.replace(wsdl,
  //         "<xs:include schemaLocation=\"GeresResidentInfo.xsd\" />",
  //         "<xs:include schemaLocation=\"http://test-webservices.ivyteam.io/wsdl/geresResidentWsdl/GeresResidentInfo.xsd\" />");
  //     Files.writeString(httpsWsdl, wsdl);
  //   }
  //   return httpsWsdl;
  // }

  // // ISSUE XIVY-3193 CXF generated WSDL pointing to local included XSD file.
  // @Test
  // void generateGeres_localIncludedXSD() throws Exception {
  //   generate(this.getClass().getResource("geres/GeresResidentInfo_v1801.wsdl").toString(),
  //       client -> {
  //         assertThat(client).exists();
  //         List<Path> entries = getJarContents(client);
  //         Path root = entries.get(0);
  //         Path servicePath = root.resolve("ch/bedag/geres/schemas/_20180101/geresresidentinfoservice");
  //         Path wsdlPath = servicePath.resolve("GeresResidentInfo_v1801.wsdl");
  //         assertThat(entries).as("contains offline WSDL").contains(wsdlPath);
  //         String wsdlContent = getFileContentFromJarFile(client, "GeresResidentInfo_v1801.wsdl");
  //         assertThat(wsdlContent).as("contains offline XSD")
  //             .containsPattern("xs:include schemaLocation=\"schema[\\d]+.xsd\"");
  //       });
  // }

  // // ISSUE XIVY-3586 CXF fails to include ObjectFactory file for empty targetNamespace in jar file.
  // @Test
  // void generateService_NoTargetNamespace() throws Exception {
  //   var jarKeeper = new JarKeeper();
  //   ToolContext context = generate(
  //       getClass().getResource("noTargetNamespace.wsdl").toString(),
  //       client -> {
  //         assertThat(client).exists();
  //         List<Path> entries = getJarContents(client);
  //         Path service = entries.get(0).resolve("generated");
  //         assertThat(entries).contains(service.resolve("ObjectFactory.class"));
  //         jarKeeper.keep(client);
  //       });

  //   List<WsService> services = CxfModelConverter.toWsConfigModel(context);
  //   assertThat(services).isNotEmpty();

  //   WsService service = services.iterator().next();

  //   try (URLClassLoader classloader = craftClassloader(jarKeeper.clientJar)) {
  //     var collector = new WebServiceOperationsCollector(classloader, service.getServiceClass());
  //     var operations = collector.getOperations("BasicHttpBinding_IService1");

  //     var firstOp = findOperation(operations, "GetDataUsingDataContract");
  //     assertThat(firstOp.getName()).isEqualTo("GetDataUsingDataContract");
  //     assertThat(inputParams(firstOp))
  //         .hasSize(1)
  //         .allMatch(param -> "parameters".equals(param.getName()));
  //     assertThat(firstOp.getResultType()).contains("org.tempuri.GetDataUsingDataContractResponse");
  //   }
  // }

  // /**
  //  * ISSUE XIVY-13809 Handle underline type-defs in CXF soap client.
  //  * @throws Exception
  //  */
  // @Test
  // void generateUnderscoreNames() throws Exception {
  //   boolean underscoreAsChar = true;
  //   CxfClientGenerator.generate(getClass().getResource("underscored.wsdl").toString(), client -> {
  //     try (URLClassLoader clientCl = newClientJarClassLoader(client)) {
  //       Class<?> bookType = clientCl.loadClass("com.cleverbuilder.bookservice.Book");
  //       assertThat(bookType.getDeclaredMethod("getPRICEDATE"))
  //           .as("getter for type natively without underscores")
  //           .isNotNull();
  //       assertThat(bookType.getDeclaredMethod("getPRICE_DATE"))
  //           .as("getter for type natively with underscore")
  //           .isNotNull();
  //     } catch (Exception ex) {
  //       throw new RuntimeException("failed to invoke CXF service", ex);
  //     }
  //   }, new CodegenOpts(Map.of(), underscoreAsChar));
  // }

  private Path createLocalWsdl() throws IOException, FileNotFoundException {
    var localWsdl = tmpDir.resolve("IvyEchoService.WSDL");
    try (var is = TestCxfClientCodegen.class.getResourceAsStream("IvyEchoService.WSDL");
        var out = Files.newOutputStream(localWsdl)) {
      is.transferTo(out);
    }
    return localWsdl;
  }

  private static String getFileContentFromJarFile(Path jarFile, String fileName) {
    var uri = URI.create("jar:" + jarFile.toUri());
    try (FileSystem zipFs = FileSystems.newFileSystem(uri, Map.of())) {
      var file = Files.walk(zipFs.getPath("/")).filter(path -> path.endsWith(fileName)).findFirst().get();
      return new String(Files.readAllBytes(file), Charset.forName("UTF-8"));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private class JarKeeper {
    private final Path clientJar = tmpDir.resolve("client.jar");

    public void keep(Path tmpClientJar) {
      try {
        Files.copy(tmpClientJar, clientJar);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  private static final URLClassLoader craftClassloader(Path clientJar) {
    try {
      var urlClasspathEntries = List.of(clientJar.toUri().toURL());
      return new URLClassLoader(urlClasspathEntries.toArray(URL[]::new), TestCxfClientCodegen.class.getClassLoader());
    } catch (Throwable ex) {
      throw new RuntimeException("Failed to resolve classpath", ex);
    }
  }

  public static ToolContext generate(String wsdlUri, Consumer<Path> clientJarUser) throws Exception {
    return CxfClientGenerator.generate(wsdlUri, clientJarUser, CxfClientGenerator.CodegenOpts.DEFAULT);
  }
}
