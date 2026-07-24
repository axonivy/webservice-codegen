# WebService Codegen

[![release-version][release-shield]][release]
[![snapshot-version][snap-shield]][snap]

[snap-shield]: https://img.shields.io/maven-metadata/v?label=dev&logo=sonatype&metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fcom%2Faxonivy%2Fivy%2Ftool%2Fsoap%2Fcxf-client-codegen%2Fmaven-metadata.xml
[snap]: https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/axonivy/ivy/tool/soap/cxf-client-codegen/

[release-shield]: https://img.shields.io/maven-metadata/v.svg?label=Stable&logo=apachemaven&metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fcom%2Faxonivy%2Fivy%2Ftool%2Fsoap%2Fcxf-client-codegen%2Fmaven-metadata.xml
[release]: https://repo1.maven.org/maven2/com/axonivy/ivy/tool/soap/cxf-client-codegen/

This tool generates an Axon Ivy compatible SOAP WebService Client from a WSDL specification.

## Usage

### CLI

The generator can be run using Maven's CLI:

```bash
mvn com.axonivy.ivy.tool.soap:cxf-client-codegen:generate-cxf-client\
 -Divy.generate.webservice.client.wsdl=https://example.com/service?wsdl\
 -Divy.generate.webservice.client.output=src_generated/ws/myService\
 -Divy.generate.webservice.client.namespace=com.example.service.client
```

### CI

The generator can run on every build cycle of your project.
With this you don't need to include the generated sources under version control.

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.axonivy.ivy.tool.soap</groupId>
      <artifactId>cxf-client-codegen</artifactId>
      <executions>
        <execution>
          <id>myService.codegen</id>
          <phase>generate-sources</phase>
          <goals>
            <goal>generate-cxf-client</goal>
          </goals>
          <configuration>
            <wsdl>https://example.com/service?wsdl</wsdl>
            <outputDir>${basedir}/src_generated/ws/myService</outputDir>
            <namespace>com.example.service.client</namespace>
          </configuration>
        </execution>
      </executions>
    </plugin>
    ...
```



## Configuration

| Parameter | CLI property | Default | Description |
|-----------|-------------|---------|-------------|
| `wsdl` | `ivy.generate.webservice.client.wsdl` | *(required)* | URI or path to the WSDL file |
| `outputDir` | `ivy.generate.webservice.client.output` | *(required)* | Directory to write the generated sources into |
| `namespace` | `ivy.generate.webservice.client.namespace` | — | Java package name for the generated client classes |
| `underscoreNames` | `ivy.generate.webservice.client.underscoreNames` | `false` | Preserve underscore distinctions in field names (e.g. `PRICE_DATE` vs `PRICEDATE`) |
| `insecureSSL` | `ivy.generate.webservice.client.insecureSSL` | `false` | Allow insecure SSL connections when fetching WSDL from HTTPS endpoints with self-signed certificates |
| `writeServiceInfo` | `ivy.generate.webservice.client.writeServiceInfo` | `false` | Opt in to write service metadata to `service.json` in the generation output directory |
| `skipGenerate` | `ivy.generate.webservice.client.skip` | `false` | Skip code generation entirely |

## FAQ

- My WSDL is hosted on an endpoint with a self-signed certificate and the generator fails with an SSL error. What can I do?

  > Enable the `insecureSSL` option to bypass certificate validation when fetching the WSDL and its imported XSD resources.

  ```xml
  <configuration>
    <wsdl>https://internal.corp/service?wsdl</wsdl>
    <outputDir>${basedir}/src_generated/ws/corpService</outputDir>
    <insecureSSL>true</insecureSSL>
  </configuration>
  ```

- My WSDL has fields that only differ by an underscore (e.g. `PRICEDATE` and `PRICE_DATE`) and one of them disappears in the generated client. How do I fix this?

  > Enable the `underscoreNames` option. This forces CXF to preserve the underscore distinction in the generated Java field names.

  ```xml
  <configuration>
    ...
    <underscoreNames>true</underscoreNames>
  </configuration>
  ```

- Migrating from Axon Ivy Designer 12 code generation, the single package definition breaks many usages of the generated types in my project. Is it possible to keep the LTS 12 and earlier approach to map specific namespaces to package names?

  > Configure the legacy 'nsMappings' in a codegen run that you include in the project pom.xml.

  ```xml
  <build>
    <plugins>
      <plugin>
        <groupId>com.axonivy.ivy.tool.soap</groupId>
        <artifactId>cxf-client-codegen</artifactId>
        <executions>
          <execution>
            <id>myService.codegen</id>
            <phase>generate-sources</phase>
            <goals>
              <goal>generate-cxf-client</goal>
            </goals>
            <configuration>
              <wsdl>https://example.com/service?wsdl</wsdl>
              <outputDir>${basedir}/src_generated/ws/myService</outputDir>
              <nsMappings>
                <nsMapping>http://service.example.com/@com.example.service.client</nsMapping>
              </nsMappings>
            </configuration>
          </execution>
        </executions>
      </plugin>
      ...
  ```

- A newer CXF version is available that fixes an issue I have with my WSDL. Can I use it?

  > Yes, add your preferred CXF version to the `<dependencies>` of the plugin.
  > As long as the APIs are compatible, it will run.

  ```xml
  <plugin>
    <groupId>com.axonivy.ivy.tool.soap</groupId>
    <artifactId>cxf-client-codegen</artifactId>
    ...
    <dependencies>
      <dependency>
        <groupId>org.apache.cxf</groupId>
        <artifactId>cxf-tools-wsdlto-core</artifactId>
        <version>3.6.12</version>
      </dependency>
    </dependencies>
  </plugin>
  ```

## Releasing

1. Run the [release](actions/workflows/release.yml) pipeline and merge the PR that initiates the next development cycle.
2. Go to Github [releases](releases) and edit the latest draft release.
   - set the 'tag' to the one which was just produced by the previous release run
   - publish it as latest release
3. Update the `webservice.codegen.tool.version` property, stated in the associated [ivy-parent-pom](https://github.com/axonivy/core/tree/master/maven/java-api/ivy-project-parent/pom.xml).

