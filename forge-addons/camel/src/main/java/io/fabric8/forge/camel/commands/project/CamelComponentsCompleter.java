/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.forge.camel.commands.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.catalog.CamelComponentCatalog;
import org.apache.camel.catalog.DefaultCamelComponentCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;
import org.jboss.forge.addon.ui.input.UIInput;

public class CamelComponentsCompleter implements UICompleter<String> {

    private Project project;
    private UIInput<String> filter;

    public CamelComponentsCompleter(Project project, UIInput<String> filter) {
        this.project = project;
        this.filter = filter;
    }

    @Override
    public Iterable<String> getCompletionProposals(UIContext context, InputComponent input, String value) {
        // find the version of Apache Camel we use

        // need to find camel-core so we known the camel version
        Dependency core = CamelProjectHelper.findCamelCoreDependency(project);
        if (core == null) {
            return null;
        }

        // find all available component names
        CamelComponentCatalog catalog = new DefaultCamelComponentCatalog();
        List<String> names = catalog.findComponentNames();

        // filter non matching names first
        List<String> filtered = new ArrayList<String>();
        for (String name : names) {
            if (value == null || name.startsWith(value)) {
                filtered.add(name);
            }
        }

        filtered = filterByName(filtered);
        filtered = filterByLabel(filtered, filter.getValue());

        return filtered;
    }

    private List<String> filterByName(List<String> choices) {
        List<String> answer = new ArrayList<String>();

        CamelComponentCatalog catalog = new DefaultCamelComponentCatalog();

        // filter names which are already on the classpath, or do not match the optional filter by label input
        for (String name : choices) {
            String json = catalog.componentJSonSchema(name);
            String artifactId = findArtifactId(json);

            // skip if we already have the dependency
            boolean already = false;
            if (artifactId != null) {
                already = CamelProjectHelper.hasDependency(project, "org.apache.camel", artifactId);
            }

            if (!already) {
                answer.add(name);
            }
        }

        return answer;
    }

    private List<String> filterByLabel(List<String> choices, String label) {
        if (label == null || label.isEmpty()) {
            return choices;
        }

        List<String> answer = new ArrayList<String>();

        CamelComponentCatalog catalog = new DefaultCamelComponentCatalog();

        // filter names which are already on the classpath, or do not match the optional filter by label input
        for (String name : choices) {
            String json = catalog.componentJSonSchema(name);
            String labels = findLabel(json);
            if (labels != null) {
                for (String target : labels.split(",")) {
                    if (target.startsWith(label)) {
                        answer.add(name);
                        break;
                    }
                }
            } else {
                // no label so they all match
                answer.addAll(choices);
            }
        }

        return answer;
    }

    private static String findArtifactId(String json) {
        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("component", json, false);
        for (Map<String, String> row : data) {
            if (row.get("artifactId") != null) {
                return row.get("artifactId");
            }
        }
        return null;
    }

    private static String findLabel(String json) {
        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("component", json, false);
        for (Map<String, String> row : data) {
            if (row.get("label") != null) {
                return row.get("label");
            }
        }
        return null;
    }

}
