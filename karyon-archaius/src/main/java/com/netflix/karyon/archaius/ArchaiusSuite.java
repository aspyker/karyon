package com.netflix.karyon.archaius;

import com.netflix.governator.configuration.ArchaiusConfigurationProvider;
import com.netflix.governator.configuration.ConfigurationOwnershipPolicies;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.guice.LifecycleInjectorBuilderSuite;

import javax.inject.Inject;

/**
 * A guice module that defines all bindings required by karyon. Applications must use this to bootstrap karyon.
 *
 * @author Nitesh Kant
 */
public class ArchaiusSuite implements LifecycleInjectorBuilderSuite {

    private final Class<? extends PropertiesLoader> propertiesLoaderClass;
    private final PropertiesLoader propertiesLoader;

    public ArchaiusSuite(String appName) {
        this(new DefaultPropertiesLoader(appName));
    }

    public ArchaiusSuite(PropertiesLoader propertiesLoader) {
        this.propertiesLoader = propertiesLoader;
        this.propertiesLoaderClass = null;
    }

    public ArchaiusSuite(Class<PropertiesLoader> propertiesLoader) {
        this.propertiesLoaderClass = propertiesLoader;
        this.propertiesLoader = null;
    }

    @Inject
    ArchaiusSuite(ArchaiusBootstrap archaiusBootstrap) {
        propertiesLoaderClass = archaiusBootstrap.loader();
        propertiesLoader = null;
    }

    @Override
    public void configure(LifecycleInjectorBuilder builder) {
        builder.withAdditionalBootstrapModules(new BootstrapModule() {

            @Override
            public void configure(BootstrapBinder bootstrapBinder) {
                if (null != propertiesLoaderClass) {
                    bootstrapBinder.bind(PropertiesLoader.class).to(propertiesLoaderClass).asEagerSingleton();
                } else {
                    bootstrapBinder.bind(PropertiesLoader.class).toInstance(propertiesLoader);
                }
                bootstrapBinder.bind(PropertiesInitializer.class).asEagerSingleton();
                ArchaiusConfigurationProvider.Builder builder = ArchaiusConfigurationProvider.builder();
                builder.withOwnershipPolicy(ConfigurationOwnershipPolicies.ownsAll());
                bootstrapBinder.bindConfigurationProvider().toInstance(builder.build());
            }
        });
    }

    /**
     * This is required as we want to invoke {@link PropertiesLoader#load()} automatically.
     * One way of achieving this is by using {@link @javax.annotation.PostConstruct} but archaius initialization is done
     * in the bootstrap phase and {@link @javax.annotation.PostConstruct} is not invoked in the bootstrap phase.
     */
    private static class PropertiesInitializer {

        @Inject
        public PropertiesInitializer(PropertiesLoader loader) {
            loader.load();
        }
    }

}
