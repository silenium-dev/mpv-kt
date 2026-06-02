package dev.silenium.libs.mpv.build;

import org.gradle.kotlin.dsl.PluginDependenciesSpecScope;
import org.gradle.kotlin.dsl.support.PluginDependenciesSpecScopeInternal;

public class PluginCatalogResolver {
    private PluginCatalogResolver() {
    }

    public static Object resolve(PluginDependenciesSpecScope scope, String name) {
        return ((PluginDependenciesSpecScopeInternal) scope).versionCatalogForPluginsBlock(name);
    }
}
