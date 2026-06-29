package me.flamboyant.manhunt.application.injection;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class GuiceDependencyTest {

    @Test
    public void shouldHaveGuiceAvailable() {
        Injector injector = Guice.createInjector();
        assertNotNull("Guice should be available", injector);
    }
}
