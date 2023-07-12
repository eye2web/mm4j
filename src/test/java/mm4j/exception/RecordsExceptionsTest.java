package mm4j.exception;

import mm4j.MM4J;
import mm4j.interfaces.RecordMapper;
import mm4j.records.FromRecordTwo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecordsExceptionsTest {

    private RecordMapper recordMapper;

    @BeforeAll
    private void setup() {
        recordMapper = MM4J.createMapper(RecordMapper.class);
    }

    @Test
    public void shouldThrowMappingExceptionForMultiConstructor() {
        final var input = new FromRecordTwo("Patrick", "Henderson");

        final var exceptionResult = Assertions.assertThrows(MM4JMappingException.class, () -> {
            recordMapper.multiConstructorMapWithMapping(input);
        });

        Assertions.assertNotNull(exceptionResult);
        Assertions.assertEquals("""
                Constructor parameter names should be present when using @Mapping feature!
                Only the first class constructor supports this. This is a Java restriction
                """.stripIndent(), exceptionResult.getMessage());
    }

}
