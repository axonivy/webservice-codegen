// package com.axonivy.ivy.tool.cxf.codegen;

// import java.io.File;
// import java.nio.file.Path;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.List;

// import org.osgi.framework.Bundle;

// import ch.ivyteam.eclipse.util.EclipsePlatformUtils;
// import ch.ivyteam.ivy.java.JdtCompiler;
// import ch.ivyteam.ivy.java.JdtCompiler.JdtCompilerArguments;
// import ch.ivyteam.ivy.java.JdtCompiler.JdtCompilerArguments.Builder;
// import ch.ivyteam.log.Logger;

// /**
//  * JDT implementation of a CXF compiler. Works in eclipse environment without a JDK.
//  * @author rew
//  * @since 7.1.0
//  */
// class JdtCxfCompiler extends org.apache.cxf.common.util.Compiler {

//   private static final Logger LOGGER = Logger.getLogger(JdtCxfCompiler.class);

//   private final Builder args = JdtCompilerArguments.create();
//   private final Path tmpGenDir;

//   public JdtCxfCompiler(Path tmpGenDir) {
//     this.tmpGenDir = tmpGenDir;
//   }

//   @Override
//   public void setOutputDir(String out) {
//     args.setOutputDirectory(out);
//   }

//   @Override
//   public void setEncoding(String encoding) {
//     args.setEncoding(encoding);
//   }

//   @Override
//   public void setVerbose(boolean verbose) {
//     args.setVerbose(verbose);
//   }

//   @Override
//   public boolean compileFiles(String[] files) {
//     List<String> fileList = new ArrayList<>(Arrays.asList(files));
//     var arguments = args
//         .setClassPath(getClasspathOfBundle() + File.pathSeparatorChar + tmpGenDir.toAbsolutePath().toString())
//         .setJavaSourceFiles(fileList)
//         .build();

//     var result = JdtCompiler.compile("Compile CXF java source files", arguments);
//     if (!result.success()) {
//       LOGGER.error(result.error());
//     }
//     return result.success();
//   }

//   private static final String getClasspathOfBundle() {
//     try {
//       Bundle bundle = EclipsePlatformUtils.getBundle(CxfClientGenerator.class);
//       return EclipsePlatformUtils.getAbsoluteClassPathFromBundle(bundle);
//     } catch (Throwable ex) {
//       throw new RuntimeException("Failed to resolve classpath", ex);
//     }
//   }
// }
