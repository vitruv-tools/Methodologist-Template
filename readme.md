# EMF Template

*Template for building EMF projects with pure (Tycho-less) Maven*

## Setup

The EMF Template project uses the [Maven build parent](https://github.com/vitruv-tools/Maven-Build-Parent) of the [vitruv-tools](https://vitruv.tools/) organization.
For detailed documentation on the definition of the build process, see the readme file of the Maven build parent.

The EMF template project is divided into multiple submodules, each showcasing a specific aspect of the build system.
Usually, the shown techniques or process steps can be combined, which is usually required when setting up a project.

On the top level there is a number of files and directories that serve a more technical purpose, listed below:

| File/Directory         | Purpose                                                                          |
|------------------------|----------------------------------------------------------------------------------|
| .mvn                   | Contains additional settings for Maven.                                          |
| .antlr-generator.*.jar | Provided and required by the Xtext build, can be ignored.                        |
| .gitignore             | Lists ignore files and directories.                                              |
| mvnw                   | Script to run Maven using an automatically provided binary with a fixed version. |
| mvnw.cmd               | *Same for Windows.*                                                              |
| pom.xml                | Main project definition, see below.                                              |
| readme.md              | *This file.*                                                                     |

## Main project definition (pom.xml)

The `pom.xml` file defines the main project and the commonalities of the submodules.
This includes project information (name, version), the submodules, additional repositories, and shared dependencies.
If you want to add a new submodule, simply add the name submodule directory in `module` tags to the `modules` list.
To allow snapshot dependencies from the OSSRH repository, add the repository definition show below to the `repositories` list.
In order to avoid conflicting dependencies in a project, dependency versions are fixed in the main `pom.xml` file using the `dependencyManagement` list.
Submodules can use dependencies (without repeating the version) in the usual `dependencies` list.
Notice that the main `pom.xml` does not define build steps, as these are define in the [Maven build parent](https://github.com/vitruv-tools/Maven-Build-Parent) and used in the individual submodules.

```
<repository>
    <id>ossrh-snapshots</id>
    <name>OSSRH Snapshots</name>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    <snapshots>
    <enabled>true</enabled>
    </snapshots>
    <releases>
    <enabled>false</enabled>
    </releases>
</repository>
```

## Submodules

The submodules in this project each show a specific aspect of the build system, described in the table below and detailed in the following subsections.

**Basic:**

| Submodule | Shown build aspect                                 |
|-----------|----------------------------------------------------|
| model     | Generating Java code from Ecore meta-models.       |
| logic     | Generating Java code from Xtend code.              |
| dsl       | Generating a parser/generator from Xtext grammars. |
| demo      | Generating code from a custom DSL.                 |

**Advanced:**

| Submodule                   | Shown build aspect                                                 |
|-----------------------------|--------------------------------------------------------------------|
| imports                     | Importing meta-models in custom DSLs, split into three submodules. |
| - imports.demo              | Using meta-models in a custom DSL.                                 |
| - imports.dependencywrapper | Wrapping p2 updatesite dependencies in a Maven module.             |
| - imports.dsl               | Defining a custom DSL with meta-model imports.                     |
| modelextension              | Referencing elements from other meta-models in a meta-model.       |
| p2dependencies              | Including dependencies from p2 (Eclipse) updatesites.              |

### `tools.vitruv.emftemplate.model`

To build Java code from Ecore meta-models, we need to:
- Use the plugins:
    - `org.codehaus.mojo:build-helper-maven-plugin` to include the Ecore files on the classpath
    - `org.codehaus.mojo:exec-maven-plugin` to generate the Java code
    - `maven-jar-plugin` to include the generated Manifest file in the Jar package
- Include the dependencies:
    - `org.eclipse.emf:org.eclipse.emf.common`
    - `org.eclipse.emf:org.eclipse.emf.ecore`
- Add the project file `.project`
    - The project name should be the artifact ID of the project, but cannot contain slashes (`-`)
- Add the workflow file `workflow/generate.mwe2` with exactly this name
    - The `module` name must be the project name from the `.project` file
    - The `platform:/resource/` paths start with the module name and must reference the genmodel and output directory
- Add the `.ecore` and `.genmodel` files to the directory `src/main/ecore/`

The generated Java code will be placed in the directory `target/generated-sources/ecore/`.

Please also consider the section [Working with Ecore meta-models](https://github.com/vitruv-tools/Maven-Build-Parent?tab=readme-ov-file#working-with-ecore-meta-models) in the readme of the Maven build parent.

### `tools.vitruv.emftemplate.logic`

To build Java code from Xtend code, we need to:
- Use the plugins:
    - `org.codehaus.mojo:build-helper-maven-plugin` to include the Xtend files on the classpath
    - `org.eclipse.xtend:xtend-maven-plugin` to generate the Java code
- Include the dependencies:
    - `org.eclipse.emf:org.eclipse.emf.common`
    - `org.eclipse.xtext:org.eclipse.xtext.xbase.lib`
- Add the `*.xtend` files to the directories `src/main/xtend` or `src/test/xtend`

The generated Java code will be placed in the directories `target/generated-sources/xtend/` (main code) and `target/generated-test-sources/xtend` (test code).

### `tools.vitruv.emftemplate.dsl`

This project contains a grammar for a custom DSL, as well as a parser and a simple code generator.

To build parser/generator code from Xtext grammars, we need to:
- Use the plugins:
    - `org.codehaus.mojo:build-helper-maven-plugin` to include the Xtext files on the classpath
    - `org.codehaus.mojo:exec-maven-plugin` to generate the Java code
- Include the dependencies in the `pom.xml` file of the submodule
- Add the workflow file `workflow/generate.mwe2` with exactly this name (settings described in the table below)
- Add the `*.xtext` files to the directory `src/main/xtext`

| Setting                                | Meaning                                                                                                                                                   |
|----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `configuration.project.runtime.root`   | Main source folder, in which Xtext generates the Java files that should be modified.                                                                      |
| `configuration.project.runtime.srcGen` | Output folder, in which Xtext generates the Java files that should not be modified.                                                                       |
| `language.name`                        | Fully qualified name of the language.                                                                                                                     |
| `language.fileExtensions`              | File extensions of files written in the custom DSL.                                                                                                       |
| `language.grammarUri`                  | `platform:/resource` URI to the `*.xtext` grammar file.                                                                                                   |
| `language.referencedResource`          | `platform:/resource` URI to a used genmodel. This setting can be included multiple times.                                                                 |
| `language.serializer.generateStub`     | Whether Xtext should generate a stub of a serializer for the custom DSL in the main source folder. Same for `generator`, `scopeProvider` and `validator`. |

The generated Java code will be placed in the directory `target/generated-sources/xtext-java/`.

### `tools.vitruv.emftemplate.demo`

To generate code from a custom DSL, we need to:
- Use the plugins:
    - `org.codehaus.mojo:build-helper-maven-plugin` with a new execution configuration, specifying the directories for the main source code and the generated source code
    - `org.eclipse.xtext:xtext-maven-plugin` with a language configuration for the custom DSL, including the output directory for the generated source code and the dependency including the custom DSL
- Add the files written in the custom DSL to the main source code directory defined in the configuration of the `build-helper-maven-plugin`

### `tools.vitruv.emftemplate.imports`

This submodule shows how to import meta-models in a custom DSL and contains three submodules.

In the `dsl` submodule, a custom DSL is defined, in which meta-models can be imported.
In the grammar rule, shown below, the rule `EPackage|STRING` is used, which accepts a meta-model URI as a string.
To correctly resolve the meta-model (namespace) URIs, the class `ImportsLanguageLinkingService` is registered in the generated class `ImportsLanguageRuntimeModule`.

```
'import' package=[ecore::EPackage|STRING] ';'
```

The `demo` submodule demonstrates the use of this feature by importing two meta-models, one from a Maven dependency and one from a p2 dependency.

As the meta-models need to be added as dependencies of the plugin `org.eclipse.xtext:xtext-maven-plugin` and dependencies of plugins cannot be p2 dependencies, we need to add a wrapper Maven module.
This is the (only) purpose of the `depedencywrapper` submodule, which includes the p2 dependencies containing the referenced meta-model.

### `tools.vitruv.emftemplate.modelextension`

When referencing elements from other meta-models in a meta-model, different URIs can be used.
Following the instructions in the readme of the [Maven build parent](https://github.com/vitruv-tools/Maven-Build-Parent), we use namespace URIs in the `extended-model.ecore` file and `platform:/resource` URIs in the `usedGenPackages` tag of the `extended-model.genmodel` file.

To correctly resolve these URIs, it is required to add an URI map to the workflow file `workflow/generate.mwe2`.
The URI map, shown below, links a namespace URI to a `platform:/resource` URI referencing the `.ecore` file defining the referenced meta-model.
The `platform:/resource` URI differs depending on the packaging of the meta-model and can be determined by opening the JAR file of the dependency with an archive manager.

```
uriMap = {
    from = "http://vitruv.tools/emftemplate/model"
    to = "platform:/resource/tools.vitruv.emftemplate.model/src/main/ecore/model.ecore"
}
```

### `tools.vitruv.emftemplate.p2dependencies`

In order to use p2 dependencies from Eclipse updatesites in our Maven build, we must add the updatesite as a repository in the `pom.xml` file, as shown below.
The `id` specifies the `groupId` we can use to include the dependencies from the updatesite in our `pom.xml`.
The `url` specified the URL of the updatesite and often (not for nightly builds) includes the version.
For the `layout` tag, we need to use the value `p2`.

```
<repository>
    <id>emf-compare</id>
    <name>EMF Compare</name>
    <url>https://download.eclipse.org/modeling/emf/compare/updates/releases/3.3/R202212280858</url>
    <layout>p2</layout>
</repository>
```

After adding the p2 updatesite as a repository, dependencies from it can be added like usualy Maven dependencies, as shown in the example below.

```
<dependency>
    <groupId>emf-compare</groupId>
    <artifactId>org.eclipse.emf.compare</artifactId>
    <version>3.5.3.202212280858</version>
</dependency>
```
