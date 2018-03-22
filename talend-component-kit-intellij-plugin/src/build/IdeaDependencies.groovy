/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import org.apache.maven.artifact.DefaultArtifact
import org.apache.maven.artifact.handler.DefaultArtifactHandler
import org.apache.maven.model.Dependency
import org.apache.maven.model.Repository


// note: we can extract that logic into a small lib instead of inlining it here

log.info 'Preparing project dependencies from idea distribution'
log.info 'Don\'t forget to activate the "ide" profile to be able to develop it'

def dependencies = [
    'lib/openapi',
    'lib/idea',
    'lib/util',
    'lib/extensions',
    'lib/gson-2.8.2',
    'lib/jdom',
    'lib/jsr305',
    'lib/swingx-core-1.6.2',
    'lib/slf4j-api-1.7.10',
    'plugins/properties/lib/properties',
    'plugins/maven/lib/maven',
    'plugins/gradle/lib/gradle'
]

def ideaBase = project.properties['idea.unpacked.folder']
def localRepository = new File(project.basedir, '.cache/m2/localrepository')

def addDependency = { base, localRepo, name ->
    def artifactId = name.replace('/', '_')
    def localPathJar = new File(localRepo, "com/intellij/idea/${artifactId}/${project.properties['idea.version']}/${artifactId}-${project.properties['idea.version']}.jar")
    if (!localPathJar.exists() ||
            Boolean.parseBoolean(System.getProperty('talend.component.kit.build.idea.m2.forceupdate', project.properties['talend.component.kit.build.studio.m2.forceupdate']))) {

        def fileSrc = new File(base, "${name}.jar")
        if (!fileSrc.exists()) {
            throw new IllegalArgumentException("No jar ${name}")
        }

        localPathJar.parentFile.mkdirs()
        def os = localPathJar.newOutputStream()
        try {
            os << fileSrc.newInputStream()
        } finally {
            os.close()
        }

        // create a fake pom
        def localPom = new File(localPathJar.parentFile, localPathJar.name.substring(0, localPathJar.name.length() - "jar".length()) + 'pom')
        def pomOs = localPom.newOutputStream()
        try {
            pomOs << """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.intellij.idea</groupId>
  <artifactId>${artifactId}</artifactId>
  <version>${project.properties['idea.version']}</version>
  <description>Generated pom at build time without dependencies</description>
</project>"""
        } finally {
            pomOs.close()
        }
    }

    localPathJar
}

// add the local repo
final Repository repository = new Repository()
repository.id = "build-idea-local-repository"
repository.url = new File(project.getBasedir(), '.cache/m2/localrepository').toURI().toURL()
project.getRepositories().add(repository)

println("""
  <repositories>
    <repository>
      <id>idea-local</id>
      <url>file:${project.basedir}/.cache/m2/localrepository</url>
    </repository>
  </repositories>
""")
println('    <!-- Generated dependencies -->')

def addArtifact = { pj, art ->
    def f = pj.class.getDeclaredField('resolvedArtifacts')
    if (!f.accessible) {
        f.accessible = true
    }
    f.get(pj).add(art)
}

// add the deps
dependencies.each {
    def dep = new Dependency()
    dep.groupId = 'com.intellij.idea'
    dep.artifactId = it.replace('/', '_')
    dep.version = "${project.properties['idea.version']}"

    def jar = addDependency(ideaBase, localRepository, it)

    project.dependencies.add(dep)

    def artHandler = new DefaultArtifactHandler()
    artHandler.addedToClasspath = true // maven-compiler-plugin uses that flag to determine the javac cp
    def art = new DefaultArtifact(dep.groupId, dep.artifactId, dep.version, 'provided', 'jar', null, artHandler)
    art.file = jar
    // project.resolvedArtifacts.add(art)
    addArtifact(project, art)

    // log it to ensure it is easy to "dev"
    println("    <dependency>\n      <groupId>${dep.groupId}</groupId>\n      <artifactId>${dep.artifactId}</artifactId>\n      <version>\${idea.version}</version>\n      <scope>provided</scope>\n    </dependency>")
}