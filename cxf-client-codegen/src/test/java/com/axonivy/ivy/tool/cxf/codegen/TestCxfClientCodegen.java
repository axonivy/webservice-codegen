package com.axonivy.ivy.tool.cxf.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.cxf.tools.common.ToolContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockserver.integration.ClientAndServer;

import com.axonivy.ivy.tool.cxf.codegen.binding.IvyGeneratorBindings;

public class TestCxfClientCodegen {

  @TempDir
  Path tmpDir;

  private static String mockBaseUrl;
  private static ClientAndServer mock;

  @BeforeAll
  static void startHttp() {
    Integer[] ports = IntStream.rangeClosed(3333, 3333 + 20).boxed().toArray(Integer[]::new);
    mock = new ClientAndServer(ports);
    mockBaseUrl = "http://localhost:" + mock.getPort();
    mockResources();
  }

  @AfterAll
  static void stopHttp() {
    mock.stop();
  }

  static void mockResources() {
    mock.when(request())
        .respond(httpRequest -> {
          var resourcePath = httpRequest.getPath().getValue().replaceFirst("^/wsdl/", "");
          try (var is = TestCxfClientCodegen.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
              return response().withStatusCode(404);
            }
            return response().withStatusCode(200).withBody(is.readAllBytes());
          }
        });
  }

  @Test
  void generateEchoServiceFromWebResource() throws Exception {
    var meta = new CxfClientGenerator(tmpDir).generate(
        mockBaseUrl + "/wsdl/IvyEchoService.WSDL",
        CxfClientGenerator.CodegenOpts.DEFAULT);

    assertThat(meta.getJavaModel().getServiceClasses()).containsOnlyKeys("IvyEchoService");

    var servicePath = tmpDir.resolve("ch/ivyteam/test/ws");
    assertThat(servicePath.resolve("IvyEchoService.java")).exists();
    assertThat(servicePath.resolve("IvyEchoServicePortType.java")).exists();
    assertThat(servicePath.resolve("IvyEchoService.wsdl"))
        .as("contains service descriptor as offline resources")
        .exists();
  }

  @Test
  void generate_https() throws Exception {
    String httpsWsdl = "https://localhost:" + mock.getPort() + "/wsdl/IvyEchoService.WSDL";
    var meta = new CxfClientGenerator(tmpDir)
        .insecureSsl(true)
        .generate(httpsWsdl, CxfClientGenerator.CodegenOpts.DEFAULT);
    assertThat(meta.getJavaModel().getServiceClasses())
        .as("opt-in insecure SSL context allows fetching from HTTPS endpoints with self-signed certificates")
        .containsOnlyKeys("IvyEchoService");
  }

  @Test
  void generate_https_rejectInsecureByDefault() throws Exception {
    String httpsWsdl = "https://localhost:" + mock.getPort() + "/wsdl/IvyEchoService.WSDL";
    assertThatThrownBy(() -> new CxfClientGenerator(tmpDir).generate(
        httpsWsdl, CxfClientGenerator.CodegenOpts.DEFAULT))
            .as("WSDL and XSD resources are by default only fetched from verified HTTPS endpoints")
            .isInstanceOf(Exception.class);
  }

  @Test
  void generateWithMultiNamespacePackageMapping() throws Exception {
    Map<String, String> mapping = new HashMap<>();
    String packageName = "ch.ivyteam.testmapping.service";
    mapping.put("urn:ws.test.ivyteam.ch", packageName);
    mapping.put("urn:schema.ws.test.ivyteam.ch", "ch.ivyteam.testmapping.schema");
    ToolContext meta = new CxfClientGenerator(tmpDir).generate(
        mockBaseUrl + "/wsdl/IvyEchoService.WSDL",
        new CxfClientGenerator.CodegenOpts(null, mapping, false, false));

    String pathPrefix = (packageName + "/").replace('.', '/');
    Path servicePath = tmpDir.resolve(pathPrefix);
    assertThat(servicePath.resolve("IvyEchoService.java")).exists();
    assertThat(servicePath.resolve("IvyEchoService.wsdl")).exists();
    assertThat(meta.getJavaModel().getServiceClasses())
        .containsOnlyKeys("IvyEchoService");
  }

  /**
   * Regression test that verifies that our preliminary solution to generate correct exception type works.
   * We filed a fix for CXF 3.2.2-SNAPSHOT
   *
   * ISSUE XIVY-2399 Fix Code generation error when service contains exception types.
   * ISSUE CXF-6134 Apache CXF generating constructor with duplicate argument names causing compilation error
   * @throws Exception
   */
  @Test
  void generatePortalConnector() throws Exception {
    generateResource("portalWebStartable.wsdl", tmpDir);
    Path service = tmpDir.resolve("ch/ivy/ws/addon");
    assertThat(service.resolve("Exception.java")).exists();
    assertThat(service.resolve("Throwable.java")).exists();
    assertThat(service.resolve("WebServiceProcessTechnicalException.java")).exists();
  }

  /**
   * ISSUE XIVY-2426 Inscribe native Webservice types with well known ivy scripting types
   * @throws Exception
   */
  @Test
  void generateNativeXmlTypes() throws Exception {
    generateResource("nativeXsdTypes.wsdl", tmpDir);
    Path service = tmpDir.resolve("ch/ivyteam/testservice/types");

    Path natives = service.resolve("XsdNativeTypes.java");
    assertThat(natives).exists();
    assertThat(Files.readString(natives))
        .contains("import ch.ivyteam.ivy.scripting.objects.DateTime;")
        .contains("protected DateTime dateTime;")
        .contains("import ch.ivyteam.ivy.scripting.objects.Time;")
        .contains("protected Time time;")
        .contains("import ch.ivyteam.ivy.scripting.objects.Date;")
        .contains("protected Date date;");

    assertThat(tmpDir.resolve(IvyGeneratorBindings.GLOBAL_JAXB_BINDINGS_XML))
      .doesNotExist();
  }

  /**
   * ISSUE XIVY-2856 Call WS methods that can only be generated in wrapper style
   * @throws Exception
   */
  @Test
  void generateCuraden_inOutParams() throws Exception {
    var meta = generateResource("curadenUntouched.wsdl", tmpDir);

    var services = meta.getJavaModel().getServiceClasses();
    assertThat(services).hasSize(1);
    var service = services.entrySet().iterator().next().getValue();
    Path generated = tmpDir.resolve(service.getFullClassName().replace(".", "/") + ".java");
    var serviceImpl = Files.readString(generated);
    assertThat(serviceImpl).contains("public AsAxonIVYObj getAsAxonIVYObj() {");

    var axonIvyObj = Files.readString(generated.getParent().resolve("AsAxonIVYObj.java"));
    assertThat(axonIvyObj)
        .as("in and out parameters happily co-exist")
        .containsIgnoringWhitespaces("""
          public void getLanguage(
              @WebParam(partName = "ipSprcd", name = "ipSprcd")
              int ipSprcd,
              @WebParam(partName = "opERROR", mode = WebParam.Mode.OUT, name = "opERROR")
              javax.xml.ws.Holder<java.lang.Boolean> opERROR,
              @WebParam(partName = "opMESSAGE", mode = WebParam.Mode.OUT, name = "opMESSAGE")
              javax.xml.ws.Holder<java.lang.String> opMESSAGE,
              @WebParam(partName = "ttSprcd", mode = WebParam.Mode.OUT, name = "ttSprcd")
              javax.xml.ws.Holder<acticleinsert.asaxonivy.GetLanguageTtSprcdParam> ttSprcd
          ) throws FaultDetailMessage;""");
  }

  @Test
  void generate_listSetter_toString_equals_hashCode() throws Exception {
    generateResource("listAndBoolean.wsdl", tmpDir);
    assertThat(tmpDir).isNotEmptyDirectory();

    var impl = Files.readString(tmpDir.resolve("wsbindin").resolve("Call.java"));
    assertThat(impl)
        .as("list getters and setters")
        .contains(
            "public List<String> getNames()",
            "public void setNames(List<String> value)");

    assertThat(impl)
        .as("wrapper types getters and setters")
        .contains(
            "public Boolean isMale()",
            "public void setMale(Boolean value)");

    assertThat(impl)
        .as("implements common object identifiers")
        .contains(
            "public String toString()",
            "public boolean equals(Object that)",
            "public int hashCode()");
  }

  @Test
  void generateComplexTypes_nsMapped() throws Exception {
    String packageName = "com.microsoft.schemas.serialization.arrays";
    String packageName2 = "com.microsoft.schemas._2003._10.serialization";
    Map<String, String> mapping = new HashMap<>();
    mapping.put("http://schemas.microsoft.com/2003/10/Serialization/Arrays", packageName);

    var meta = new CxfClientGenerator(tmpDir).generate(
        this.getClass().getResource("infoshare/InfoShare.wsdl").toString(),
        new CxfClientGenerator.CodegenOpts(null, mapping, false, false));

    assertThat(meta.getJavaModel().getServiceClasses()).containsOnlyKeys("InfoShare");

    String pathPrefix = (packageName + "/").replace('.', '/');
    String pathPrefix2 = (packageName2 + "/").replace('.', '/');
    Path servicePath = tmpDir.resolve("com/kendox/infoshare");
    assertThat(servicePath.resolve("InfoShare.wsdl")).exists();
    assertThat(servicePath.resolve("schema1.xsd")).exists();
    assertThat(tmpDir.resolve(pathPrefix + "ArrayOfstring.java")).exists();
    assertThat(tmpDir.resolve(pathPrefix2 + "ObjectFactory.java")).exists();
  }

  @Test
  void generateComplexTypes_singlePackage() throws Exception {
    String packageName = "com.kendox.infoshare.client";

    var meta = new CxfClientGenerator(tmpDir).generate(
        this.getClass().getResource("infoshare/InfoShare.wsdl").toString(),
        new CxfClientGenerator.CodegenOpts(packageName, Map.of(), false, false));

    assertThat(meta.getJavaModel().getServiceClasses()).containsOnlyKeys("InfoShare");

    Path servicePath = tmpDir.resolve("com/kendox/infoshare/client");
    assertThat(servicePath.resolve("InfoShare.wsdl")).exists();
    assertThat(servicePath.resolve("schema1.xsd")).exists();
    assertThat(servicePath.resolve("ArrayOfstring.java"))
        .as("Types deriving from microsoft-xsd are inlined into the single main package of the client")
        .exists();
    assertThat(servicePath.resolve("ObjectFactory.java")).exists();
  }

  // ISSUE XIVY-3546 CXF WebService Creation with undefined element
  @Test
  void generateWithUndefinedNames() throws Exception {
    var meta = generateResource("undefinedElement.wsdl", tmpDir);

    var services = meta.getJavaModel().getServiceClasses();
    var service = services.entrySet().iterator().next().getValue();
    Path generated = tmpDir.resolve(service.getFullClassName().replace(".", "/") + ".java");
    var serviceImpl = Files.readString(generated);
    assertThat(serviceImpl)
        .contains("public PersonService getPersonServicePort()");

    var personService = Files.readString(generated.getParent().resolve("PersonService.java"));
    assertThat(personService).containsIgnoringWhitespaces("""
      public AddPersonResponse addPerson(
          @WebParam(partName = "parameters", name = "addPerson", targetNamespace = "http://service.soap.connectivity.axonivy.com/")
          AddPerson parameters
      ) throws WebServiceProcessTechnicalException;""");
  }

  /**
   * ISSUE XIVY-3117 CXF WebService Creation with included xsd
   * @throws Exception
   *
   *           Fetching the initial WSDL and handling optional redirects to HTTPS is
   *           well supported by our CXF client generator.
   *
   *           However, for fetching internal XSD refs to HTTP, we need additional flags.
   *
   *           e.g. <xs:include schemaLocation="http://test-webservices.ivyteam.io/wsdl/geresResidentWsdl/GeresResidentInfo.xsd" />
   *           which redirects to 'https://test-webservices.ivyteam.io/wsdl/geresResidentWsdl/GeresResidentInfo.xsd
   *           seeCxfClientGenerator.withRedirectConfigurer
   */
  @Test
  void generateGeres_httpsRedirect(@TempDir Path tmpXsdSupplier) throws Exception {
    Path httpsWsdl = httpsXsdRedirect(tmpXsdSupplier);
    var meta = new CxfClientGenerator(tmpDir).generate(httpsWsdl.toString(),
        CxfClientGenerator.CodegenOpts.DEFAULT);

    var services = meta.getJavaModel().getServiceClasses();
    var service = services.entrySet().iterator().next().getValue();
    Path generated = tmpDir.resolve(service.getFullClassName().replace(".", "/") + ".java");
    var serviceImpl = Files.readString(generated);
    assertThat(serviceImpl)
        .contains("public ResidentInfoPortType getResidentInfoPort()");

    var portType = Files.readString(generated.getParent().resolve("ResidentInfoPortType.java"));
    assertThat(portType).containsIgnoringWhitespaces("""
      public ResidentInfoFastResponse residentInfoFast(
          @WebParam(partName = "parameters", name = "ResidentInfoFast", targetNamespace = "http://geres.bedag.ch/schemas/20180101/GeresResidentInfoService")
          ResidentInfoFast parameters
      ) throws InvalidArgumentsFault, PermissionDeniedFault, InfrastructureFault""");
  }

  private static Path httpsXsdRedirect(Path tmpDir) throws IOException {
    var httpsWsdl = tmpDir.resolve("residentInfo.wsdl");
    try (var is = TestCxfClientCodegen.class.getResourceAsStream("geres/GeresResidentInfo_v1801.wsdl")) {
      var wsdl = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      wsdl = wsdl.replace(
          "<xs:include schemaLocation=\"GeresResidentInfo.xsd\" />",
          "<xs:include schemaLocation=\"" + mockBaseUrl + "/wsdl/geres/GeresResidentInfo.xsd\" />");
      Files.writeString(httpsWsdl, wsdl);
    }
    return httpsWsdl;
  }

  // ISSUE XIVY-3193 CXF generated WSDL pointing to local included XSD file.
  @Test
  void generateGeres_localIncludedXSD() throws Exception {
    generateResource("geres/GeresResidentInfo_v1801.wsdl", tmpDir);
    Path servicePath = tmpDir.resolve("ch/bedag/geres/schemas/_20180101/geresresidentinfoservice");
    Path wsdlPath = servicePath.resolve("GeresResidentInfo_v1801.wsdl");
    assertThat(wsdlPath)
        .as("contains offline WSDL")
        .exists();
    String wsdlContent = Files.readString(wsdlPath);
    assertThat(wsdlContent).as("contains offline XSD")
        .containsPattern("xs:include schemaLocation=\"schema[\\d]+.xsd\"");
  }

  // ISSUE XIVY-3586 CXF fails to include ObjectFactory file for empty targetNamespace in jar file.
  @Test
  void generateService_NoTargetNamespace() throws Exception {
    generateResource("noTargetNamespace.wsdl", tmpDir);
    assertThat(tmpDir.resolve("generated").resolve("ObjectFactory.java"))
        .exists();

    var serviceImpl = Files.readString(tmpDir.resolve("org").resolve("tempuri").resolve("IService1.java"));
    assertThat(serviceImpl).isNotEmpty();

    assertThat(serviceImpl).containsIgnoringWhitespaces("""
      public GetDataUsingDataContractResponse getDataUsingDataContract(
        @WebParam(partName = "parameters", name = "GetDataUsingDataContract", targetNamespace = "http://tempuri.org/")
        GetDataUsingDataContract parameters
      );""");
  }

  /**
   * ISSUE XIVY-13809 Handle underline type-defs in CXF soap client.
   * @throws Exception
   */
  @Test
  void generateUnderscoreNames() throws Exception {
    boolean underscoreAsChar = true;
    var opts = new CxfClientGenerator.CodegenOpts(null, Map.of(), underscoreAsChar, false);

    new CxfClientGenerator(tmpDir).generate(getClass().getResource("underscored.wsdl").toString(), opts);
    var bookImpl = Files.readString(tmpDir.resolve("com/cleverbuilder/bookservice/Book.java"));
    assertThat(bookImpl)
        .as("getter for type natively without underscores")
        .containsIgnoringWhitespaces("""
          public String getPRICEDATE() {
            return pricedate;
          }""");

    assertThat(bookImpl)
        .as("getter for type natively with underscore")
        .containsIgnoringWhitespaces("""
          public String getPRICE_DATE() {
            return price_DATE;
          }""");
  }

  // ISSUE XIVY-2477 WSDL Fix for ClientJars that contain an empty schemaLocation on the import(like:schemaLocation="")
  // ISSUE https://issues.apache.org/jira/browse/CXF-7706
  @Test
  void generate_absentImportSchemaLocation() throws Exception {
    generateResource("fileDownload.wsdl", tmpDir);
    Path servicePath = tmpDir.resolve("org/example/filedownload");
    Path wsdlPath = servicePath.resolve("fileDownload.wsdl");
    assertThat(wsdlPath)
        .as("contains offline WSDL")
        .exists();
    String wsdlContent = Files.readString(wsdlPath);
    assertThat(wsdlContent)
        .as("no empty schemaLocations are being injected by CXF codegen")
        .doesNotContain("schemaLocation=\"\"")
        .contains("<xs:import namespace=\"http://www.w3.org/2005/05/xmlmime\"/>");
  }

  public static ToolContext generateResource(String resourceName, Path tmpDir) throws Exception {
    return new CxfClientGenerator(tmpDir).generate(
        TestCxfClientCodegen.class.getResource(resourceName).toString(),
        CxfClientGenerator.CodegenOpts.DEFAULT);
  }

}
