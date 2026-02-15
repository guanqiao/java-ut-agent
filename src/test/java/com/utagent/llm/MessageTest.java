package com.utagent.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Message Tests")
class MessageTest {

    @Nested
    @DisplayName("Static Factory Methods")
    class StaticFactoryMethods {

        @Test
        @DisplayName("Should create system message")
        void shouldCreateSystemMessage() {
            String content = "You are a helpful assistant";
            
            Message message = Message.system(content);
            
            assertEquals("system", message.role());
            assertEquals(content, message.content());
        }

        @Test
        @DisplayName("Should create user message")
        void shouldCreateUserMessage() {
            String content = "Hello, how are you?";
            
            Message message = Message.user(content);
            
            assertEquals("user", message.role());
            assertEquals(content, message.content());
        }

        @Test
        @DisplayName("Should create assistant message")
        void shouldCreateAssistantMessage() {
            String content = "I am doing well, thank you!";
            
            Message message = Message.assistant(content);
            
            assertEquals("assistant", message.role());
            assertEquals(content, message.content());
        }

        @Test
        @DisplayName("Should handle empty content")
        void shouldHandleEmptyContent() {
            Message systemMessage = Message.system("");
            Message userMessage = Message.user("");
            Message assistantMessage = Message.assistant("");
            
            assertEquals("", systemMessage.content());
            assertEquals("", userMessage.content());
            assertEquals("", assistantMessage.content());
        }

        @Test
        @DisplayName("Should handle null content")
        void shouldHandleNullContent() {
            Message systemMessage = Message.system(null);
            Message userMessage = Message.user(null);
            Message assistantMessage = Message.assistant(null);
            
            assertNull(systemMessage.content());
            assertNull(userMessage.content());
            assertNull(assistantMessage.content());
        }

        @Test
        @DisplayName("Should handle multiline content")
        void shouldHandleMultilineContent() {
            String multilineContent = """
                Line 1
                Line 2
                Line 3
                """;
            
            Message message = Message.user(multilineContent);
            
            assertEquals(multilineContent, message.content());
            assertTrue(message.content().contains("\n"));
        }
    }

    @Nested
    @DisplayName("Constructor and Accessors")
    class ConstructorAndAccessors {

        @Test
        @DisplayName("Should create message with constructor")
        void shouldCreateMessageWithConstructor() {
            String role = "custom";
            String content = "Custom message";
            
            Message message = new Message(role, content);
            
            assertEquals(role, message.role());
            assertEquals(content, message.content());
        }

        @Test
        @DisplayName("Should access role correctly")
        void shouldAccessRoleCorrectly() {
            Message message = new Message("test-role", "test-content");
            
            assertEquals("test-role", message.role());
        }

        @Test
        @DisplayName("Should access content correctly")
        void shouldAccessContentCorrectly() {
            Message message = new Message("test-role", "test-content");
            
            assertEquals("test-content", message.content());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("Should be equal when same role and content")
        void shouldBeEqualWhenSameRoleAndContent() {
            Message message1 = new Message("user", "Hello");
            Message message2 = new Message("user", "Hello");
            
            assertEquals(message1, message2);
            assertEquals(message1.hashCode(), message2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when different role")
        void shouldNotBeEqualWhenDifferentRole() {
            Message message1 = new Message("user", "Hello");
            Message message2 = new Message("assistant", "Hello");
            
            assertNotEquals(message1, message2);
        }

        @Test
        @DisplayName("Should not be equal when different content")
        void shouldNotBeEqualWhenDifferentContent() {
            Message message1 = new Message("user", "Hello");
            Message message2 = new Message("user", "Hi");
            
            assertNotEquals(message1, message2);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            Message message = new Message("user", "Hello");
            
            assertEquals(message, message);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            Message message = new Message("user", "Hello");
            
            assertNotEquals(null, message);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            Message message = new Message("user", "Hello");
            
            assertNotEquals("user", message);
            assertNotEquals(123, message);
        }

        @Test
        @DisplayName("Should handle null fields in equals")
        void shouldHandleNullFieldsInEquals() {
            Message message1 = new Message(null, null);
            Message message2 = new Message(null, null);
            
            assertEquals(message1, message2);
            assertEquals(message1.hashCode(), message2.hashCode());
        }

        @Test
        @DisplayName("Should handle partial null fields in equals")
        void shouldHandlePartialNullFieldsInEquals() {
            Message message1 = new Message(null, "content");
            Message message2 = new Message(null, "content");
            Message message3 = new Message("role", null);
            Message message4 = new Message("role", null);
            
            assertEquals(message1, message2);
            assertEquals(message3, message4);
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToString {

        @Test
        @DisplayName("Should contain role in toString")
        void shouldContainRoleInToString() {
            Message message = new Message("user", "Hello");
            
            String result = message.toString();
            
            assertTrue(result.contains("user"));
        }

        @Test
        @DisplayName("Should contain content in toString")
        void shouldContainContentInToString() {
            Message message = new Message("user", "Hello World");
            
            String result = message.toString();
            
            assertTrue(result.contains("Hello World"));
        }

        @Test
        @DisplayName("Should handle null in toString")
        void shouldHandleNullInToString() {
            Message message = new Message(null, null);
            
            String result = message.toString();
            
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Record Properties")
    class RecordProperties {

        @Test
        @DisplayName("Should have correct record components count")
        void shouldHaveCorrectRecordComponentsCount() {
            assertEquals(2, Message.class.getRecordComponents().length);
        }

        @Test
        @DisplayName("Should have role component")
        void shouldHaveRoleComponent() {
            var components = Message.class.getRecordComponents();
            boolean hasRole = false;
            for (var component : components) {
                if (component.getName().equals("role")) {
                    hasRole = true;
                    assertEquals(String.class, component.getType());
                }
            }
            assertTrue(hasRole);
        }

        @Test
        @DisplayName("Should have content component")
        void shouldHaveContentComponent() {
            var components = Message.class.getRecordComponents();
            boolean hasContent = false;
            for (var component : components) {
                if (component.getName().equals("content")) {
                    hasContent = true;
                    assertEquals(String.class, component.getType());
                }
            }
            assertTrue(hasContent);
        }
    }
}
