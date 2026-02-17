package com.utagent.di;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ServiceContainer Tests")
class ServiceContainerTest {

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register and retrieve service")
        void shouldRegisterAndRetrieveService() {
            ServiceContainer container = new ServiceContainer();
            
            container.register(TestService.class, TestServiceImpl::new);
            
            TestService service = container.get(TestService.class);
            
            assertNotNull(service);
            assertTrue(service instanceof TestServiceImpl);
        }

        @Test
        @DisplayName("Should register singleton service")
        void shouldRegisterSingletonService() {
            ServiceContainer container = new ServiceContainer();
            
            container.registerSingleton(TestService.class, TestServiceImpl::new);
            
            TestService service1 = container.get(TestService.class);
            TestService service2 = container.get(TestService.class);
            
            assertSame(service1, service2);
        }

        @Test
        @DisplayName("Should register instance directly")
        void shouldRegisterInstanceDirectly() {
            ServiceContainer container = new ServiceContainer();
            TestServiceImpl instance = new TestServiceImpl();
            
            container.registerInstance(TestService.class, instance);
            
            TestService service = container.get(TestService.class);
            
            assertSame(instance, service);
        }
    }

    @Nested
    @DisplayName("Dependency Injection Tests")
    class DependencyInjectionTests {

        @Test
        @DisplayName("Should inject dependencies")
        void shouldInjectDependencies() {
            ServiceContainer container = new ServiceContainer();
            
            container.register(TestService.class, TestServiceImpl::new);
            container.register(DependentService.class, () -> 
                new DependentServiceImpl(container.get(TestService.class)));
            
            DependentService dependent = container.get(DependentService.class);
            
            assertNotNull(dependent);
            assertNotNull(dependent.getTestService());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw exception for unregistered service")
        void shouldThrowExceptionForUnregisteredService() {
            ServiceContainer container = new ServiceContainer();
            
            assertThrows(ServiceNotFoundException.class, () -> 
                container.get(TestService.class));
        }

        @Test
        @DisplayName("Should check if service is registered")
        void shouldCheckIfServiceIsRegistered() {
            ServiceContainer container = new ServiceContainer();
            
            assertFalse(container.contains(TestService.class));
            
            container.register(TestService.class, TestServiceImpl::new);
            
            assertTrue(container.contains(TestService.class));
        }
    }

    @Nested
    @DisplayName("Factory Tests")
    class FactoryTests {

        @Test
        @DisplayName("Should create new instance each time for non-singleton")
        void shouldCreateNewInstanceEachTimeForNonSingleton() {
            ServiceContainer container = new ServiceContainer();
            
            container.register(TestService.class, TestServiceImpl::new);
            
            TestService service1 = container.get(TestService.class);
            TestService service2 = container.get(TestService.class);
            
            assertNotSame(service1, service2);
        }
    }

    interface TestService {
        String execute();
    }

    static class TestServiceImpl implements TestService {
        @Override
        public String execute() {
            return "executed";
        }
    }

    interface DependentService {
        TestService getTestService();
    }

    static class DependentServiceImpl implements DependentService {
        private final TestService testService;

        DependentServiceImpl(TestService testService) {
            this.testService = testService;
        }

        @Override
        public TestService getTestService() {
            return testService;
        }
    }
}
